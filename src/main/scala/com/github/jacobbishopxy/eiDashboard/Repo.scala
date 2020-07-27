package com.github.jacobbishopxy.eiDashboard

import com.github.jacobbishopxy.Utilities._
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{Projections, ReplaceOneModel, ReplaceOptions}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}

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
    s.map(equal(s"${FieldName.anchorConfig}.${FieldName.symbol}", _))
  private val dateEqual = (d: Option[String]) =>
    d.map(equal(s"${FieldName.anchorConfig}.${FieldName.date}", _))

  private def anchorCond(a: Anchor): Bson = {

    val al = a.anchorConfig.fold(List.empty[Option[Bson]])(i => List(
      symbolEqual(i.symbol),
      dateEqual(i.date)
    ))
    val l = List(
      identityEqual(a.anchorKey.identity),
      categoryEqual(a.anchorKey.category),
    ) ::: al

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


  // common methods

  /**
   * get store by anchor
   */
  def fetchStore(dc: DbCollection, anchor: Anchor): Future[Store] =
    getCollection[Store](dc.db, dc.collectionName)
      .find(anchorCond(anchor))
      .first()
      .toFuture()

  /**
   * get stores by anchors
   */
  def fetchStores(dc: DbCollection, anchors: Seq[Anchor]): Future[Seq[Store]] =
    getCollection[Store](dc.db, dc.collectionName)
      .find(or(anchors.map(anchorCond): _*))
      .toFuture()

  /**
   * delete store by anchor
   */
  def deleteStore(dc: DbCollection, anchor: Anchor): Future[DeleteResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .deleteMany(anchorCond(anchor))
      .toFuture()

  /**
   * delete stores by anchors
   */
  def deleteStores(dc: DbCollection, anchors: Seq[Anchor]): Future[DeleteResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .deleteMany(or(anchors.map(anchorCond): _*))
      .toFuture()

  /**
   * modify store by store
   */
  def replaceStore(dc: DbCollection, store: Store): Future[UpdateResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .replaceOne(
        anchorCond(Anchor(store.anchorKey, None)),
        store,
        ReplaceOptions().upsert(true)
      )
      .toFuture()

  /**
   * modify stores by stores
   */
  def replaceStores(dc: DbCollection, stores: Seq[Store]): Future[BulkWriteResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .bulkWrite(
        stores.map(i =>
          ReplaceOneModel(anchorCond(Anchor(i.anchorKey, i.anchorConfig)), i, ReplaceOptions().upsert(true))
        )
      )
      .toFuture()

  /**
   * get layout by templatePanel
   */
  def fetchLayout(dc: DbCollection, tp: TemplatePanel): Future[Layout] =
    getCollection[Layout](dc.db, dc.collectionName)
      .find(templatePanelCond(tp))
      .first()
      .toFuture()

  /**
   * delete layout by templatePanel
   */
  def deleteLayout(dc: DbCollection, tp: TemplatePanel): Future[DeleteResult] =
    getCollection[Layout](dc.db, dc.collectionName)
      .deleteMany(templatePanelCond(tp))
      .toFuture()

  /**
   * modify layout by layout
   */
  def replaceLayout(dc: DbCollection, layout: Layout): Future[UpdateResult] = {
    getCollection[Layout](dc.db, dc.collectionName)
      .replaceOne(
        templatePanelCond(layout.templatePanel),
        layout,
        ReplaceOptions().upsert(true)
      )
      .toFuture()
  }

  /**
   *
   */
  def replaceLayoutWithStore(layoutDC: DbCollection,
                             storeDC: DbCollection,
                             layoutWithStore: LayoutWithStore): Future[BulkWriteResult] = {
    import Implicits.global

    val tp = layoutWithStore.templatePanel
    val lo = layoutWithStore.layouts
    val st = layoutWithStore.stores

    for {
      _ <- replaceLayout(layoutDC, Layout(tp, lo))
      res <- replaceStores(storeDC, st)
    } yield res
  }


  // api methods

  /**
   * get industry store by anchor
   */
  def fetchIndustryStore(collection: String, anchor: Anchor): Future[Store] =
    fetchStore(DbCollection(DB.Industry, collection), anchor)

  /**
   * get industry stores by anchors
   */
  def fetchIndustryStores(collection: String, anchors: Seq[Anchor]): Future[Seq[Store]] =
    fetchStores(DbCollection(DB.Industry, collection), anchors)

  /**
   * delete industry store by anchor
   */
  def deleteIndustryStore(collection: String, anchor: Anchor): Future[DeleteResult] =
    deleteStore(DbCollection(DB.Industry, collection), anchor)

  /**
   * delete industry stores by anchors
   */
  def deleteIndustryStores(collection: String, anchors: Seq[Anchor]): Future[DeleteResult] =
    deleteStores(DbCollection(DB.Industry, collection), anchors)

  /**
   * modify industry store by store
   */
  def replaceIndustryStore(collection: String, store: Store): Future[UpdateResult] =
    replaceStore(DbCollection(DB.Industry, collection), store)

  /**
   * modify industry stores by stores
   */
  def replaceIndustryStores(collection: String, stores: Seq[Store]): Future[BulkWriteResult] =
    replaceStores(DbCollection(DB.Industry, collection), stores)

  /**
   * get template layout by templatePanel
   */
  def fetchTemplateLayout(collection: String, tp: TemplatePanel): Future[Layout] =
    fetchLayout(DbCollection(DB.Template, collection), tp)

  /**
   * delete template layout by templatePanel
   */
  def deleteTemplateLayout(collection: String, tp: TemplatePanel): Future[DeleteResult] = {
    import Implicits.global

    for {
      lo <- fetchTemplateLayout(collection, tp)
      _ <- Future.sequence(lo.layouts.map(i => deleteIndustryStore(collection, Anchor(i.anchorKey, None))))
      res <- deleteLayout(DbCollection(DB.Template, collection), tp)
    } yield res
  }

  /**
   * modify template layout by layout
   */
  def replaceTemplateLayout(collection: String, layout: Layout): Future[UpdateResult] =
    replaceLayout(DbCollection(DB.Template, collection), layout)

  /**
   *
   */
  def replaceTemplateLayoutWithIndustryStore(collection: String,
                                             layoutWithStore: LayoutWithStore): Future[BulkWriteResult] =
    replaceLayoutWithStore(
      DbCollection(DB.Template, collection),
      DbCollection(DB.Industry, collection),
      layoutWithStore
    )


}

