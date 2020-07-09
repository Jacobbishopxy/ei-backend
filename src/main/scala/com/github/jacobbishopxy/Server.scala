package com.github.jacobbishopxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import scala.concurrent.ExecutionContextExecutor

/**
 * Created by Jacob Xie on 3/12/2020
 */
object Server extends App {

  import eiDashboard.Routing.{route => eiDashboardRoute}
  import eiAdmin.Routing.{route => eiAdminRoute}
  import eiFile.Routing.{route => eiFileRoute}

  implicit val system: ActorSystem = ActorSystem("Server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val route = cors() {
    pathPrefix("ei") {
      concat(
        eiDashboardRoute,
        eiAdminRoute,
        eiFileRoute
      )
    }
  }

  val (host, port) = ("0.0.0.0", 2020)
  val bindFuture = Http().bindAndHandle(route, host, port)

  bindFuture.failed.foreach { ex =>
    println(ex, s"Failed to bind to $host, $port")
  }

  println(s"Server online at http://$host:$port/")
}
