
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

  import Repo._

  // get current validator
  def showCollectionInfo(colName: String): Future[Seq[Document]] =
    database.listCollections().filter(Filters.eq("name", colName)).toFuture()

  // before change validator
  val show1: Seq[Document] = Await.result(showCollectionInfo("user"), 10.seconds)
  println(s"original validator: $show1")


  val modifyValidatorDoc = Document(
    "collMod" -> "user",
    "validator" -> Document("username" -> Document("$type" -> 2), "gender" -> Document("$type" -> 2))
  )

  val mod = database.runCommand(modifyValidatorDoc).toFuture()
  Await.result(mod, 10.seconds)

  // after change validator
  val show2 = Await.result(showCollectionInfo("user"), 10.seconds)
  println(s"validator after changed: $show2")

  // check inserting data
  val testData = Document("username" -> "J", "gender" -> "M", "email" -> "J@example.com")
  val insertData = database.getCollection("user").insertOne(testData).toFuture()
  val res = Await.result(insertData, 10.seconds)
  println(res)

}

object DevMongo3 extends App {

  /**
   * This demo shows using a wrapped function to modify a collection's validator
   */

  import Repo._
  import com.github.jacobbishopxy.Utilities._

  import spray.json._
  import spray.json.DefaultJsonProtocol._

  case class RawValidatorMap(validator: Map[String, Map[String, Int]])

  // get current validator
  def getValidator(collectionName: String): Future[Map[String, Int]] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val fut = database
      .listCollections()
      .filter(Filters.eq("name", collectionName))
      .toFuture()

    fut
      .map(r => r.head.toList(2)._2.asDocument()) // validator is the third element in the list
      .map(r => r.asDocument().toJson.parseJson.convertTo(jsonFormat1(RawValidatorMap))) // Bson to ValidatorMap
      .map(r => r.validator.map {
        case (k, v) => k -> v.getOrElse("$type", 0)
      })
  }

  val curValidator = Await.result(getValidator("user"), 10.seconds)
  println(curValidator)

  // pure
  def genModifyValidatorDoc(collectionName: String, validatorMap: Map[String, Int]): Document = {
    val vld = validatorMap.map {
      case (k, v) => k -> Document("$type" -> v)
    }

    Document(
      "collMod" -> collectionName,
      "validator" -> Document(vld)
    )
  }

  trait ValidatorAction
  case class AddEle(name: String, $type: Int) extends ValidatorAction
  case class DelEle(name: String) extends ValidatorAction

  // pure
  def validatorMapUpdate(currentValidator: Map[String, Int], action: ValidatorAction): Map[String, Int] =
    action match {
      case AddEle(n, t) =>
        currentValidator ++ Map(n -> t)
      case DelEle(n) =>
        currentValidator - n
    }

  println(validatorMapUpdate(curValidator, AddEle("career", 2)))


  // side effect
  def modifyValidator(collectionName: String, action: ValidatorAction): Future[Document] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    getValidator(collectionName)
      .map(validatorMapUpdate(_, action))
      .map(genModifyValidatorDoc(collectionName, _))
      .flatMap(database.runCommand(_).toFuture())
  }


  val res = Await.result(modifyValidator("user", AddEle("career", 2)), 10.seconds)
  println(res)
}


object Repo {

  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")
  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev")

}

