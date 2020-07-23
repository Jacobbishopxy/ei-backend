package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoModel._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._


/**
 * Created by Jacob Xie on 3/23/2020
 */
object Routing extends MongoJsonSupport with ValidatorJsonSupport with QueryJsonSupport {

  import com.github.jacobbishopxy.eiAdmin.Repo._


  private val paramCollection = Symbol("collection").as[String]

  // todo: `handleExceptions()` is required for each route

  private val onShowCollections = path("show-collections") {
    get {
      onSuccess(mongoLoader.listCollections()) { res =>
        complete((StatusCodes.OK, res))
      }
    }
  }

  private val onDoesCollectionExist = path("does-collection-exist") {
    get {
      parameter(paramCollection) {collectionName =>
        onSuccess(mongoLoader.doesCollectionExist(collectionName)) {res =>
          complete((StatusCodes.OK, res.toJson))
        }
      }
    }
  }

  private val onShowCollection = path("show-collection") {
    get {
      parameter(paramCollection) { collectionName =>
        onSuccess(mongoLoader.showCollection(collectionName)) { res =>
          complete((StatusCodes.OK, res))
        }
      }
    }
  }

  private val onCreateCollection = path("create-collection") {
    post {
      entity(as[CollectionInfo]) { collectionInfo =>
        onSuccess(mongoLoader.createCollection(collectionInfo)) { res =>
          complete((StatusCodes.Created, res))
        }
      }
    }
  }

  private val onDropCollection = path("drop-collection") {
    get {
      parameter(paramCollection) {collectionName =>
        onSuccess(mongoLoader.dropCollection(collectionName)) {res =>
          complete((StatusCodes.OK, res.toString))
        }
      }
    }
  }

  private val onShowFieldType = path("show-field-type") {
    get {
      parameter(paramCollection) {collectionName =>
        onSuccess(mongoLoader.showFieldType(collectionName)) {res =>
          complete((StatusCodes.OK, res.toJson))
        }
      }
    }
  }

  private val onModifyValidator = path("modify-validator") {
    post {
      parameter(paramCollection) { collectionName =>
        entity(as[ValidatorContent]) { vc =>
          onSuccess(mongoLoader.modifyValidator(collectionName, vc)) { res =>
            complete((StatusCodes.Accepted, res.toString))
          }
        }
      }
    }
  }

  private val onModifyCollection = path("modify-collection") {
    post {
      entity(as[CollectionInfo]) {collectionInfo =>
        onSuccess(mongoLoader.modifyCollection(collectionInfo)) {res =>
          complete((StatusCodes.Accepted, res.toString))
        }
      }
    }
  }


  private val onShowIndex = path("show-index") {
    get {
      parameter(paramCollection) { collectionName =>
        onSuccess(mongoLoader.showIndex(collectionName)) { res =>
          complete((StatusCodes.OK, res.toJson))
        }
      }
    }
  }


  private val onInsertData = path("insert-data") {
    post {
      parameter(paramCollection) { collectionName =>
        entity(as[Seq[JsValue]]) { data =>
          onSuccess(mongoLoader.insertData(collectionName, data)) { res =>
            complete((StatusCodes.Accepted, res.toString))
          }
        }
      }
    }
  }

  private val onQueryData = path("query-data") {
    post {
      parameter(paramCollection) { collectionName =>
        entity(as[QueryContent]) { qc =>
          onSuccess(mongoLoader.fetchData(collectionName, qc)) { res =>
            complete((StatusCodes.OK, res.map(_.toJson.parseJson)))
          }
        }
      }
    }
  }

  private val onDeleteData = path("delete-data") {
    post {
      parameter(paramCollection) { collectionName =>
        entity(as[QueryContent]) { qc =>
          onSuccess(mongoLoader.deleteData(collectionName, qc)) { res =>
            complete((StatusCodes.OK, res.toString))
          }
        }
      }
    }
  }


  val route: Route =
    pathPrefix("admin") {
      concat(
        onShowCollections,
        onDoesCollectionExist,
        onShowCollection,
        onCreateCollection,
        onDropCollection,
        onShowFieldType,
        onModifyValidator,
        onModifyCollection,
        onShowIndex,
        onInsertData,
        onQueryData,
        onDeleteData,
      )
    }
}
