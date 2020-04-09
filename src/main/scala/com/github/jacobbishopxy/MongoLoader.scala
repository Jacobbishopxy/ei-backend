package com.github.jacobbishopxy

import MongoModel._

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Completed, Document, MongoDatabase}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import spray.json._
import spray.json.DefaultJsonProtocol._

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
   * @param cols : [[Cols]]
   * @return
   */
  def createCollection(cols: Cols): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val co = genCreateCollectionOptions(cols)
    val createColl = database.createCollection(cols.collectionName, co).toFuture()
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
            val ia = if (io.ascending) Indexes.ascending(c.fieldName) else Indexes.descending(c.fieldName)
            acc :+ ia
        }
    }
    val idxOpt = IndexOptions().background(false).unique(true)

    collection(cols.collectionName).createIndex(Indexes.compoundIndex(idx: _*), idxOpt).toFuture()
  }

  // todo: collectionIndexes needs a model, instead of Seq[String] as display
  /**
   * show indexes for a collection
   *
   * @param collectionName : String
   * @return
   */
  def getCollectionIndexes(collectionName: String): Future[Seq[Document]] =
    collection(collectionName).listIndexes().toFuture()

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
  def getCollectionValidator(collectionName: String): Future[Cols] =
    getCollectionCols(database, collectionName)

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
   * modify collection's validator
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorActions]]
   * @return
   */
  def modifyValidator(collectionName: String, validatorContent: ValidatorContent): Future[Document] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    getCollectionCols(database, collectionName)
      .map(validatorUpdate(_, validatorContent))
      .map(genModifyValidatorDocument)
      .flatMap(database.runCommand(_).toFuture())
  }

}


object MongoLoader extends MongoValidatorJsonSupport {

  import Utilities.MongoTypeMapping

  case class RawValidatorMap(validator: Map[String, Map[String, Int]])


  // todo: projecting fields (limit the fields returned)

  /**
   *
   * @param cols : [[Cols]]
   * @return
   */
  private def genValidatorDocument(cols: Cols): Document = {
    val properties = cols.cols.foldLeft(List.empty[(String, Document)]) {
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
      "required" -> cols.cols.map(_.fieldName),
      "properties" -> properties
    )
  }

  private def genCreateCollectionOptions(cols: Cols) = {
    val jsonSchema = Filters.jsonSchema(genValidatorDocument(cols))
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
  private def convertMongoCollectionValidatorToCol(mongoCollectionValidator: MongoCollectionValidator): List[Col] =
    mongoCollectionValidator.validator.$jsonSchema.properties.map {
      case (k, v) =>
        Col(
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
  private def getCollectionCols(database: MongoDatabase,
                                collectionName: String): Future[Cols] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()
      .map(extractMongoCollectionValidatorFromSeqDocument)
      .map(convertMongoCollectionValidatorToCol)
      .map(i => Cols(collectionName = collectionName, cols = i))
  }

  /**
   *
   * @param currentCols      : [[Cols]]
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  private def validatorUpdate(currentCols: Cols,
                              validatorContent: ValidatorContent): Cols = {
    val colList = validatorContent.actions.foldLeft(currentCols.cols.reverse) {
      case (acc, a) => a match {
        case AddEle(fn, na, ft, d) =>
          acc :+ Col(fieldName = fn, nameAlias = na, fieldType = ft, description = d)
        case DelEle(fn) =>
          acc.filterNot(c => c.fieldName == fn)
      }
    }
    Cols(currentCols.collectionName, colList)
  }

  /**
   *
   * @param validatorCols : [[Cols]]
   * @return
   */
  private def genModifyValidatorDocument(validatorCols: Cols): Document =
    Document(
      "collMod" -> validatorCols.collectionName,
      "validator" -> Document("$jsonSchema" -> genValidatorDocument(validatorCols))
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
   * @param filter: [[Conjunctions]]
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