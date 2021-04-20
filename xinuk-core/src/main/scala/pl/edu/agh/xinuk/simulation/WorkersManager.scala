package pl.edu.agh.xinuk.simulation

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import pl.edu.agh.xinuk.model.WorkerId

import java.net.InetAddress
import scala.concurrent.Future

class WorkersManager(existingSystem: ActorSystem, workerRegionRef: ActorRef, workersId: List[WorkerId], port: Int) {

  implicit val system: ActorSystem = existingSystem

  val interface: String = InetAddress.getLocalHost.getHostAddress

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(GET, path@Uri.Path("/setSimulationDelay"), _, _, _) if path.rawQueryString.get.split("=")(0) == "delay" =>
      val delayInMs = path.rawQueryString.get.split("=")(1).toLong
      workersId.foreach(workerId => {
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.SetSimulationDelay(delayInMs))
      })
      HttpResponse(200)

    case HttpRequest(GET, Uri.Path("/startSteppedSimulation"), _, _, _) =>
      workersId.foreach(workerId => {
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.StartSteppedSimulation)
      })
      HttpResponse(200)

    case HttpRequest(GET, Uri.Path("/stopSteppedSimulation"), _, _, _) =>
      workersId.foreach(workerId => {
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.StopSteppedSimulation)
      })
      HttpResponse(200)

    case HttpRequest(GET, Uri.Path("/makeIteration"), _, _, _) =>
      workersId.foreach(workerId => {
        WorkerActor.send(workerRegionRef, workerId, WorkerActor.MakeIteration)
      })
      HttpResponse(200)

    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")
  }

  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt(interface, port).bindSync(requestHandler)

  println(s"Server online at http://$interface:$port/")
}

