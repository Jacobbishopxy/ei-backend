package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoModel._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.JsValue

/**
 * Created by Jacob Xie on 3/23/2020
 */
object Routing extends ValidatorJsonSupport with ConjunctionsJsonSupport {

  import com.github.jacobbishopxy.eiAdmin.Repo._

  val route: Route =
    pathPrefix("admin") {
      concat(
        path("show-collections") {
          get {
            complete(mongoLoader.listCollections())
          }
        },
        path("create-collection") {
          post {
            entity(as[Cols]) { cols =>
              onSuccess(mongoLoader.createCollection(cols)) { res =>
                complete((StatusCodes.Created, res))
              }
            }
          }
        },
        path("show-indexes") {
          get {
            parameter(Symbol("collection").as[String]) { coll =>
              complete(mongoLoader.getCollectionIndexes(coll))
            }
          }
        },
        path("insert-data") {
          post {
            parameter(Symbol("collection").as[String]) { coll =>
              entity(as[Seq[JsValue]]) {data =>
                println(data.toString)
                onSuccess(mongoLoader.insertData(coll, data)) { res =>
                  complete((StatusCodes.Accepted, res.toString))
                }
              }
            }
          }
        },

      )
    }
}
