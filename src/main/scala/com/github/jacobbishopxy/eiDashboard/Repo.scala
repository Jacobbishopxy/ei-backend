package com.github.jacobbishopxy.eiDashboard

import com.github.jacobbishopxy.Utilities._
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson
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
  import Namespace._

  import DbFinder._

  private val config: Map[String, String] = getMongoConfig(ConfigName.config)
  private val mongoDBs: Map[DB, MongoDatabase] = config.map {
    case (k, v) => k.db -> MongoClient(v).getDatabase(k)
  }

  def getCollection[T: ClassTag](db: DB, collectionName: String): MongoCollection[T] =
    mongoDBs
      .getOrElse(db, throw new RuntimeException(s"database $db not found!"))
      .getCollection[T](collectionName)
      .withCodecRegistry(CR)


  private val identityEqual = (i: String) =>
    Some(equal(s"${FieldName.anchor}.${FieldName.identity}", i))
  private val categoryEqual = (c: Category) =>
    Some(equal(s"${FieldName.anchor}.${FieldName.category}.${EnumIdentifierName.category}", c.name))
  private val symbolEqual = (s: Option[String]) =>
    s.map(equal(s"${FieldName.anchor}.${FieldName.symbol}", _))
  private val dateEqual = (d: Option[String]) =>
    d.map(equal(s"${FieldName.anchor}.${FieldName.date}", _))

  private def anchorCond(a: Anchor): Bson = {
    val l = List(
      identityEqual(a.identity),
      categoryEqual(a.category),
      symbolEqual(a.symbol),
      dateEqual(a.date)
    )

    val cds = l.foldLeft(List.empty[Bson]) {
      case (s, None) => s
      case (s, Some(v)) => s :+ v
    }

    and(cds: _*)
  }

  private val templateEqual = (t: String) =>
    equal(s"${FieldName.templatePanel}.${FieldName.template}", t)
  private val panelEqual = (p: String) =>
    equal(s"${FieldName.templatePanel}.${FieldName.panel}", p)

  private def templatePanelCond(tp: TemplatePanel) =
    and(templateEqual(tp.template), panelEqual(tp.panel))


  /**
   *
   * @param dc     : [[DbCollection]]
   * @param anchor : [[Anchor]]
   * @return
   */
  def fetchStore(dc: DbCollection, anchor: Anchor): Future[Store] = {
    val cond = anchorCond(anchor)
    getCollection[Store](dc.db, dc.collectionName)
      .find(cond)
      .first()
      .toFuture()
  }

  /**
   *
   * @param dc : [[DbCollection]]
   * @param tp : [[TemplatePanel]]
   * @return
   */
  def fetchLayout(dc: DbCollection, tp: TemplatePanel): Future[Layout] = {
    val cond = templatePanelCond(tp)
    getCollection[Layout](dc.db, dc.collectionName)
      .find(cond)
      .first()
      .toFuture()
  }

  /**
   *
   * @param dc    : [[DbCollection]]
   * @param store : [[Store]]
   * @return
   */
  def upsertStore(dc: DbCollection, store: Store): Future[UpdateResult] = {
    val cond = anchorCond(store.anchor)
    getCollection[Store](dc.db, dc.collectionName)
      .replaceOne(cond, store, ReplaceOptions().upsert(true))
      .toFuture()
  }

  /**
   *
   * @param dc     : [[DbCollection]]
   * @param layout : [[Layout]]
   * @return
   */
  def upsertLayout(dc: DbCollection, layout: Layout) = {
    val cond = templatePanelCond(layout.templatePanel)
    getCollection[Layout](dc.db, dc.collectionName)
      .replaceOne(cond, layout, ReplaceOptions().upsert(true))
      .toFuture()
  }


}

