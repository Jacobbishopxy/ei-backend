package com.github.jacobbishopxy.eiDashboard

import com.github.jacobbishopxy.Utilities._
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{Projections, ReplaceOneModel, ReplaceOptions}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import cats.implicits._
import spray.json._

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

  import com.github.jacobbishopxy.CatsEnriched._
  import com.github.jacobbishopxy.MongoResultParser._

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
    Some(equal(s"${FieldName.anchorKey}.${FieldName.identity}", i))
  private val categoryEqual = (c: Category) =>
    Some(equal(s"${FieldName.anchorKey}.${FieldName.category}.${EnumIdentifierName.category}", c.name))
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
  def fetchStore(dc: DbCollection, anchor: Anchor): Future[Option[Store]] = {
    import Implicits.global

    getCollection[Store](dc.db, dc.collectionName)
      .find(anchorCond(anchor))
      .toFuture()
      .map(_.headOption)
  }

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
        anchorCond(Anchor(store.anchorKey, store.anchorConfig)),
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
  def fetchLayout(dc: DbCollection, tp: TemplatePanel): Future[Option[Layout]] = {
    import Implicits.global

    getCollection[Layout](dc.db, dc.collectionName)
      .find(templatePanelCond(tp))
      .toFuture()
      .map(_.headOption)
  }

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
   * modify layout and store at once
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

  def replaceLayoutWithStorePro(layoutDC: DbCollection,
                                storeDC: DbCollection,
                                layoutWithStore: LayoutWithStore): Future[List[ValidatedNelType[String]]] = {
    import Implicits.global

    val tp = layoutWithStore.templatePanel
    val lo = layoutWithStore.layouts
    val st = layoutWithStore.stores

    List(
      replaceLayout(layoutDC, Layout(tp, lo)).map(_.toString),
      replaceStores(storeDC, st).map(_.toString)
    ).traverse(_.toValidatedNel)

  }


  // api methods

  /**
   * get industry store by anchor
   */
  def fetchIndustryStore(collection: String, anchor: Anchor): Future[JsValue] = {
    import Implicits.global
    fetchStore(DbCollection(DB.Industry, collection), anchor).map(_.toJson)
  }

  /**
   * get industry stores by anchors
   */
  def fetchIndustryStores(collection: String, anchors: Seq[Anchor]): Future[JsValue] = {
    import Implicits.global
    fetchStores(DbCollection(DB.Industry, collection), anchors).map(_.toJson)
  }

  /**
   * delete industry store by anchor
   */
  def deleteIndustryStore(collection: String, anchor: Anchor): Future[JsValue] = {
    import Implicits.global
    deleteStore(DbCollection(DB.Industry, collection), anchor).map(_.toJson)
  }

  /**
   * delete industry stores by anchors
   */
  def deleteIndustryStores(collection: String, anchors: Seq[Anchor]): Future[JsValue] = {
    import Implicits.global
    deleteStores(DbCollection(DB.Industry, collection), anchors).map(_.toJson)
  }

  /**
   * modify industry store by store
   */
  def replaceIndustryStore(collection: String, store: Store): Future[JsValue] = {
    import Implicits.global
    replaceStore(DbCollection(DB.Industry, collection), store).map(_.toJson)
  }

  /**
   * modify industry stores by stores
   */
  def replaceIndustryStores(collection: String, stores: Seq[Store]): Future[JsValue] = {
    import Implicits.global
    replaceStores(DbCollection(DB.Industry, collection), stores).map(_.toJson)
  }

  /**
   * get template layout by templatePanel
   */
  def fetchTemplateLayout(collection: String, tp: TemplatePanel): Future[JsValue] = {
    import Implicits.global
    fetchLayout(DbCollection(DB.Template, collection), tp).map(_.toJson)
  }

  /**
   * delete template layout by templatePanel
   */
  def deleteTemplateLayout(collection: String, tp: TemplatePanel): Future[JsValue] = {
    import Implicits.global

    for {
      lo <- fetchLayout(DbCollection(DB.Template, collection), tp)
      st <- {
        lo match {
          case None => Future.successful(None)
          case Some(v) =>
            deleteStores(
              DbCollection(DB.Industry, collection),
              v.layouts.map(i => Anchor(i.anchorKey, None))
            ).map(Some(_))
        }
      }
      res <- {
        deleteLayout(DbCollection(DB.Template, collection), tp).map(_.toJson)
      }

    } yield List(st.toJson, res).toJson
  }

  /**
   * modify template layout by layout
   */
  def replaceTemplateLayout(collection: String, layout: Layout): Future[JsValue] = {
    import Implicits.global
    replaceLayout(DbCollection(DB.Template, collection), layout).map(_.toJson)
  }

  /**
   * modify template layout and industry store at once
   */
  def replaceTemplateLayoutWithIndustryStore(collection: String,
                                             layoutWithStore: LayoutWithStore): Future[JsValue] = {
    import Implicits.global

    val tp = layoutWithStore.templatePanel
    val lo = layoutWithStore.layouts
    val st = layoutWithStore.stores

    val f = List(
      replaceLayout(DbCollection(DB.Template, collection), Layout(tp, lo)).map(_.toJson),
      replaceStores(DbCollection(DB.Industry, collection), st).map(_.toJson)
    )

    f.traverse(_.toValidatedNel).map(_.map(_.toJson).toJson)

  }


}

