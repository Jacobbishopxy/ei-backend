package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoModel._

import akka.http.scaladsl.common.NameReceptacle
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._

/**
 * Created by Jacob Xie on 3/23/2020
 */
object Routing extends ValidatorJsonSupport with ConjunctionsJsonSupport {

  import com.github.jacobbishopxy.eiAdmin.Repo._

  private val paramCollection: NameReceptacle[String] = Symbol("collection").as[String]

  private val onShowCollections = path("show-collections") {
    get {
      complete(mongoLoader.listCollections())
    }
  }

  private val onCreateCollection = path("create-collection") {
    post {
      entity(as[Cols]) { cols =>
        onSuccess(mongoLoader.createCollection(cols)) { res =>
          complete((StatusCodes.Created, res))
        }
      }
    }
  }

  private val onShowIndex = path("show-index") {
    get {
      parameter(paramCollection) { coll =>
        complete(mongoLoader.getCollectionIndexes(coll))
      }
    }
  }

  private val onModifyValidator = path("modify-validator") {
    post {
      parameter(paramCollection) { coll =>
        entity(as[ValidatorContent]) { va =>
          onSuccess(mongoLoader.modifyValidator(coll, va)) { res =>
            complete((StatusCodes.Accepted, res.toString))
          }
        }
      }
    }
  }

  private val onInsertData = path("insert-data") {
    post {
      parameter(paramCollection) { coll =>
        entity(as[Seq[JsValue]]) { data =>
          onSuccess(mongoLoader.insertData(coll, data)) { res =>
            complete((StatusCodes.Accepted, res.toString))
          }
        }
      }
    }
  }

  private val onQueryData = path("query-data") {
    post {
      parameter(paramCollection) { coll =>
        entity(as[QueryContent]) { qc =>
          onSuccess(mongoLoader.fetchData(coll, qc)) { res =>
            complete((StatusCodes.Accepted, res.map(_.toJson)))
          }
        }
      }
    }
  }

  private val onDeleteData = path("delete-data") {
    post {
      parameter(paramCollection) { coll =>
        entity(as[QueryContent]) { qc =>
          onSuccess(mongoLoader.deleteData(coll, qc)) { res =>
            complete((StatusCodes.Accepted, res.toString))
          }
        }
      }
    }
  }


  val route: Route =
    pathPrefix("admin") {
      concat(
        onShowCollections,
        onCreateCollection,
        onShowIndex,
        onModifyValidator,
        onInsertData,
        onQueryData,
        onDeleteData,
      )
    }
}
