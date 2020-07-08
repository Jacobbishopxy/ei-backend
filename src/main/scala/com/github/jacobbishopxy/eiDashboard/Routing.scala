package com.github.jacobbishopxy.eiDashboard

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

/**
 * Created by Jacob Xie on 3/18/2020
 */
object Routing {

  import com.github.jacobbishopxy.eiDashboard.Model._
  import com.github.jacobbishopxy.eiDashboard.Repo._

  private val paramDb = Symbol("db").as[String]
  private val paramCollection = Symbol("collection").as[String]
  private val paramTemplate = Symbol("template").as[String]
  private val paramPanel = Symbol("panel").as[String]

  val route: Route =
    pathPrefix("dashboard") {
      concat(
        path("grid-layouts") {
          parameter(paramDb, paramCollection) { (db, collection) =>
            complete(fetchAll(db, collection))
          }
        },
        path("grid-layout") {
          concat(
            get {
              parameter(paramDb, paramCollection, paramTemplate, paramPanel) { (db, collection, template, panel) =>
                complete(fetchItem(db, collection, template, panel))
              }
            },
            post {
              parameter(paramDb, paramCollection) { (db, collection) =>
                entity(as[GridLayout]) { gl =>
                  onSuccess(upsertItem(db, collection, gl)) { res =>
                    complete((StatusCodes.Created, res.toString))
                  }
                }
              }
            }
          )
        },
        path("show-template-panel") {
          parameter(paramDb, paramCollection, paramTemplate) { (db, collection, template) =>
            complete(fetchAllPanelByTemplate(db, collection, template))
          }
        },
        path("show-templates") {
          parameter(paramDb, paramCollection) { (db, collection) =>
            complete(fetchAllTemplates(db, collection))
          }
        }
      )
    }
}
