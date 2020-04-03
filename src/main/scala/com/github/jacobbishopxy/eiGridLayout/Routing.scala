package com.github.jacobbishopxy.eiGridLayout

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

/**
 * Created by Jacob Xie on 3/18/2020
 */
object Routing {

  import com.github.jacobbishopxy.eiGridLayout.Repo._

  val route: Route =
    pathPrefix("utils") {
      concat(
        path("grid-layout-all") {
          complete(fetchAll())
        },
        path("grid-layout") {
          concat(
            get {
              parameter(Symbol("panel").as[String]) { id =>
                complete(fetchItem(id))
              }
            },
            post {
              entity(as[GridLayout]) { gl =>
                onSuccess(upsertItem(gl)) { res =>
                  complete((StatusCodes.Created, res.toString))
                }
              }
            }
          )
        }
      )
    }
}
