package com.github.jacobbishopxy

import MongoModel._

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Completed, Document, MongoDatabase}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.DeleteResult
import spray.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


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

    val co = genCreateCollectionOptions(collectionInfo)
    val createColl = database.createCollection(collectionInfo.collectionName, co).toFuture()
    val createIdx = createIndex(collectionInfo)

    createColl.flatMap(_ => createIdx)
  }

  /**
   * modify collection's validator, [[ValidatorContent]] cannot contain primary keys
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  def modifyValidator(collectionName: String,
                      validatorContent: ValidatorContent): Future[Document] =
    for {
      res <- checkIfContainsPrimaryKeys(collectionName, validatorContent)
      ans <- if (res) Future(Document("error" -> "Cannot modify primary keys"))
      else forceModifyCollectionValidator(collectionName, validatorContent)
    } yield ans


  /**
   * modify a collection's validator by accepting new [[CollectionInfo]]
   *
   * @param collectionInfo : [[CollectionInfo]]
   * @return
   */
  def modifyCollection(collectionInfo: CollectionInfo): Future[Document] = {
    val collectionName = collectionInfo.collectionName
    showCollection(collectionName)
      .map(_.fields)
      .map(genValidatorContent(_, collectionInfo.fields))
      .flatMap(modifyValidator(collectionName, _))
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
  def showIndex(collectionName: String): Future[Seq[MongoIndex]] =
    showCollectionIndexes(database, collectionName)

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
  def fetchData(collectionName: String,
                queryContent: QueryContent): Future[Seq[Document]] = {
    val filter = getFilterFromQueryContent(queryContent)
    val cf = collection(collectionName).find(filter)
    val res = queryContent.limit.fold(cf) { i => cf.limit(i) }
    res.toFuture()
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
  def deleteData(collectionName: String,
                 queryContent: QueryContent): Future[DeleteResult] =
    collection(collectionName).deleteMany(getFilterFromQueryContent(queryContent)).toFuture()

  /**
   *
   * @param collectionName   : String
   * @param validatorContent : [[ValidatorContent]]
   * @return
   */
  private def checkIfContainsPrimaryKeys(collectionName: String,
                                         validatorContent: ValidatorContent): Future[Boolean] = {

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
                                             validatorContent: ValidatorContent): Future[Document] =
    showCollectionInfo(database, collectionName)
      .map(validatorUpdate(_, validatorContent))
      .map(genModifyValidatorDocument)
      .flatMap(database.runCommand(_).toFuture())

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
  private def convertMongoCollectionValidatorToFields(mongoCollectionValidator: MongoCollectionValidator,
                                                      indexesMapping: Map[String, Int]): List[FieldInfo] =
    mongoCollectionValidator.validator.$jsonSchema.properties.map {
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
                                    collectionName: String): Future[Seq[MongoIndex]] =
    database.getCollection(collectionName).listIndexes().toFuture().map(convertMongoIndexes)

  /**
   *
   * @param database       : [[MongoDatabase]]
   * @param collectionName : String
   * @return
   */
  private def showCollectionInfo(database: MongoDatabase,
                                 collectionName: String): Future[CollectionInfo] = {
    val indexes = showCollectionIndexes(database, collectionName)
      .map(_ (1).key)

    database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()
      .map(extractMongoCollectionValidatorFromSeqDocument)
      .zip(indexes)
      .map { case (validator, indexes) =>
        convertMongoCollectionValidatorToFields(validator, indexes)
      }
      .map(i => CollectionInfo(collectionName, i))
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

