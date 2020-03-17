import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
 * Created by Jacob Xie on 3/17/2020
 */
object DevDynamicRouting extends App {

  import DevDynamicRoutingRepo._

  implicit val system: ActorSystem = ActorSystem("Server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  val (host, port) = ("localhost", 2020)
  val bindFuture = Http().bindAndHandle(route, host, port)

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}

object DevDynamicRoutingRepo {

  case class MockDefinition(path: String, requests: Seq[JsValue], response: Seq[JsValue])

  implicit val format: RootJsonFormat[MockDefinition] = jsonFormat3(MockDefinition)

  @volatile var state = Map.empty[String, Map[JsValue, JsValue]]

  // fixed route to update state
  val fixedRoute: Route = post {
    pathSingleSlash {
      entity(as[MockDefinition]) {mock =>
        val mapping = mock.requests.zip(mock.response).toMap
        state = state + (mock.path -> mapping)
        complete("ok")
      }
    }
  }

  // dynamic routing based on current state
  val dynamicRoute: Route = ctx => {
    val routes = state.map {
      case (segment, responses) =>
        post {
          path(segment) {
            entity(as[JsValue]) {input =>
              complete(responses.get(input))
            }
          }
        }
    }
    concat(routes.toList: _*)(ctx)
  }

  val route: Route = fixedRoute ~ dynamicRoute

}

