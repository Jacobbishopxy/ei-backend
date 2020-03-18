package com.github.jacobbishopxy.gridLayout

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 3/18/2020
 */
object Routing {

  import com.github.jacobbishopxy.gridLayout.Repo._

  val route: Route =
    concat(
      path("grid-layout-all") {
        val result: Future[Seq[GridLayout]] = fetchAll()
        complete(result)
      },
      path("grid-layout") {
        concat(
          get {
            parameter(Symbol("id").as[String]) { id =>
              val result = fetchItem(id)
              complete(result)
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
