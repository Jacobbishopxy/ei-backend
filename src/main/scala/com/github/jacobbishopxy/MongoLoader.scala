package com.github.jacobbishopxy

import MongoModel._

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Completed, Document, MongoDatabase}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import spray.json._

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 4/1/2020
 */
class MongoLoader(connectionString: String, databaseName: String) extends MongoConn {

  override val conn: String = connectionString
  override val dbName: String = databaseName

  import MongoLoader._

  /**
   * create a collection with validator
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def createCollection(collectionInfo: CollectionInfo): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val co = genCreateCollectionOptions(collectionInfo)
    val createColl = database.createCollection(collectionInfo.collectionName, co).toFuture()
    val createIdx = createIndex(collectionInfo)

    createColl.flatMap(_ => createIdx)
  }


  /**
   * create indexes for a collection
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def createIndex(collectionInfo: CollectionInfo): Future[String] = {
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

    collection(collectionInfo.collectionName).createIndex(Indexes.compoundIndex(idx: _*), idxOpt).toFuture()
  }

  /**
   * show indexes for a collection
   *
   * @param collectionName : String
   * @return
   */
  def getCollectionIndexes(collectionName: String): Future[Seq[MongoIndex]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val fut = collection(collectionName).listIndexes().toFuture()
    fut.map(convertMongoIndexes)
  }

  // todo: public method -- modify indexes

  private def getFilterFromQueryContent(queryContent: QueryContent): Bson =
    queryContent.filter match {
      case Some(v) => getFilter(v)
      case None => BsonDocument()
    }

  /**
   * get all data with conditions
   *
   * @param collectionName : String
   * @param queryContent   : [[QueryContent]]
   * @return
   */
  def fetchData(collectionName: String, queryContent: QueryContent): Future[Seq[Document]] = {
    val filter = getFilterFromQueryContent(queryContent)
    val cf = collection(collectionName).find(filter)
    val res = queryContent.limit.fold(cf) { i => cf.limit(i) }
    res.toFuture()
  }

  /**
   * get collection's validator
   *
   * @param collectionName : String
   * @return
   */
  def getCollectionValidator(collectionName: String): Future[CollectionInfo] =
    getCollectionInfo(database, collectionName)

  /**
   * insert seq of data
   *
   * @param collectionName : String
   * @param data           : Seq[JsValue]
   * @return
   */
  def insertData(collectionName: String, data: Seq[JsValue]): Future[Completed] = {
    val d = data.map(_.toString()).map(Document(_))
    collection(collectionName).insertMany(d).toFuture()
  }

  // def updateData(collectionName: String) = ???

  /**
   * delete matched data from collection
   *
   * @param collectionName : String
   * @param queryContent   : [[QueryContent]]
   * @return
   */
  def deleteData(collectionName: String, queryContent: QueryContent): Future[DeleteResult] = {
    val filter = getFilterFromQueryContent(queryContent)
    collection(collectionName).deleteMany(filter).toFuture()
  }

  /**
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  private def checkIfContainsPrimaryKeys(collectionName: String, validatorContent: ValidatorContent): Future[Boolean] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val vc = validatorContent.actions.map {
      case AddField(fieldName, _, _, _) => fieldName
      case DelField(fieldName) => fieldName
    }.toSet

    getCollectionIndexes(collectionName)
      .map(_ (1))
      .map(_.key.keys.toSet)
      .map(_.union(vc).nonEmpty)
  }

  /**
   * modify collection's validator, [[ValidatorContent]] cannot contain primary keys
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  def modifyValidator(collectionName: String, validatorContent: ValidatorContent): Future[Document] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val fut = getCollectionInfo(database, collectionName)
      .map(validatorUpdate(_, validatorContent))
      .map(genModifyValidatorDocument)
      .flatMap(database.runCommand(_).toFuture())

    checkIfContainsPrimaryKeys(collectionName, validatorContent)
      .map(res => {
        if (res) throw new RuntimeException("cannot modify primary keys")
      })
      .flatMap(_ => fut)
  }

}


object MongoLoader extends MongoJsonSupport {

  import Utilities.MongoTypeMapping

  case class RawValidatorMap(validator: Map[String, Map[String, Int]])


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

  private def genCreateCollectionOptions(collectionInfo: CollectionInfo) = {
    val jsonSchema = Filters.jsonSchema(genValidatorDocument(collectionInfo))
    val vo = ValidationOptions().validator(jsonSchema)
    CreateCollectionOptions().validationOptions(vo)
  }

  /**
   *
   * @param seqDocument : Seq[Document]
   * @return
   */
  private def extractMongoCollectionValidatorFromSeqDocument(seqDocument: Seq[Document]): MongoCollectionValidator =
    seqDocument.head.toList(2)._2.asDocument().toJson.parseJson.convertTo[MongoCollectionValidator]

  /**
   *
   * @param mongoCollectionValidator : [[MongoCollectionValidator]]
   * @return
   */
  private def convertMongoCollectionValidatorToFields(mongoCollectionValidator: MongoCollectionValidator): List[FieldInfo] =
    mongoCollectionValidator.validator.$jsonSchema.properties.map {
      case (k, v) =>
        FieldInfo(
          fieldName = k,
          nameAlias = v.title,
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
  private def getCollectionInfo(database: MongoDatabase,
                                collectionName: String): Future[CollectionInfo] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()
      .map(extractMongoCollectionValidatorFromSeqDocument)
      .map(convertMongoCollectionValidatorToFields)
      .map(i => CollectionInfo(collectionName = collectionName, fields = i))
  }

  /**
   *
   * @param seqDocument : Seq[Document]
   * @return
   */
  private def convertMongoIndexes(seqDocument: Seq[Document]): Seq[MongoIndex] =
    seqDocument.map(_.toJson.parseJson.convertTo[MongoIndex])

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
   * @param d : [[JsValue]]
   * @return
   */
  private def jsValueConvert(d: JsValue): Any = d match {
    case JsString(v) => v
    case JsNumber(v) => v.toDouble
    case JsTrue => true
    case JsFalse => false
    case JsNull => None
    case _ => throw new RuntimeException(s"Invalid JSON format: ${d.toString}")
  }

  /**
   *
   * @param name          : String
   * @param filterOptions : [[FilterOptions]]
   * @return
   */
  private def extractFilter(name: String, filterOptions: FilterOptions): Bson = {

    val eq = filterOptions.eq.fold(Option.empty[Bson]) { i => Some(Filters.eq(name, jsValueConvert(i))) }
    val gt = filterOptions.gt.fold(Option.empty[Bson]) { i => Some(Filters.gt(name, jsValueConvert(i))) }
    val lt = filterOptions.lt.fold(Option.empty[Bson]) { i => Some(Filters.lt(name, jsValueConvert(i))) }
    val gte = filterOptions.gte.fold(Option.empty[Bson]) { i => Some(Filters.gte(name, jsValueConvert(i))) }
    val lte = filterOptions.lte.fold(Option.empty[Bson]) { i => Some(Filters.lte(name, jsValueConvert(i))) }

    val gatheredFilters = List(eq, gt, lt, gte, lte).foldLeft(List.empty[Bson]) {
      case (acc, ob) => ob match {
        case None => acc
        case Some(v) => acc :+ v
      }
    }
    Filters.and(gatheredFilters: _*)
  }

  // todo: redo Conjunction, unmarshall and/or recursively
  /**
   *
   * @param filter : [[Conjunctions]]
   * @return
   */
  private def getFilter(filter: Conjunctions): Bson = filter match {
    case AND(and) =>
      val res = and.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
      Filters.and(res: _*)
    case OR(or) =>
      val res = or.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
      Filters.or(res: _*)
  }

}

