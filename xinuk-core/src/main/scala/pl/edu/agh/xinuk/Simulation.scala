package pl.edu.agh.xinuk

import java.awt.Color
import java.io.File

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.cluster.sharding.ShardCoordinator.ShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.ShardId
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.readers.ValueReader
import pl.edu.agh.xinuk.algorithm.{Metrics, PlanCreator, PlanResolver, WorldCreator}
import pl.edu.agh.xinuk.config.{GuiType, XinukConfig}
import pl.edu.agh.xinuk.gui.{GuiActor, LedPanelGuiActor}
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.model.grid.GridWorldShard
import pl.edu.agh.xinuk.simulation.WorkerActor

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
  private val system = ActorSystem(rawConfig.getString("application.name"), rawConfig)

  implicit val config: ConfigType = {
    val applicationConfig = rawConfig.getConfig(configPrefix)
    logger.info(WorkerActor.MetricsMarker, applicationConfig.root().render(ConfigRenderOptions.concise()))
    logger.info(WorkerActor.MetricsMarker, logHeader)

    import net.ceedubs.ficus.Ficus._
    Try(applicationConfig.as[ConfigType]("config")) match {
      case Success(parsedConfig) =>
        parsedConfig
      case Failure(parsingError) =>
        logger.error("Config parsing error.", parsingError)
        System.exit(2)
        throw new IllegalArgumentException
    }
  }


  val ownShardingStrategy: OwnStrategy = new OwnStrategy(ClusterShardingSettings(system).tuningParameters.leastShardAllocationRebalanceThreshold,
    ClusterShardingSettings(system).tuningParameters.leastShardAllocationMaxSimultaneousRebalance)

  private val workerRegionRef: ActorRef =
    ClusterSharding(system).start(
      typeName = WorkerActor.Name,
      entityProps = WorkerActor.props[ConfigType](workerRegionRef, planCreatorFactory(), planResolverFactory(), emptyMetrics, signalPropagation),
      settings = ClusterShardingSettings(system),
      extractShardId = WorkerActor.extractShardId,
      extractEntityId = WorkerActor.extractEntityId,
      allocationStrategy = ownShardingStrategy,
      handOffStopMessage = PoisonPill

    )

  def start(): Unit = {
    if (config.isSupervisor) {
      val workerToWorld: Map[WorkerId, WorldShard] = worldCreator.prepareWorld().build()

      workerToWorld.foreach( { case (workerId, world) =>
        (config.guiType, world) match {
          case (GuiType.None, _) =>
          case (GuiType.Grid, gridWorld: GridWorldShard) =>
            system.actorOf(GuiActor.props(workerRegionRef, workerId, gridWorld.span, cellToColor))
          case (GuiType.LedPanel, gridWorld: GridWorldShard) =>
            system.actorOf(LedPanelGuiActor.props(workerRegionRef, workerId, gridWorld.span, cellToColor, config.ledPanelPort))
          case _ => logger.warn("GUI type incompatible with World format.")
        }
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.WorkerInitialized(world))
      })
    }
  }

  private def logHeader: String = s"worker:${metricHeaders.mkString(";")}"
}

class OwnStrategy(rebalanceThreshold: Int, maxSimultaneousRebalance: Int)
  extends ShardAllocationStrategy
    with Serializable {
  override def allocateShard(
                              requester: ActorRef,
                              shardId: ShardId,
                              currentShardAllocations: Map[ActorRef, immutable.IndexedSeq[ShardId]]): Future[ActorRef] = {
    val (regionWithLeastShards, _) = currentShardAllocations.minBy { case (s, v) => v.size & s.path.address.hashCode }

    Future.successful(regionWithLeastShards)
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

