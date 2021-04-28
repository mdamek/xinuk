package pl.edu.agh.xinuk.gui

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.LastChunk.data
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Sink, Source}
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import pl.edu.agh.xinuk.algorithm.Metrics
import pl.edu.agh.xinuk.config.XinukConfig
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.model.grid.{GridCellId, GridWorldShard}
import pl.edu.agh.xinuk.simulation.WorkerActor.GridInfo

import java.awt.Color
import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

class LedPanelGuiActor private(bounds: GridWorldShard.Bounds,
                               ledPanelPort: String)
                              (implicit config: XinukConfig) extends Actor with ActorLogging {

  override def receive: Receive = started

  override def preStart(): Unit = {
    log.info("GUI started")
  }

  override def postStop(): Unit = {
    log.info("GUI stopped")
  }

  def started: Receive = {
    case GridInfo(iteration, cells, _) =>
      updateLedPanel(iteration, cells)
  }

  private val (xOffset, yOffset, xSize, ySize) = (bounds.xMin, bounds.yMin, bounds.xSize, bounds.ySize)
  private val connectedLedPanelHost = "127.0.0.1"
  private val connectedLedPanelPort = ledPanelPort

  implicit val system: ActorSystem = context.system
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher
  implicit val formats: DefaultFormats.type = DefaultFormats

  val flow: Flow[(HttpRequest, NotUsed), (Try[HttpResponse], NotUsed), NotUsed] = Http().superPool[NotUsed]()

  def updateLedPanel(iteration: Long, cells: Map[CellId, Color]): Unit = {
    val pointsMatrix = Array.ofDim[Int](xSize, ySize)

    cells foreach {
      case (GridCellId(x, y), color) =>
        pointsMatrix(x - xOffset)(y - yOffset) = color.getRGB
      case _ =>
    }

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://$connectedLedPanelHost:$connectedLedPanelPort/xinukIteration",
      entity = HttpEntity(ContentTypes.`application/json`,
        write(Iteration(iteration.toInt, pointsMatrix))))

    val dat = write(Iteration(iteration.toInt, pointsMatrix))

    requests.post("http://0.0.0.0:8012", data = dat, headers = Map("Content-Type" -> "application/json"))

    //Source.single((request, NotUsed))
    //.via(flow)
    //.runWith(Sink.head)
    //.flatMap { result => result._1.get.entity.discardBytes().future() }
  }
}

object LedPanelGuiActor {

  final case class GridInfo private(iteration: Long, cells: Set[Cell], metrics: Metrics)

  final case class WorkerAddress private(host: String, port: String)

  def props(bounds: GridWorldShard.Bounds, ledPanelPort: String)
           (implicit config: XinukConfig): Props = {
    Props(new LedPanelGuiActor(bounds, ledPanelPort))
  }
}

case class Iteration(iteration: Int, points: Array[Array[Int]])