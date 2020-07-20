package com.github.jacobbishopxy.eiDashboard

import com.github.jacobbishopxy.eiDashboard.ProModel.{Category, DB}
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

  object DB extends Enumeration {
    type DB = Value
    val Template, Market, Bank = Value
  }

  val dbMap: Map[String, DB.Value] = Map(
    "template" -> DB.Template,
    "market" -> DB.Market,
    "bank" -> DB.Bank
  )

  object Category extends Enumeration {
    type Category = Value
    val EmbedLink, Text, TargetPrice, Image,
    FileList, FileManager,
    EditableTable, Table,
    Lines, Histogram, Pie, Scatter, Heatmap, Box, Tree, TreeMap
    = Value
  }

  val categoryMap: Map[String, Category.Value] = Map(
    "embedLink" -> Category.EmbedLink,
    "text" -> Category.Text,
    "targetPrice" -> Category.TargetPrice,
    "image" -> Category.Image,
    "fileList" -> Category.FileList,
    "fileManager" -> Category.FileManager,
    "editableTable" -> Category.EditableTable,
    "table" -> Category.Table,
    "lines" -> Category.Lines,
    "histogram" -> Category.Histogram,
    "pie" -> Category.Pie,
    "scatter" -> Category.Scatter,
    "heatmap" -> Category.Heatmap,
    "box" -> Category.Box,
    "tree" -> Category.Tree,
    "treeMap" -> Category.TreeMap,
  )


  case class Anchor(identity: String,
                    category: Category.Value,
                    symbol: Option[String],
                    date: Option[String]) // maybe more key?

  case class Content(data: String, config: Option[String])
  case class Store(anchor: Anchor, content: Content)

  case class Coordinate(x: Int, y: Int, h: Int, w: Int)
  case class Element(anchor: Anchor, coordinate: Coordinate)

  case class Layout(template: String, panel: String, layouts: Seq[Element])

  case class TemplatePanel(template: String, panel: String)
  case class DbCollection(db: DB.Value, collectionName: String)


  val CR: CodecRegistry = fromRegistries(fromProviders(
    classOf[Anchor],
    classOf[Content],
    classOf[Store],
    classOf[Coordinate],
    classOf[Element],
    classOf[Layout],
    classOf[TemplatePanel],
  ))

}

trait ProModel extends DefaultJsonProtocol {

  import ProModel._
  import com.github.jacobbishopxy.Utilities.EnumJsonConverter


  implicit val dbConverter: EnumJsonConverter[ProModel.DB.type] = new EnumJsonConverter(DB)
  implicit val categoryConverter: EnumJsonConverter[ProModel.Category.type] = new EnumJsonConverter(Category)
  implicit val anchorFormat: RootJsonFormat[Anchor] = jsonFormat4(Anchor)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat2(Content)
  implicit val storeFormat: RootJsonFormat[Store] = jsonFormat2(Store)
  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat4(Coordinate)
  implicit val elementFormat: RootJsonFormat[Element] = jsonFormat2(Element)
  implicit val gridLayoutFormat: RootJsonFormat[Layout] = jsonFormat3(Layout)
  implicit val gridTemplatePanelFormat: RootJsonFormat[TemplatePanel] = jsonFormat2(TemplatePanel)
  implicit val gridDbCollectionFormat: RootJsonFormat[DbCollection] = jsonFormat2(DbCollection)
}

