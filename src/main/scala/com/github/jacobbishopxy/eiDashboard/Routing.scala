package com.github.jacobbishopxy.eiDashboard

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route

/**
 * Created by Jacob Xie on 3/18/2020
 */
object Routing extends ProModel with SprayJsonSupport {

  import com.github.jacobbishopxy.eiDashboard.Model._
  import com.github.jacobbishopxy.eiDashboard.Repo._

  import com.github.jacobbishopxy.eiDashboard.Namespace._
  import com.github.jacobbishopxy.eiDashboard.ProModel._
  import com.github.jacobbishopxy.eiDashboard.ProRepo._

  private val paramDb = Symbol(FieldName.db).as[String]
  private val paramCollection = Symbol(FieldName.collection).as[String]
  private val paramTemplate = Symbol(FieldName.template).as[String]
  private val paramPanel = Symbol(FieldName.panel).as[String]
  private val paramIdentity = Symbol(FieldName.identity).as[String]
  private val paramCategory = Symbol(FieldName.category).as[String]
  private val paramSymbol = FieldName.symbol.?
  private val paramDate = FieldName.date.?


  private val routeStore = path(RouteName.store) {
    concat(
      get {
        parameter(paramDb, paramCollection, paramIdentity, paramCategory, paramSymbol, paramDate) {
          (d, cl, id, ct, syb, dt) =>
            val dc = DbCollection(DbFinder.Finder(d).db, cl)
            val ac = Anchor(
              identity = id,
              category = CategoryFinder.Finder(ct).category,
              symbol = syb,
              date = dt
            )
            complete(fetchStore(dc, ac))
        }
      },
      post {
        parameter(paramDb, paramCollection) { (d, cl) =>
          entity(as[Store]) { st =>
            val dc = DbCollection(DbFinder.Finder(d).db, cl)
            onSuccess(upsertStore(dc, st)) { res =>
              complete((StatusCodes.Created, res.toString))
            }
          }
        }
      }
    )
  }

  private val routeLayout = path(RouteName.layout) {
    concat(
      get {
        parameter(paramDb, paramCollection, paramTemplate, paramPanel) {
          (d, cl, tpl, pn) =>
            val dc = DbCollection(DbFinder.Finder(d).db, cl)
            val tp = TemplatePanel(tpl, pn)
            complete(fetchLayout(dc, tp))
        }
      },
      post {
        parameter(paramDb, paramCollection) { (d, cl) =>
          entity(as[Layout]) { lo =>
            val dc = DbCollection(DbFinder.Finder(d).db, cl)
            onSuccess(upsertLayout(dc, lo)) { res =>
              complete((StatusCodes.Created, res.toString))
            }
          }
        }
      }
    )
  }

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
              parameter(paramDb, paramCollection, paramTemplate, paramPanel) {
                (db, collection, template, panel) =>
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
        },

        routeStore,
        routeLayout,

      )
    }
}
