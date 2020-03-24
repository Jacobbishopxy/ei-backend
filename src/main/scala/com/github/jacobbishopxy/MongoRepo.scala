package com.github.jacobbishopxy

import org.mongodb.scala._

import scala.concurrent.Future

/**
 * Created by Jacob Xie on 3/23/2020
 */
trait MongoRepo {
  val conn: String
  val dbName: String

  val mongoClient: MongoClient = MongoClient(conn)
  val database: MongoDatabase = mongoClient.getDatabase(dbName)

  def collection(name: String): MongoCollection[Document] =
    database.getCollection(name)

  def listDatabases(): Future[Seq[String]] =
    mongoClient.listDatabaseNames().toFuture()

  def dropDatabase(name: String): Future[Completed] =
    mongoClient.getDatabase(name).drop().toFuture()

  def listCollections(): Future[Seq[String]] =
    database.listCollectionNames().toFuture()

  def dropCollection(name: String): Future[Completed] =
    collection(name).drop().toFuture()

}

