package com.github.jacobbishopxy.eiDashboard

import com.github.jacobbishopxy.Utilities._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{Projections, ReplaceOptions}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits
import scala.reflect.ClassTag


/**
 * Created by Jacob Xie on 3/17/2020
 */
object Repo {

  import Model._

  private val config: Map[String, String] = getMongoConfig("research")
  private val mongoDBs: Map[String, MongoDatabase] = config.map {
    case (k, v) => k -> MongoClient(v).getDatabase(k)
  }

  def getCollection[T: ClassTag](db: String,
                                 collectionName: String): MongoCollection[T] =
    mongoDBs
      .getOrElse(db, throw new RuntimeException(s"database $db not found!"))
      .getCollection[T](collectionName)
      .withCodecRegistry(CRGridLayout)

  def fetchAll(db: String, collectionName: String): Future[Seq[GridLayout]] =
    getCollection[GridLayout](db, collectionName)
      .find()
      .toFuture()

  def fetchAllByTemplate(db: String, collectionName: String, template: String): Future[Seq[GridLayout]] =
    getCollection[GridLayout](db, collectionName)
      .find(equal("template", template))
      .toFuture()

  private val projectionSymbolPanel = Projections.include("template", "panel")

  def fetchAllPanelByTemplate(db: String, collectionName: String, symbol: String): Future[Seq[GridTemplatePanel]] =
    getCollection[GridTemplatePanel](db, collectionName)
      .find(equal("template", symbol))
      .projection(projectionSymbolPanel)
      .toFuture()

  def fetchItem(db: String, collectionName: String, template: String, panel: String): Future[GridLayout] = {
    val cond = and(equal("template", template), equal("panel", panel))
    getCollection[GridLayout](db, collectionName)
      .find(cond)
      .first()
      .toFuture()
  }

  def updateItem(db: String, collectionName: String, gl: GridLayout): Future[UpdateResult] = {
    val cond = and(equal("template", gl.template), equal("panel", gl.panel))
    getCollection[GridLayout](db, collectionName)
      .replaceOne(cond, gl)
      .toFuture()
  }

  def upsertItem(db: String, collectionName: String, gl: GridLayout): Future[UpdateResult] = {
    val cond = and(equal("template", gl.template), equal("panel", gl.panel))
    getCollection[GridLayout](db, collectionName)
      .replaceOne(cond, gl, ReplaceOptions().upsert(true))
      .toFuture()
  }

  def fetchAllTemplates(db: String, collectionName: String): Future[List[String]] = {
    import Implicits.global

    val raw = getCollection[GridTemplatePanel](db, collectionName).find().toFuture()
    raw.map(_.map(_.template).toList.distinct)
  }
}


object ProRepo extends ProModel {

  import ProModel._

  private val config: Map[String, String] = getMongoConfig("research")
  private val mongoDBs: Map[DB.Value, MongoDatabase] = config.map {
    case (k, v) =>
      dbMap.getOrElse(k, throw new RuntimeException(s"database $k not found!")) ->
        MongoClient(v).getDatabase(k)
  }

  def getCollection[T: ClassTag](db: DB.Value,
                                 collectionName: String): MongoCollection[T] =
    mongoDBs
      .getOrElse(db, throw new RuntimeException(s"database $db not found!"))
      .getCollection[T](collectionName)
      .withCodecRegistry(CR)

  def getTemplate(collectionName: String): MongoCollection[Layout] =
    getCollection[Layout](DB.Template, collectionName)

  def fetchStore(dc: DbCollection, anchor: Anchor) = ???

  def fetchGridLayout(dc: DbCollection, tp: TemplatePanel) = ???

  def upsertStore(dc: DbCollection, store: Store) = ???

  def upsertGridLayout(dc: DbCollection, l: Layout) = ???


}

