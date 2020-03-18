package com.github.jacobbishopxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.Success

/**
 * Created by Jacob Xie on 3/12/2020
 */
object Server extends App {

  import gridLayout.GridLayoutRepo._

  implicit val system: ActorSystem = ActorSystem("Server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val route =
    concat(
      path("grid-layout-all") {
        val result: Future[Seq[GridLayout]] = fetchAll()
        complete(result)
      },
      path("grid-layout") {
        get {
          parameter(Symbol("id").as[String]) { id =>
            val result = fetchItem(id)
            complete(result)
          }
        }
      }
    )

  val (host, port) = ("localhost", 2020)
  val bindFuture = Http().bindAndHandle(route, host, port)

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
