package com.github.jacobbishopxy.gridLayout

import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.Filters._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 3/17/2020
 */
object GridLayoutRepo {

  case class Coordinate(i: Int, x: Int, y: Int, h: Int, w: Int)
  case class Content(title: String,
                     @BsonProperty("type") contentType: String,
                     hyperLink: String)
  case class GridModel(coordinate: Coordinate, content: Content)

  case class GridLayout(@BsonProperty("_id") id: String, layouts: Seq[GridModel])

  val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(
      classOf[Coordinate],
      classOf[Content],
      classOf[GridModel],
      classOf[GridLayout]
    ), DEFAULT_CODEC_REGISTRY)


  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat5(Coordinate)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat3(Content)
  implicit val gridModelFormat: RootJsonFormat[GridModel] = jsonFormat2(GridModel)
  implicit val gridLayoutFormat: RootJsonFormat[GridLayout] = jsonFormat2(GridLayout)


  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")

  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev").withCodecRegistry(codecRegistry)
  val collection: MongoCollection[GridLayout] = database.getCollection("HomeEiGridLayout")

  def fetchAll(): Future[Seq[GridLayout]] = collection.find().toFuture()

  def fetchItem(id: String): Future[GridLayout] =
    collection.find(equal("_id", id)).first().toFuture()

}
