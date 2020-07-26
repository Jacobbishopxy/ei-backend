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

  private def anchorCond(a: Anchor, ac: Option[AnchorConfig]): Bson = {

    val al = ac.fold(List.empty[Option[Bson]])(i => List(
      symbolEqual(i.symbol),
      dateEqual(i.date)
    ))
    val l = List(
      identityEqual(a.identity),
      categoryEqual(a.category),
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
   *
   * @param dc     : [[DbCollection]]
   * @param anchor : [[Anchor]]
   * @return
   */
  def fetchStore(dc: DbCollection, anchor: Anchor, anchorConfig: Option[AnchorConfig]): Future[Store] =
    getCollection[Store](dc.db, dc.collectionName)
      .find(anchorCond(anchor, anchorConfig))
      .first()
      .toFuture()

  /**
   *
   * @param dc     : [[DbCollection]]
   * @param anchor : [[Anchor]]
   * @return
   */
  def deleteStore(dc: DbCollection, anchor: Anchor, anchorConfig: Option[AnchorConfig]): Future[DeleteResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .deleteMany(anchorCond(anchor, anchorConfig))
      .toFuture()

  /**
   *
   * @param dc    : [[DbCollection]]
   * @param store : [[Store]]
   * @return
   */
  def replaceStore(dc: DbCollection, store: Store): Future[UpdateResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .replaceOne(
        anchorCond(store.anchor, store.anchorConfig),
        store,
        ReplaceOptions().upsert(true)
      )
      .toFuture()

  /**
   *
   * @param dc     : [[DbCollection]]
   * @param stores : Seq[[Store]]
   * @return
   */
  def replaceStores(dc: DbCollection, stores: Seq[Store]): Future[BulkWriteResult] =
    getCollection[Store](dc.db, dc.collectionName)
      .bulkWrite(
        stores.map(i =>
          ReplaceOneModel(anchorCond(i.anchor, i.anchorConfig), i, ReplaceOptions().upsert(true))
        )
      )
      .toFuture()

  /**
   *
   * @param dc : [[DbCollection]]
   * @param tp : [[TemplatePanel]]
   * @return
   */
  def fetchLayout(dc: DbCollection, tp: TemplatePanel): Future[Layout] =
    getCollection[Layout](dc.db, dc.collectionName)
      .find(templatePanelCond(tp))
      .first()
      .toFuture()

  /**
   *
   * @param dc : [[DbCollection]]
   * @param tp : [[TemplatePanel]]
   * @return
   */
  def deleteLayout(dc: DbCollection, tp: TemplatePanel): Future[DeleteResult] =
    getCollection[Layout](dc.db, dc.collectionName)
      .deleteMany(templatePanelCond(tp))
      .toFuture()

  /**
   *
   * @param dc     : [[DbCollection]]
   * @param layout : [[Layout]]
   * @return
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

  def replaceLayoutWithStore(dbCollection: DbCollection, layoutWithStore: LayoutWithStore): Future[BulkWriteResult] = {
    import Implicits.global

    val tp = layoutWithStore.templatePanel
    val lo = layoutWithStore.layouts
    val st = layoutWithStore.stores

    for {
      _ <- replaceLayout(dbCollection, Layout(tp, lo))
      res <- replaceStores(dbCollection, st)
    } yield res
  }


  // api methods

  /**
   *
   * @param collection : String
   * @param anchor     : [[Anchor]]
   * @return
   */
  def fetchIndustryStore(collection: String, anchor: Anchor, anchorConfig: Option[AnchorConfig]): Future[Store] =
    fetchStore(DbCollection(DB.Industry, collection), anchor, anchorConfig)

  /**
   *
   * @param collection : String
   * @param anchor     : [[Anchor]]
   * @return
   */
  def deleteIndustryStore(collection: String, anchor: Anchor, anchorConfig: Option[AnchorConfig]): Future[DeleteResult] =
    deleteStore(DbCollection(DB.Industry, collection), anchor, anchorConfig)

  /**
   *
   * @param collection : String
   * @param store      : [[Store]]
   * @return
   */
  def replaceIndustryStore(collection: String, store: Store): Future[UpdateResult] =
    replaceStore(DbCollection(DB.Industry, collection), store)

  /**
   *
   * @param collection : String
   * @param tp         : [[TemplatePanel]]
   * @return
   */
  def fetchTemplateLayout(collection: String, tp: TemplatePanel): Future[Layout] =
    fetchLayout(DbCollection(DB.Template, collection), tp)

  /**
   *
   * @param collection : String
   * @param tp         : [[TemplatePanel]]
   * @return
   */
  def deleteTemplateLayout(collection: String, tp: TemplatePanel): Future[DeleteResult] = {
    import Implicits.global

    for {
      lo <- fetchTemplateLayout(collection, tp)
      _ <- Future.sequence(lo.layouts.map(i => deleteIndustryStore(collection, i.anchor, None)))
      res <- deleteLayout(DbCollection(DB.Template, collection), tp)
    } yield res
  }

  /**
   *
   * @param collection : String
   * @param layout     : [[Layout]]
   * @return
   */
  def replaceTemplateLayout(collection: String, layout: Layout): Future[UpdateResult] =
    replaceLayout(DbCollection(DB.Template, collection), layout)

}

