package com.github.jacobbishopxy.eiDashboard

import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * Created by Jacob Xie on 7/6/2020
 */
object Model {

  case class Coordinate(i: String, x: Int, y: Int, h: Int, w: Int)

  case class Content(title: String,
                     contentType: String,
                     contentData: String,
                     contentConfig: Option[String])

  case class GridModel(coordinate: Coordinate, content: Content)

  case class GridLayout(template: String, panel: String, layouts: Seq[GridModel])

  case class GridTemplatePanel(template: String, panel: String)

  val CRGridLayout: CodecRegistry =
    fromRegistries(fromProviders(
      classOf[Coordinate],
      classOf[Content],
      classOf[GridModel],
      classOf[GridLayout],
      classOf[GridTemplatePanel],
    ), DEFAULT_CODEC_REGISTRY)


  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat5(Coordinate)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat4(Content)
  implicit val gridModelFormat: RootJsonFormat[GridModel] = jsonFormat2(GridModel)
  implicit val gridLayoutFormat: RootJsonFormat[GridLayout] = jsonFormat3(GridLayout)
  implicit val gridTPFormat: RootJsonFormat[GridTemplatePanel] = jsonFormat2(GridTemplatePanel)

}

object ProModel {

  /*
  anchor
      |___store
      |___element
   */

  import Namespace._

  //  sealed trait DB
  //  object DB {
  //    case object Template extends DB
  //    case object Market extends DB
  //    case object Bank extends DB
  //  }
  //
  //  val dbMap: Map[String, DB] = Map(
  //    DbName.template -> DB.Template,
  //    DbName.market -> DB.Market,
  //    DbName.bank -> DB.Bank,
  //  )
  //  val dbReversedMap: Map[DB, String] =
  //    dbMap.map(_.swap)
  //
  //  sealed trait Category
  //  object Category {
  //    case object EmbedLink extends Category
  //    case object Text extends Category
  //    case object TargetPrice extends Category
  //    case object Image extends Category
  //    case object FileList extends Category
  //    case object FileManager extends Category
  //    case object EditableTable extends Category
  //    case object Table extends Category
  //    case object Lines extends Category
  //    case object Histogram extends Category
  //    case object Pie extends Category
  //    case object Scatter extends Category
  //    case object Heatmap extends Category
  //    case object Box extends Category
  //    case object Tree extends Category
  //    case object TreeMap extends Category
  //  }
  //
  //  val categoryMap: Map[String, Category] = Map(
  //    CategoryName.embedLink -> Category.EmbedLink,
  //    CategoryName.text -> Category.Text,
  //    CategoryName.targetPrice -> Category.TargetPrice,
  //    CategoryName.image -> Category.Image,
  //    CategoryName.fileList -> Category.FileList,
  //    CategoryName.fileManager -> Category.FileManager,
  //    CategoryName.editableTable -> Category.EditableTable,
  //    CategoryName.table -> Category.Table,
  //    CategoryName.lines -> Category.Lines,
  //    CategoryName.histogram -> Category.Histogram,
  //    CategoryName.pie -> Category.Pie,
  //    CategoryName.scatter -> Category.Scatter,
  //    CategoryName.heatmap -> Category.Heatmap,
  //    CategoryName.box -> Category.Box,
  //    CategoryName.tree -> Category.Tree,
  //    CategoryName.treeMap -> Category.TreeMap,
  //  )
  //  val categoryReversedMap: Map[Category, String] =
  //    categoryMap.map(_.swap)


  case class Anchor(identity: String,
                    category: String,
                    symbol: Option[String],
                    date: Option[String]) // maybe more key?

  case class Content(data: String, config: Option[String])
  case class Store(anchor: Anchor, content: Content)

  case class Coordinate(x: Int, y: Int, h: Int, w: Int)
  case class Element(anchor: Anchor, coordinate: Coordinate)

  case class TemplatePanel(template: String, panel: String)
  case class Layout(templatePanel: TemplatePanel, layouts: Seq[Element])

  case class DbCollection(db: String, collectionName: String)


  val CR: CodecRegistry = fromRegistries(fromProviders(
    classOf[Anchor],
    classOf[Content],
    classOf[Store],
    classOf[Coordinate],
    classOf[Element],
    classOf[TemplatePanel],
    classOf[Layout],
    classOf[DbCollection],
  ), DEFAULT_CODEC_REGISTRY)

}

trait ProModel extends DefaultJsonProtocol {

  import ProModel._

  implicit val anchorFormat: RootJsonFormat[Anchor] = jsonFormat4(Anchor)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat2(Content)
  implicit val storeFormat: RootJsonFormat[Store] = jsonFormat2(Store)
  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat4(Coordinate)
  implicit val elementFormat: RootJsonFormat[Element] = jsonFormat2(Element)
  implicit val gridLayoutFormat: RootJsonFormat[Layout] = jsonFormat2(Layout)
  implicit val gridTemplatePanelFormat: RootJsonFormat[TemplatePanel] = jsonFormat2(TemplatePanel)
  implicit val gridDbCollectionFormat: RootJsonFormat[DbCollection] = jsonFormat2(DbCollection)
}

