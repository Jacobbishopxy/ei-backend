package com.github.jacobbishopxy.eiGridLayout

import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.ReplaceOptions
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.result.UpdateResult
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits

/**
 * Created by Jacob Xie on 3/17/2020
 */
object Repo {

  case class Coordinate(i: String, x: Int, y: Int, h: Int, w: Int)

  // todo: support json type, this whole Repo should change to untyped Repo
  case class Content(title: String,
                     contentType: String,
                     contentData: String,
                     contentConfig: Option[String])

  case class GridModel(coordinate: Coordinate, content: Content)

  case class GridLayout(panel: String, layouts: Seq[GridModel])

  val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(
      classOf[Coordinate],
      classOf[Content],
      classOf[GridModel],
      classOf[GridLayout]
    ), DEFAULT_CODEC_REGISTRY)


  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat5(Coordinate)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat4(Content)
  implicit val gridModelFormat: RootJsonFormat[GridModel] = jsonFormat2(GridModel)
  implicit val gridLayoutFormat: RootJsonFormat[GridLayout] = jsonFormat2(GridLayout)


  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")

  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev").withCodecRegistry(codecRegistry)
  val collection: MongoCollection[GridLayout] = database.getCollection("HomeEiGridLayout")

  def fetchAll(): Future[Seq[GridLayout]] = collection.find().toFuture()

  def fetchItem(panel: String): Future[GridLayout] =
    collection.find(equal("panel", panel)).first().toFuture()

  def updateItem(gl: GridLayout): Future[UpdateResult] =
    collection.replaceOne(equal("panel", gl.panel), gl).toFuture()

  def upsertItem(gl: GridLayout): Future[UpdateResult] =
    collection.replaceOne(equal("panel", gl.panel), gl, ReplaceOptions().upsert(true)).toFuture()

  def getAllPanelsName: Future[Seq[String]] = {
    import Implicits.global

    collection.find().toFuture().map(_.map(_.panel))
  }
}
