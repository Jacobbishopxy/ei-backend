package com.github.jacobbishopxy.eiDashboard

import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

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

  case class Anchor(identity: String, category: String, symbol: String, date: String) // maybe more key?

  case class Content(data: String, config: Option[String])
  case class Store(anchor: Anchor, content: Content)

  case class Coordinate(x: Int, y: Int, h: Int, w: Int)
  case class Element(anchor: Anchor, coordinate: Coordinate)

  case class GridLayout(template: String, panel: String, layouts: Seq[Element])
  case class GridTemplatePanel(template: String, panel: String)


  val CR: CodecRegistry = fromRegistries(fromProviders(
    classOf[Anchor],
    classOf[Content],
    classOf[Store],
    classOf[Coordinate],
    classOf[Element],
    classOf[GridLayout],
    classOf[GridTemplatePanel],
  ))

  implicit val anchorFormat: RootJsonFormat[Anchor] = jsonFormat4(Anchor)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat2(Content)
  implicit val storeFormat: RootJsonFormat[Store] = jsonFormat2(Store)
  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat4(Coordinate)
  implicit val elementFormat: RootJsonFormat[Element] = jsonFormat2(Element)
  implicit val gridLayoutFormat: RootJsonFormat[GridLayout] = jsonFormat3(GridLayout)
  implicit val gridTemplatePanelFormat: RootJsonFormat[GridTemplatePanel] = jsonFormat2(GridTemplatePanel)
}

