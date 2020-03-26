package com.github.jacobbishopxy.eiAdmin

import com.github.jacobbishopxy.MongoRepo
import com.github.jacobbishopxy.eiAdmin.Model.Cols

import org.mongodb.scala.Document
import org.mongodb.scala.model._
import org.bson.conversions.Bson

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 3/23/2020
 */
class Repo(val conn: String, val dbName: String) extends MongoRepo {

  import Repo._

  def createTable(cols: Cols): Future[Seq[String]] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val co = CreateCollectionOptions().validationOptions(createValidator(cols))
    val createColl = database.createCollection(cols.name, co).toFuture()

    val idx = cols.cols.map(i => IndexModel(Indexes.ascending(i.name)))
    val createIdx = collection(cols.name).createIndexes(idx).toFuture()

    createColl.flatMap(_ => createIdx)
  }

  def fetchData(collectionName: String,
                limit: Option[Int] = None): Future[Seq[Document]] = {
    val cf = collection(collectionName).find()
    val res = limit match {
      case None => cf
      case Some(l) => cf.limit(l)
    }
    res.toFuture()
  }

  def fetchDataByKeys(collectionName: String,
                      keysEql: List[(String, Any)]): Future[Seq[Document]] = {
    val cf = collection(collectionName)
      .find(Filters.or(keysEql.map {
        case (k, v) => Filters.equal(k, v)
      }: _*))
    cf.toFuture()
  }

  def fetchDataByKeyRange(collectionName: String,
                          gte: (String, Any),
                          lte: (String, Any)): Future[Seq[Document]] = {
    val cf = collection(collectionName)
      .find(Filters.and(Filters.gte(gte._1, gte._2), Filters.lte(lte._1, lte._2)))
    cf.toFuture()
  }

  def getCollectionInfos(collectionName: String): Future[Seq[Document]] =
    database.listCollections().filter(Filters.eq("name", collectionName)).toFuture()


  def insertData() = ???

  def updateData() = ???

  def updateDataColumns() = ???

}

object Repo {

  import com.github.jacobbishopxy.Utilities.intToBsonType

  private def createValidator(cols: Cols): ValidationOptions = {
    val bsonList = cols.cols.foldLeft(List.empty[Bson]) {
      case (l, i) => l :+ Filters.`type`(i.name, intToBsonType(i.colType))
    }
    val bson = Filters.and(bsonList: _*)
    new ValidationOptions().validator(bson)
  }

}
