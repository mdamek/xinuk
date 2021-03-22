package pl.edu.agh.xinuk.gui

import java.awt.Color

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
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
    // new values
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
