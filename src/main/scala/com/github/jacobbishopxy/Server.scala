package com.github.jacobbishopxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
 * Created by Jacob Xie on 3/12/2020
 */
object Server extends App {

  import gridLayout.Routing.{route => gridLayoutRoute}

  implicit val system: ActorSystem = ActorSystem("Server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val route = cors() {
    concat(
      gridLayoutRoute
    )
  }

  val (host, port) = ("localhost", 2020)
  val bindFuture = Http().bindAndHandle(route, host, port)

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
