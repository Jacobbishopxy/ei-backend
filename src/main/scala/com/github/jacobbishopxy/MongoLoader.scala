package com.github.jacobbishopxy

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Completed, Document, MongoDatabase}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{CreateCollectionOptions, Filters, IndexModel, IndexOptions, Indexes, ValidationOptions}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 4/1/2020
 */
class MongoLoader(connectionString: String, databaseName: String) extends MongoConn {

  override val conn: String = connectionString
  override val dbName: String = databaseName

  import MongoModel._
  import MongoLoader._

  /**
   * create a collection with validator
   *
   * @param cols : [[Cols]]
   * @return
   */
  def createCollection(cols: Cols): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val co = CreateCollectionOptions().validationOptions(createValidator(cols))
    val createColl = database.createCollection(cols.name, co).toFuture()
    val createIdx = createIndex(cols)

    createColl.flatMap(_ => createIdx)
  }


  /**
   * create indexes for a collection
   *
   * @param cols : [[Cols]]
   * @return
   */
  def createIndex(cols: Cols): Future[String] = {
    val idx = cols.cols.foldLeft(List.empty[Bson]) {
      case (acc, c) =>
        c.indexOption match {
          case None => acc
          case Some(io) =>
            val ia = if (io.ascending) Indexes.ascending(c.name) else Indexes.descending(c.name)
            acc :+ ia
        }
    }
    val idxOpt = IndexOptions().background(false).unique(true)

    collection(cols.name).createIndex(Indexes.compoundIndex(idx: _*), idxOpt).toFuture()
  }

  /**
   * show indexes for a collection
   *
   * @param collectionName : String
   * @return
   */
  def getCollectionIndexes(collectionName: String): Future[Seq[String]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    collection(collectionName).listIndexes().toFuture().map(_.map(_.toString))
  }

  // todo: public method -- modify indexes

  /**
   * get all data with conditions
   *
   * @param collectionName : String
   * @param queryContent   : [[QueryContent]]
   * @return
   */
  def fetchData(collectionName: String, queryContent: QueryContent): Future[Seq[Document]] = {
    val filter = queryContent.filter match {
      case Some(v) => getFilter(v)
      case None => BsonDocument()
    }
    val cf = collection(collectionName).find(filter)
    val res = queryContent.limit.fold(cf) { i => cf.limit(i) }
    res.toFuture()
  }

  /**
   * get collection's information
   *
   * @param collectionName : String
   * @return
   */
  def getCollectionInfos(collectionName: String): Future[Seq[Document]] =
    database.listCollections().filter(Filters.eq("name", collectionName)).toFuture()


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

  def updateData(collectionName: String) = ???

  def deleteData(collectionName: String) = ???

  /**
   * modify collection's validator
   *
   * @param collectionName : String
   * @param action         : [[ValidatorActions]]
   * @return
   */
  def modifyValidator(collectionName: String, action: ValidatorActions): Future[Document] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    getValidatorMap(database, collectionName)
      .map(validatorMapUpdate(_, action))
      .map(genModifyValidatorDoc(collectionName, _))
      .flatMap(database.runCommand(_).toFuture())
  }

}


object MongoLoader {

  import MongoModel._
  import Utilities.intToBsonType

  case class RawValidatorMap(validator: Map[String, Map[String, Int]])

  /**
   *
   * @param cols : [[Cols]]
   * @return
   */
  private def createValidator(cols: Cols): ValidationOptions = {
    val bsonList = cols.cols.foldLeft(List.empty[Bson]) {
      case (l, i) => l :+ Filters.`type`(i.name, intToBsonType(i.colType))
    }
    val bson = Filters.and(bsonList: _*)
    new ValidationOptions().validator(bson)
  }

  /**
   *
   * @param database       : [[MongoDatabase]]
   * @param collectionName : String
   * @return
   */
  private def getValidatorMap(database: MongoDatabase,
                              collectionName: String): Future[Map[String, Int]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val fut = database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()

    fut
      .map(r => r.head.toList(2)._2.asDocument()) // validator is the third element in the list
      .map(r => r.asDocument().toJson.parseJson.convertTo(jsonFormat1(RawValidatorMap))) // Bson to ValidatorMap
      .map(r => r.validator.map {
        case (k, v) => k -> v.getOrElse("$type", 0)
      })
  }

  /**
   *
   * @param currentValidatorMap : Map[String, Int]
   * @param action              : [[ValidatorActions]]
   * @return
   */
  private def validatorMapUpdate(currentValidatorMap: Map[String, Int],
                                 action: ValidatorActions): Map[String, Int] =
    action match {
      case AddEle(n, t) => currentValidatorMap ++ Map(n -> t)
      case DelEle(n) => currentValidatorMap - n
    }

  /**
   *
   * @param collectionName : String
   * @param validatorMap   : Map[String, Int]
   * @return
   */
  private def genModifyValidatorDoc(collectionName: String,
                                    validatorMap: Map[String, Int]): Document = {
    val vld = validatorMap.map {
      case (k, v) => k -> Document("$type" -> v)
    }

    Document(
      "collMod" -> collectionName,
      "validator" -> Document(vld)
    )
  }

  private def jsValueConvert(d: JsValue): Any = d match {
    case JsString(v) => v
    case JsNumber(v) => v.toDouble
    case JsTrue => true
    case JsFalse => false
    case JsNull => None
    case _ => throw new RuntimeException(s"Invalid JSON format: ${d.toString}")
  }

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

  private def getFilter(filter: Conjunctions): Bson = filter match {
    case AND(and) =>
      val res = and.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
      Filters.and(res: _*)
    case OR(or) =>
      val res = or.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
      Filters.or(res: _*)
  }

}