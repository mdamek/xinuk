package pl.edu.agh.xinuk

import java.awt.Color
import java.io.File
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.cluster.sharding.ShardCoordinator.ShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.ShardId
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import net.ceedubs.ficus.readers.ValueReader
import pl.edu.agh.xinuk.algorithm.{Metrics, PlanCreator, PlanResolver, WorldCreator}
import pl.edu.agh.xinuk.config.{GuiType, XinukConfig}
import pl.edu.agh.xinuk.gui.{GridGuiActor, LedPanelGuiActor, SnapshotActor, SplitSnapshotActor}
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.model.grid.{GridWorldShard, GridWorldType}
import pl.edu.agh.xinuk.simulation.WorkerActor

import scala.jdk.CollectionConverters._
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Simulation[ConfigType <: XinukConfig : ValueReader](
                                                           configPrefix: String,
                                                           metricHeaders: Vector[String],
                                                           worldCreator: WorldCreator[ConfigType],
                                                           planCreatorFactory: () => PlanCreator[ConfigType],
                                                           planResolverFactory: () => PlanResolver[ConfigType],
                                                           emptyMetrics: => Metrics,
                                                           signalPropagation: SignalPropagation,
                                                           cellToColor: PartialFunction[CellState, Color] = PartialFunction.empty
                                                         ) extends LazyLogging {

  private val rawConfig: Config =
    Try(ConfigFactory.parseFile(new File("xinuk.conf")))
      .filter(_.hasPath(configPrefix))
      .getOrElse {
        logger.info("Falling back to reference.conf")
        ConfigFactory.empty()
      }.withFallback(ConfigFactory.load("cluster.conf"))

  implicit val config: ConfigType = {
    val applicationConfig = rawConfig.getConfig(configPrefix)
    logger.info(WorkerActor.MetricsMarker, applicationConfig.root().render(ConfigRenderOptions.concise()))
    logger.info(WorkerActor.MetricsMarker, logHeader)

    import net.ceedubs.ficus.Ficus._
    Try(applicationConfig.as[ConfigType]("config")) match {
      case Success(parsedConfig) =>
        logger.info("Config parsed successfully.")
        parsedConfig
      case Failure(parsingError) =>
        logger.error("Config parsing error.", parsingError)
        System.exit(2)
        throw new IllegalArgumentException
    }
  }

  private val system = ActorSystem(rawConfig.getString("application.name"), rawConfig)

  private val customAllocationStrategy = new CustomAllocationStrategy(
    ClusterShardingSettings(system).tuningParameters.leastShardAllocationRebalanceThreshold,
    ClusterShardingSettings(system).tuningParameters.leastShardAllocationMaxSimultaneousRebalance,
    logger,
    rawConfig.getStringList("shard-allocation-order").asScala.toList)

  private val workerRegionRef: ActorRef = ClusterSharding(system).start(
    typeName = WorkerActor.Name,
    entityProps = WorkerActor.props[ConfigType](workerRegionRef, planCreatorFactory(), planResolverFactory(), emptyMetrics, signalPropagation, cellToColor),
    settings = ClusterShardingSettings(system),
    extractShardId = WorkerActor.extractShardId,
    extractEntityId = WorkerActor.extractEntityId,
    allocationStrategy = customAllocationStrategy,
    handOffStopMessage = PoisonPill
  )

  def start(): Unit = {
    if (config.isSupervisor) {
      val workerToWorld: Map[WorkerId, WorldShard] = worldCreator.prepareWorld().build()
      val simulationId: String = UUID.randomUUID().toString

      workerToWorld.foreach({ case (workerId, world) =>
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.WorkerInitialized(world))
      })

      (config.guiType, config.worldType) match {
        case (GuiType.None, _) =>
        case (GuiType.Grid, GridWorldType) =>
          workerToWorld.foreach({ case (workerId, world) =>
            system.actorOf(GridGuiActor.props(workerRegionRef, simulationId, workerId, world.asInstanceOf[GridWorldShard].bounds))
          })
        case (GuiType.SplitSnapshot, GridWorldType) =>
          workerToWorld.foreach({ case (workerId, world) =>
            system.actorOf(SplitSnapshotActor.props(workerRegionRef, simulationId, workerId, world.asInstanceOf[GridWorldShard].bounds))
          })
        case (GuiType.Snapshot, GridWorldType) =>
          system.actorOf(SnapshotActor.props(workerRegionRef, simulationId, workerToWorld.keySet))
        case (GuiType.LedPanel, GridWorldType) =>
          workerToWorld.foreach({ case (workerId, world) =>
            system.actorOf(LedPanelGuiActor.props(workerRegionRef, simulationId, workerId, world.asInstanceOf[GridWorldShard].bounds, config.ledPanelPort))
          })
        case _ => logger.warn("GUI type not recognized or incompatible with World format.")
      }
    }
  }

  private def logHeader: String = s"worker:iteration;activeTime;waitingTime;${metricHeaders.mkString(";")}"
}

class CustomAllocationStrategy(rebalanceThreshold: Int, maxSimultaneousRebalance: Int, logger: Logger,
                               shardAllocationOrder: List[String]) extends ShardAllocationStrategy
  with Serializable {

  val order: List[String] = shardAllocationOrder

  override def allocateShard(
                              requester: ActorRef,
                              shardId: ShardId,
                              currentShardAllocations: Map[ActorRef, immutable.IndexedSeq[ShardId]]): Future[ActorRef] = {
    val targetHost = order(shardId.toInt - 1)
    var selectedRegion: ActorRef = ActorRef.noSender
    val remoteHosts = currentShardAllocations.map(sa => sa._1.path.address.host).filter(h => h.isDefined).map(d => d.get).toList
    if (remoteHosts.contains(targetHost)) {
      logger.info("Selected node for shard " + shardId + " : " + targetHost)
      selectedRegion = currentShardAllocations.filter(c => c._1.path.address.host.isDefined && c._1.path.address.host.get == targetHost).head._1
    } else {
      logger.info("Selected node for shard " + shardId + " : local node")
      selectedRegion = currentShardAllocations.filter(c => c._1.path.address.host.isEmpty).head._1
    }
    Future.successful(selectedRegion)
  }

  override def rebalance(
                          currentShardAllocations: Map[ActorRef, immutable.IndexedSeq[ShardId]],
                          rebalanceInProgress: Set[ShardId]): Future[Set[ShardId]] = {
    if (rebalanceInProgress.size < maxSimultaneousRebalance) {
      val (_, leastShards) = currentShardAllocations.minBy { case (_, v) => v.size }
      val mostShards = currentShardAllocations
        .collect {
          case (_, v) => v.filterNot(s => rebalanceInProgress(s))
        }
        .maxBy(_.size)
      val difference = mostShards.size - leastShards.size
      if (difference > rebalanceThreshold) {
        val n = math.min(
          math.min(difference - rebalanceThreshold, rebalanceThreshold),
          maxSimultaneousRebalance - rebalanceInProgress.size)
        Future.successful(mostShards.sorted.take(n).toSet)
      } else
        Future.successful(Set.empty[ShardId])
    } else Future.successful(Set.empty[ShardId])
  }
}
