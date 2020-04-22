package com.github.jacobbishopxy

import MongoModel._

import org.mongodb.scala.{Completed, Document, MongoDatabase}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import spray.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits

/**
 * Created by Jacob Xie on 4/1/2020
 */
class MongoLoader(connectionString: String, databaseName: String) extends MongoConn {

  override val conn: String = connectionString
  override val dbName: String = databaseName

  import MongoLoader._


  /**
   * get collection's validator and indexes
   *
   * @param collectionName : String
   * @return
   */
  def showCollection(collectionName: String): Future[CollectionInfo] =
    showCollectionInfo(database, collectionName)

  /**
   * create a collection with validator
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def createCollection(collectionInfo: CollectionInfo): Future[String] = {
    import Implicits.global

    val co = genCreateCollectionOptions(collectionInfo)

    for {
      ifExist <- doesCollectionExist(collectionInfo.collectionName)
      _ <-
        if (!ifExist) database.createCollection(collectionInfo.collectionName, co).toFuture()
        else throw new RuntimeException("collection already exists!")
      ans <- createIndex(collectionInfo)
    } yield ans
  }

  /**
   * modify collection's validator, [[ValidatorContent]] cannot contain primary keys
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  def modifyValidator(collectionName: String,
                      validatorContent: ValidatorContent): Future[Document] = {
    import Implicits.global

    for {
      ifExist <- doesCollectionExist(collectionName)
      res <-
        if (ifExist) checkIfContainsPrimaryKeys(collectionName, validatorContent)
        else throw new RuntimeException("collection does not exist!")
      ans <- if (res) Future(Document("error" -> "Cannot modify primary keys"))
      else forceModifyCollectionValidator(collectionName, validatorContent)
    } yield ans
  }


  /**
   * modify a collection's validator by accepting new [[CollectionInfo]]
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def modifyCollection(collectionInfo: CollectionInfo): Future[Document] = {
    import Implicits.global

    val collectionName = collectionInfo.collectionName

    for {
      ifExist <- doesCollectionExist(collectionName)
      res <-
        if (ifExist) showCollection(collectionName)
          .map(_.fields)
          .map(genValidatorContent(_, collectionInfo.fields))
        else throw new RuntimeException("collection does not exist!")
      ans <- modifyValidator(collectionName, res)
    } yield ans
  }


  /**
   * create indexes for a collection
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def createIndex(collectionInfo: CollectionInfo): Future[String] = {
    val collectionName = collectionInfo.collectionName
    val idx = collectionInfo.fields.foldLeft(List.empty[Bson]) {
      case (acc, c) =>
        c.indexOption match {
          case None => acc
          case Some(io) =>
            val ia = if (io.ascending) Indexes.ascending(c.fieldName) else Indexes.descending(c.fieldName)
            acc :+ ia
        }
    }
    val idxOpt = IndexOptions().background(false).unique(true)

    val fut = collection(collectionInfo.collectionName)
      .createIndex(Indexes.compoundIndex(idx: _*), idxOpt)
      .toFuture()

    ifExistThen(collectionName, fut)
  }

  /**
   * show indexes for a collection
   *
   * @param collectionName : String
   * @return
   */
  def showIndex(collectionName: String): Future[Seq[MongoIndex]] = {
    val fut = showCollectionIndexes(database, collectionName)
    ifExistThen(collectionName, fut)
  }

  // todo: public method -- modify indexes


  /**
   * get all data with conditions
   *
   * @param collectionName : String
   * @param queryContent   : [[QueryContent]]
   * @return
   */
  def fetchData(collectionName: String,
                queryContent: QueryContent): Future[Seq[Document]] = {


    val filter = genFilterFromQueryContent(queryContent)
    val cf = collection(collectionName).find(filter)
    val fut = queryContent.limit.fold(cf) { i => cf.limit(i) }.toFuture()

    ifExistThen(collectionName, fut)
  }


  /**
   * insert seq of data
   *
   * @param collectionName : String
   * @param data           : Seq[JsValue]
   * @return
   */
  def insertData(collectionName: String,
                 data: Seq[JsValue]): Future[Completed] = {
    val d = data.map(_.toString()).map(Document(_))
    val fut = collection(collectionName).insertMany(d).toFuture()

    ifExistThen(collectionName, fut)
  }

  // def updateData(collectionName: String) = ???

  /**
   * delete matched data from collection
   *
   * @param collectionName : String
   * @param queryContent   : [[QueryContent]]
   * @return
   */
  def deleteData(collectionName: String,
                 queryContent: QueryContent): Future[DeleteResult] = {
    val fut = collection(collectionName)
      .deleteMany(genFilterFromQueryContent(queryContent))
      .toFuture()

    ifExistThen(collectionName, fut)
  }

  /**
   *
   * @param collectionName : String
   * @param fut            : Future[T]
   * @tparam T : type
   * @return
   */
  private def ifExistThen[T](collectionName: String, fut: Future[T]): Future[T] = {
    import Implicits.global

    for {
      ifExist <- doesCollectionExist(collectionName)
      ans <- if (ifExist) fut else throw new RuntimeException("collection does not exist!")
    } yield ans
  }

  /**
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  private def checkIfContainsPrimaryKeys(collectionName: String,
                                         validatorContent: ValidatorContent): Future[Boolean] = {
    import Implicits.global

    val vc = validatorContent.actions.map {
      case AddField(fieldName, _, _, _) => fieldName
      case DelField(fieldName) => fieldName
    }.toSet

    showIndex(collectionName)
      .map(_ (1))
      .map(_.key.keys.toSet)
      .map(_.intersect(vc).nonEmpty)
  }

  /**
   * modify collection's validator, unsafe!
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  private def forceModifyCollectionValidator(collectionName: String,
                                             validatorContent: ValidatorContent): Future[Document] = {
    import Implicits.global

    for {
      res <- showCollectionInfo(database, collectionName)
        .map(validatorUpdate(_, validatorContent))
        .map(genModifyValidatorDocument)
      ans <- database.runCommand(res).toFuture()
    } yield ans
  }

}


object MongoLoader extends MongoJsonSupport with QueryJsonSupport {

  import Utilities.MongoTypeMapping


  // todo: projecting fields (limit the fields returned)

  /**
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  private def genValidatorDocument(collectionInfo: CollectionInfo): Document = {
    val properties = collectionInfo.fields.foldLeft(List.empty[(String, Document)]) {
      case (l, i) =>
        val field = i.fieldName -> Document(
          "bsonType" -> MongoTypeMapping.intToString(i.fieldType),
          "title" -> i.nameAlias,
          "description" -> i.description.getOrElse("")
        )
        l :+ field
    }
    Document(
      "bsonType" -> "object",
      "required" -> collectionInfo.fields.map(_.fieldName),
      "properties" -> properties
    )
  }

  /**
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  private def genCreateCollectionOptions(collectionInfo: CollectionInfo) = {
    val jsonSchema = Filters.jsonSchema(genValidatorDocument(collectionInfo))
    val vo = ValidationOptions().validator(jsonSchema)
    CreateCollectionOptions().validationOptions(vo)
  }

  /**
   *
   * @param currentFields : List[FieldInfo]
   * @param targetFields  : List[FieldInfo]
   * @return
   */
  private def genValidatorContent(currentFields: List[FieldInfo],
                                  targetFields: List[FieldInfo]): ValidatorContent = {
    val cf = currentFields.filter(_.indexOption.isEmpty)
    val cfNames = cf.map(_.fieldName).toSet
    val nf = targetFields.filter(_.indexOption.isEmpty)
    val nfNames = nf.map(_.fieldName).toSet

    val nfMapping: Map[String, FieldInfo] = nf.map { fi =>
      fi.fieldName -> fi
    }.toMap

    val delFields = cfNames.diff(nfNames).map(DelField).toList
    val addField = nfNames.diff(cfNames).foldLeft(List.empty[AddField]) {
      case (l, name) => nfMapping.get(name) match {
        case None => l
        case Some(v) => l :+ AddField(
          fieldName = v.fieldName,
          nameAlias = v.nameAlias,
          fieldType = v.fieldType,
          description = v.description
        )
      }
    }

    ValidatorContent(delFields ::: addField)
  }

  /**
   * extract Mongo Collection Validator From Seq of Document
   *
   * @param seqDocument : Seq[Document]
   * @return
   */
  private def extractMCV(seqDocument: Seq[Document]): MongoCollectionValidator =
    seqDocument
      .head
      .toList(2)
      ._2
      .asDocument()
      .toJson
      .parseJson
      .convertTo[MongoCollectionValidator]

  /**
   * convert Mongo Collection Validator to Fields
   *
   * @param mongoCollectionValidator : [[MongoCollectionValidator]]
   * @return
   */
  private def convertMCV(mongoCollectionValidator: MongoCollectionValidator,
                         indexesMapping: Map[String, Int]): List[FieldInfo] =
    mongoCollectionValidator
      .validator
      .$jsonSchema
      .properties
      .map {
        case (k, v) =>
          val indexOption = indexesMapping.get(k)
            .map(i => IndexOption(if (i == 1) true else false))

          FieldInfo(
            fieldName = k,
            nameAlias = v.title,
            indexOption = indexOption,
            fieldType = MongoTypeMapping.stringToInt(v.bsonType),
            description = Some(v.description)
          )
      }.toList

  /**
   *
   * @param database       : [[MongoDatabase]]
   * @param collectionName : String
   * @return
   */
  private def showCollectionIndexes(database: MongoDatabase,
                                    collectionName: String): Future[Seq[MongoIndex]] = {
    import Implicits.global

    database
      .getCollection(collectionName)
      .listIndexes()
      .toFuture()
      .map(_.map(_.toJson.parseJson.convertTo[MongoIndex]))
  }

  /**
   *
   * @param database       : [[MongoDatabase]]
   * @param collectionName : String
   * @return
   */
  private def showCollectionInfo(database: MongoDatabase,
                                 collectionName: String): Future[CollectionInfo] = {
    import Implicits.global

    val indexes = showCollectionIndexes(database, collectionName)
      .map(_ (1).key)

    database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()
      .map(extractMCV)
      .zip(indexes)
      .map { case (validator, indexes) =>
        convertMCV(validator, indexes)
      }
      .map(i => CollectionInfo(collectionName, i))
  }

  /**
   *
   * @param currentCollectionInfo : [[CollectionInfo]]
   * @param validatorContent      : [[ValidatorContent]]
   * @return
   */
  private def validatorUpdate(currentCollectionInfo: CollectionInfo,
                              validatorContent: ValidatorContent): CollectionInfo = {
    val colList = validatorContent.actions.foldLeft(currentCollectionInfo.fields.reverse) {
      case (acc, a) => a match {
        case AddField(fn, na, ft, d) =>
          acc :+ FieldInfo(fieldName = fn, nameAlias = na, fieldType = ft, description = d)
        case DelField(fn) =>
          acc.filterNot(c => c.fieldName == fn)
      }
    }
    CollectionInfo(currentCollectionInfo.collectionName, colList)
  }

  /**
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  private def genModifyValidatorDocument(collectionInfo: CollectionInfo): Document =
    Document(
      "collMod" -> collectionInfo.collectionName,
      "validator" -> Document("$jsonSchema" -> genValidatorDocument(collectionInfo))
    )

  /**
   *
   * @param queryContent : [[QueryContent]]
   * @return
   */
  private def genFilterFromQueryContent(queryContent: QueryContent): Document =
    queryContent.filter match {
      case Some(v) => Document(v.toJson.toString)
      case None => Document()
    }

}

