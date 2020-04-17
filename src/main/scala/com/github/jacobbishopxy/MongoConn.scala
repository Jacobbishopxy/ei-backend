package com.github.jacobbishopxy

import org.mongodb.scala._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Jacob Xie on 3/23/2020
 */
trait MongoConn {
  val conn: String
  val dbName: String

  lazy val mongoClient: MongoClient = MongoClient(conn)
  lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)

  def collection(name: String): MongoCollection[Document] =
    database.getCollection(name)

  def listDatabases(): Future[Seq[String]] =
    mongoClient.listDatabaseNames().toFuture()

  def dropDatabase(name: String): Future[Completed] =
    mongoClient.getDatabase(name).drop().toFuture()

  def listCollections(): Future[Seq[String]] =
    database.listCollectionNames().toFuture()

  def doesCollectionExist(name: String): Future[Boolean] =
    listCollections().map(_.contains(name))

  def dropCollection(name: String): Future[Completed] =
    collection(name).drop().toFuture()

}

