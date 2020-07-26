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


  private val routeStore = path(RouteName.industryStore) {
    concat(
      get {
        parameter(paramCollection, paramIdentity, paramCategory, paramSymbol, paramDate) {
          (cl, id, ct, syb, dt) =>
            val ac = Anchor(id, CategoryFinder.Finder(ct).category)
            val acc = AnchorConfig(syb, dt)

            complete(fetchIndustryStore(cl, ac, Some(acc)))
        }
      },
      post {
        parameter(paramCollection) { cl =>
          entity(as[Store]) { st =>
            onSuccess(replaceIndustryStore(cl, st)) { res =>
              complete((StatusCodes.Created, res.toString))
            }
          }
        }
      }
    )
  }

  private val routeStoreRemove = path(RouteName.industryStoreRemove) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Anchor]) { ac =>
          onSuccess(deleteIndustryStore(cl, ac, None)) { res =>
            complete((StatusCodes.Created, res.toString))
          }
        }
      }
    }
  }

  private val routeLayout = path(RouteName.templateLayout) {
    concat(
      get {
        parameter(paramCollection, paramTemplate, paramPanel) {
          (cl, tpl, pn) =>
            val tp = TemplatePanel(tpl, pn)
            complete(fetchTemplateLayout(cl, tp))
        }
      },
      post {
        parameter(paramCollection) { cl =>
          entity(as[Layout]) { lo =>
            onSuccess(replaceTemplateLayout(cl, lo)) { res =>
              complete((StatusCodes.Created, res.toString))
            }
          }
        }
      }
    )
  }

  private val routeLayoutRemove = path(RouteName.templateLayoutRemove) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[TemplatePanel]) { tp =>
          onSuccess(deleteTemplateLayout(cl, tp)) { res =>
            complete((StatusCodes.Created, res.toString))
          }
        }
      }
    }
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
        routeStoreRemove,
        routeLayout,
        routeLayoutRemove,

      )
    }
}
