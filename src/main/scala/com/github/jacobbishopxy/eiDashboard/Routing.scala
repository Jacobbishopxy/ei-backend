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


  private val routeStoreFetch = path(RouteName.industryStoreFetch) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Anchor]) { ac =>
          onSuccess(fetchIndustryStore(cl, ac)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeStoresFetch = path(RouteName.industryStoresFetch) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Seq[Anchor]]) { acs =>
          onSuccess(fetchIndustryStores(cl, acs)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeStoreModify = path(RouteName.industryStoreModify) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Store]) { st =>
          onSuccess(replaceIndustryStore(cl, st)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeStoresModify = path(RouteName.industryStoresModify) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Seq[Store]]) { sts =>
          onSuccess(replaceIndustryStores(cl, sts)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeStoreRemove = path(RouteName.industryStoreRemove) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Anchor]) { ac =>
          onSuccess(deleteIndustryStore(cl, ac)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeStoresRemove = path(RouteName.industryStoresRemove) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Seq[Anchor]]) { acs =>
          onSuccess(deleteIndustryStores(cl, acs)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeLayoutFetch = path(RouteName.templateLayoutFetch) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[TemplatePanel]) { tp =>
          onSuccess(fetchTemplateLayout(cl, tp)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }

  }

  private val routeLayoutModify = path(RouteName.templateLayoutModify) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[Layout]) { lo =>
          onSuccess(replaceTemplateLayout(cl, lo)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeLayoutRemove = path(RouteName.templateLayoutRemove) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[TemplatePanel]) { tp =>
          onSuccess(deleteTemplateLayout(cl, tp)) { res =>
            complete((StatusCodes.Accepted, res))
          }
        }
      }
    }
  }

  private val routeLayoutStoreModify = path(RouteName.templateLayoutWithIndustryStoreModify) {
    post {
      parameter(paramCollection) { cl =>
        entity(as[LayoutWithStore]) { tp =>
          onSuccess(replaceTemplateLayoutWithIndustryStore(cl, tp)) { res =>
            complete((StatusCodes.Accepted, res))
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

        routeStoreFetch,
        routeStoresFetch,
        routeStoreModify,
        routeStoresModify,
        routeStoreRemove,
        routeStoresRemove,
        routeLayoutFetch,
        routeLayoutModify,
        routeLayoutRemove,
        routeLayoutStoreModify,

      )
    }
}
