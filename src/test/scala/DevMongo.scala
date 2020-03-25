
import com.typesafe.config.{Config, ConfigFactory}
import org.bson.BsonType
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.{Filters, ValidationOptions}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.mongodb.scala.model.CreateCollectionOptions

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 3/16/2020
 */
object DevMongo extends App {

  /**
   * This demo shows how to insert & query typed data (using `codecRegistry`)
   */

  import Repo._

  // ADT
  final case class Cord(x: Int, y: Int)
  final case class InfoDB(@BsonProperty("_id") id: Int,
                          @BsonProperty("name") dbName: String,
                          @BsonProperty("type") dbType: String,
                          count: Int,
                          info: Cord)

  val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(classOf[Cord], classOf[InfoDB]), DEFAULT_CODEC_REGISTRY)

  val db: MongoDatabase = database.withCodecRegistry(codecRegistry)
  val collection: MongoCollection[InfoDB] = db.getCollection("test")



  val infoDB = InfoDB(1, "Postgres", "database", 2, Cord(233, 135)) // fake data

  val insertExe = Await.result(collection.insertOne(infoDB).toFuture(), 10.seconds) // execute action
  println(insertExe)

  val res = Await.result(collection.find().toFuture(), 10.seconds) // read all
  println(res)

}

object DevMongo1 extends App {

  /**
   * This demo shows how to create a collection with validator
   */

  import Repo._

  // mongo create collection with validation options (written in Mongo shell)
  /*
  {
      "validator": { "$and":
         [
            { "username": { "$type": "string" } },
            { "email": { "$regex": "@*.*$" } },
            { "password": { "$type": "string" } }
         ]
      }
   }
   */

  // collection data types' restriction
  val username: Bson = Filters.`type`("username", BsonType.STRING)
  val email: Bson = Filters.regex("email", "@*.*$")
  val password: Bson = Filters.`type`("password", BsonType.STRING)

  val validator: Bson = Filters.and(username, email, password)
  val validationOptions: ValidationOptions = new ValidationOptions().validator(validator)

  def createUserCollection(colName: String, db: MongoDatabase): Future[Completed] = {
    val co = CreateCollectionOptions().validationOptions(validationOptions)
    db.createCollection(colName, co).toFuture()
  }

  val res = createUserCollection("user", database)
  Await.result(res, 10.seconds)

}

object DevMongo2 extends App {

  /**
   * This demo shows how to modify a collection's validator
   */

  // todo: add/drop single "column" validation

  import Repo._


  def showCollectionInfo(colName: String): Future[Seq[Document]] =
    database.listCollections().filter(Filters.eq("name", colName)).toFuture()

  val showCI = Await.result(showCollectionInfo("user"), 10.seconds)
  println(showCI.head.toList(2))


  val modifyValidatorDoc = Document(
    "collMod" -> "user",
    "validator" -> Document("gender" -> Document("$type" -> 2))
  )

  val res = database.runCommand(modifyValidatorDoc).toFuture()
  Await.result(res, 10.seconds)

}


object Repo {

  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")
  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev")

}

