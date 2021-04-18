package pl.edu.agh.xinuk.simulation

import akka.actor.Status.Success
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import pl.edu.agh.xinuk.model.WorkerId

class WorkersManager(existingSystem: ActorSystem, workerRegionRef: ActorRef, workersId :List[WorkerId]) {

  implicit val system: ActorSystem = existingSystem

  val interface = "192.168.100.180"
  val port = 8005

  val route: Route = {
    path("nextIteration") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }
    path("setIterationDelay") {
      get {
        parameter("time".as[Int]) { timeInMs =>
          workersId.foreach(workerId => {
            //wyslij wiadomosc
            WorkerActor.send(workerRegionRef, workerId, "")
          })
          complete(StatusCodes.OK)
        }
      }
    }
  }
  Http().newServerAt(interface, port).bindFlow(route)

  println(s"Server online at http://${interface}:${port}/\nPress RETURN to stop...")
}

