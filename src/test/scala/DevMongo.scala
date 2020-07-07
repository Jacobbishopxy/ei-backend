
import com.typesafe.config.{Config, ConfigFactory}
import org.bson.BsonType
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.annotations.BsonProperty
import org.mongodb.scala.model.{CreateCollectionOptions, Filters, Updates, ValidationOptions, Projections}
import org.mongodb.scala.result.DeleteResult
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 3/16/2020
 */
object DevMongo extends App with DevMongoRepo {

  /**
   * This demo shows how to insert & query typed data (using `codecRegistry`)
   * Projection for querying specified fields
   */

  // ADT
  final case class Cord(x: Int, y: Int)

  final case class InfoDB(@BsonProperty("_id") id: Int,
                          @BsonProperty("name") dbName: String,
                          @BsonProperty("type") dbType: String,
                          count: Int,
                          info: Cord)

  final case class SimpleInfoDB(@BsonProperty("name") dbName: String,
                                @BsonProperty("type") dbType: String)

  val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(
      classOf[Cord],
      classOf[InfoDB],
      classOf[SimpleInfoDB]
    ), DEFAULT_CODEC_REGISTRY)

  val db: MongoDatabase = database.withCodecRegistry(codecRegistry)
  val collection: MongoCollection[InfoDB] = db.getCollection("test")
  val collectionSimple: MongoCollection[SimpleInfoDB] = db.getCollection("test")


  val infoDB = InfoDB(3, "Postgres", "database", 2, Cord(233, 135)) // fake data

  val insertExe = Await.result(collection.insertOne(infoDB).toFuture(), 10.seconds) // execute action
  println(insertExe)

  val res = Await.result(collection.find().toFuture(), 10.seconds) // read all
  println(res)

  val que = Await.result(collection.find(Filters.eq("name", "Postgres")).toFuture(), 10.seconds)
  println(que)

  val res1 = Await.result(collectionSimple.find().projection(Projections.include("name", "type")).toFuture(), 10.seconds)
  println(res1)

}

object DevMongo1 extends App with DevMongoRepo {

  /**
   * This demo shows how to insert, query & update untyped data
   */

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    override def write(value: Any): JsValue = value match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
      case _ => throw new RuntimeException(s"AnyJsonFormat write failed: ${value.toString}")
    }

    override def read(value: JsValue): Any = value match {
      case JsNumber(n) => n.intValue
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
      case _ => throw new RuntimeException(s"AnyJsonFormat read failed: ${value.toString}")
    }
  }

  def extractJsonData(d: Seq[Document]): Seq[String] =
    d.map(_.toJson)

  val collection = database.getCollection("user")

  // insertOne: make sure untyped data is a json string
  val foo = Map("username" -> "J", "gender" -> "M", "career" -> "Engineer", "rank" -> 10).toJson.toString

  val fut = collection.insertOne(Document(foo)).toFuture()
  val insertExe = Await.result(fut, 10.seconds)
  println(insertExe)

  // insertMany: make sure untyped data is a seq of json string
  val bar = Seq(
    Map("username" -> "MZ", "gender" -> "M", "career" -> "Data Scientist"),
    Map("username" -> "Sam", "gender" -> "M", "career" -> "Researcher"),
  ).map(_.toJson.toString).map(Document(_))

  val fut1 = collection.insertMany(bar).toFuture()
  val insertExe1 = Await.result(fut1, 10.seconds)
  println(insertExe1)

  // query with filter option
  val que: Future[Seq[Document]] = collection.find(Filters.eq("username", "J")).toFuture()
  val res: Seq[Document] = Await.result(que, 10.seconds)

  println(extractJsonData(res))

  // delete
  val del1: Future[DeleteResult] = collection.deleteOne(
    Filters.and(Filters.eq("username", "J"), Filters.eq("rank", 10))
  ).toFuture()
  println(Await.result(del1, 10.seconds))

  val del2 = collection.deleteMany(Filters.eq("username", "J")).toFuture()
  println(Await.result(del2, 10.seconds))

  // update
  val upd1 = collection.updateOne(
    Filters.eq("username", "MZ"), Updates.set("rank", 2)
  ).toFuture()
  println(Await.result(upd1, 10.seconds))

}

object DevMongo2 extends App with DevMongoRepo {

  /**
   * This demo shows how to create a collection with validator:
   * 1. normal method
   * 2. using jsonSchema
   */

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

  // 1. normal method, create field restriction first
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

  // 2. use Filters.jsonSchema method to create a validator, since we also need other validator attributes
  val username1 = "username" -> Document("bsonType" -> "string", "title" -> "用户", "description" -> "用户名称！")
  val email1 = "email" -> Document("bsonType" -> "string", "title" -> "邮箱", "description" -> "邮箱账号！")

  val jsonSchema: Document = Document(
    "bsonType" -> "object",
    "required" -> Seq("username", "email"),
    "properties" -> Document(username1, email1)
  )
  val validator1: Bson = Filters.jsonSchema(jsonSchema)
  val validationOptions1 = ValidationOptions().validator(validator1)
  val createCollectionOptions1 = CreateCollectionOptions().validationOptions(validationOptions1)

  val res1 = database.createCollection("user1", createCollectionOptions1).toFuture()
  Await.result(res1, 10.seconds)

}

import com.github.jacobbishopxy.MongoModel.{MongoCollectionValidator, MongoJsonSupport}

object DevMongo3 extends App with DevMongoRepo with MongoJsonSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * This demo shows how to query a collection's validator
   */

  // get current validator
  def showCollectionInfo(colName: String): Future[Seq[Document]] =
    database.listCollections().filter(Filters.eq("name", colName)).toFuture()

  val show = showCollectionInfo("user")
    .map(r => r.head.toList(2)._2
      .asDocument().toJson.parseJson.convertTo[MongoCollectionValidator]
    )


  val res = Await.result(show, 10.seconds)
  println(res)

}

object DevMongo4 extends App with DevMongoRepo {

  /**
   * This demo shows how to modify a collection's validator
   */

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

object DevMongo5 extends App with DevMongoRepo {

  /**
   * This demo shows using a wrapped function to modify a collection's validator
   */

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


trait DevMongoRepo {

  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo-dev.dev")
  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev")

}

