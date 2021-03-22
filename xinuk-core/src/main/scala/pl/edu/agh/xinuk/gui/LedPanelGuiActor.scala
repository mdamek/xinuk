package pl.edu.agh.xinuk.gui

import java.awt.Color

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import net.liftweb.json.Serialization.write
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import net.liftweb.json.DefaultFormats
import pl.edu.agh.xinuk.algorithm.Metrics
import pl.edu.agh.xinuk.config.XinukConfig
import pl.edu.agh.xinuk.gui.GuiActor.GridInfo
import pl.edu.agh.xinuk.gui.LedPanelGuiActor.WorkerAddress
import pl.edu.agh.xinuk.model._
import pl.edu.agh.xinuk.simulation.WorkerActor.{MsgWrapper, SubscribeGridInfo}

class LedPanelGuiActor private(worker: ActorRef,
                               workerId: WorkerId,
                               worldSpan: ((Int, Int), (Int, Int)),
                               cellToColor: PartialFunction[CellState, Color])
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
      log.info("We have address of Worker Actor! " + host + port)

    case GridInfo(iteration, cells, metrics) =>
      updateLedPanel(iteration, cells)

    case HttpResponse(code, _, _, _) =>
      log.info("Response code: " + code)
  }

  def updateLedPanel(iteration: Long, cells: Set[Cell]): Unit = {
    import akka.pattern.pipe
    import context.dispatcher

    val http = Http(context.system)

    implicit val formats = DefaultFormats
    val jsonString = write(cells)

    print(jsonString)

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "http://192.168.100.199:8080/easy",
      entity = HttpEntity(ContentTypes.`application/json`, jsonString)
    )

    http.singleRequest(request)
      .pipeTo(self)
  }
}

object LedPanelGuiActor {

  final case class GridInfo private(iteration: Long, cells: Set[Cell], metrics: Metrics)
  final case class WorkerAddress private(host: String, port: String)

  def props(worker: ActorRef, workerId: WorkerId, worldSpan: ((Int, Int), (Int, Int)), cellToColor: PartialFunction[CellState, Color])
           (implicit config: XinukConfig): Props = {
    Props(new LedPanelGuiActor(worker, workerId, worldSpan, cellToColor))
  }
}
