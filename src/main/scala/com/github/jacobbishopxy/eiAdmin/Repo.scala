package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoRepo

import org.mongodb.scala._
import org.mongodb.scala.model._
import org.bson.conversions.Bson
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 3/23/2020
 */
class Repo(val conn: String, val dbName: String) extends MongoRepo {

  import Model._
  import Repo._

  /**
   * create a collection with validator
   *
   * @param cols : [[Cols]]
   * @return
   */
  def createCollection(cols: Cols): Future[Seq[String]] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val co = CreateCollectionOptions().validationOptions(createValidator(cols))
    val createColl = database.createCollection(cols.name, co).toFuture()

    val idx = cols.cols.map(i => IndexModel(Indexes.ascending(i.name)))
    val createIdx = collection(cols.name).createIndexes(idx).toFuture()

    createColl.flatMap(_ => createIdx)
  }

  /**
   * get all data with limit option
   *
   * @param collectionName : String
   * @param limit          : Option[Int]
   * @return
   */
  def fetchData(collectionName: String,
                limit: Option[Int] = None): Future[Seq[Document]] = {
    val cf = collection(collectionName).find()
    val res = limit match {
      case None => cf
      case Some(l) => cf.limit(l)
    }
    res.toFuture()
  }

  /**
   * get data filter by name equal
   *
   * @param collectionName : String
   * @param namesEqual     : List[(String, Any)]
   * @return
   */
  def fetchDataByName(collectionName: String,
                      namesEqual: List[(String, Any)]): Future[Seq[Document]] = {
    val cf = collection(collectionName)
      .find(Filters.or(namesEqual.map {
        case (k, v) => Filters.equal(k, v)
      }: _*))
    cf.toFuture()
  }

  /**
   * get data filter by name range
   *
   * @param collectionName : String
   * @param gte            : (String, Any)
   * @param lte            : (String, Any)
   * @return
   */
  def fetchDataByRange(collectionName: String,
                       gte: (String, Any),
                       lte: (String, Any)): Future[Seq[Document]] = {
    val cf = collection(collectionName)
      .find(Filters.and(Filters.gte(gte._1, gte._2), Filters.lte(lte._1, lte._2)))
    cf.toFuture()
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

object Repo {

  import Model._
  import com.github.jacobbishopxy.Utilities.intToBsonType

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


}
