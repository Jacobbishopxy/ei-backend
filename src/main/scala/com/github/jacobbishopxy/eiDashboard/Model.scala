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
