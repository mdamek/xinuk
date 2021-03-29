package pl.edu.agh.xinuk.gui

import java.awt.Color
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import net.liftweb.json.DefaultFormats
import pl.edu.agh.xinuk.algorithm.Metrics
import pl.edu.agh.xinuk.config.XinukConfig
import pl.edu.agh.xinuk.gui.GuiActor.GridInfo
import pl.edu.agh.xinuk.gui.LedPanelGuiActor.WorkerAddress
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.model.grid.GridCellId
import pl.edu.agh.xinuk.simulation.WorkerActor.{MsgWrapper, SubscribeGridInfo}

import scala.util.Random

class LedPanelGuiActor private(worker: ActorRef,
                               workerId: WorkerId,
                               worldSpan: ((Int, Int), (Int, Int)),
                               cellToColor: PartialFunction[CellState, Color],
                               ledPanelPort: String)
                              (implicit config: XinukConfig) extends Actor with ActorLogging {

  override def receive: Receive = started

  override def preStart(): Unit = {
    worker ! MsgWrapper(workerId, SubscribeGridInfo())
    log.info("GUI started")
  }

  override def postStop(): Unit = {
    log.info("GUI stopped")
  }

  def started: Receive = {
    case WorkerAddress(host, port) =>
      connectedLedPanelHost = host
      log.info("We have address of Worker Actor! " + host + ":" + port)

    case GridInfo(iteration, cells, metrics) =>
      updateLedPanel(iteration, cells)

    case HttpResponse(code, _, _, _) =>
      log.info("Response code: " + code)
  }

  private val ((xOffset, yOffset), (xSize, ySize)) = worldSpan
  private val obstacleColor = new swing.Color(0, 0, 0)
  private val emptyColor = new swing.Color(255, 255, 255)
  private var connectedLedPanelHost = ""
  private var connectedLedPanelPort = ledPanelPort



  private def defaultColor: CellState => Color =
    state => state.contents match {
      case Obstacle => obstacleColor
      case Empty => emptyColor
      case other =>
        val random = new Random(other.getClass.hashCode())
        val hue = random.nextFloat()
        val saturation = 1.0f
        val luminance = 0.6f
        Color.getHSBColor(hue, saturation, luminance)
    }

  def updateLedPanel(iteration: Long, cells: Set[Cell]): Unit = {
    import akka.pattern.pipe
    import context.dispatcher
    import net.liftweb.json.Serialization.write

    val http = Http(context.system)

    val pointsMatrix = Array.ofDim[Int](xSize, ySize)

    cells foreach {
      case Cell(GridCellId(x, y), state) =>
        val color: Color = cellToColor.applyOrElse(state, defaultColor)
        pointsMatrix(x - xOffset)(y - yOffset) = color.getRGB
      case _ =>
    }

    implicit val formats: DefaultFormats.type = DefaultFormats

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://$connectedLedPanelHost:$connectedLedPanelPort/xinukIteration",
      entity = HttpEntity(ContentTypes.`application/json`,
        write(Iteration(iteration.toInt, pointsMatrix)))
    )

    http.singleRequest(request)
      .pipeTo(self)
  }
}

object LedPanelGuiActor {

  final case class GridInfo private(iteration: Long, cells: Set[Cell], metrics: Metrics)
  final case class WorkerAddress private(host: String, port: String)

  def props(worker: ActorRef, workerId: WorkerId, worldSpan: ((Int, Int), (Int, Int)), cellToColor: PartialFunction[CellState, Color], ledPanelPort: String)
           (implicit config: XinukConfig): Props = {
    Props(new LedPanelGuiActor(worker, workerId, worldSpan, cellToColor, ledPanelPort))
  }
}
case class Iteration(iteration: Int, points:  Array[Array[Int]])