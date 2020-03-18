
import com.typesafe.config.ConfigFactory
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.annotations.BsonProperty
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 3/16/2020
 */
object DevMongo extends App {

  import Repo._

  val config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl = config.getString("mongo.url")

  val mongoClient = MongoClient(mongoUrl)
  val database = mongoClient.getDatabase("dev").withCodecRegistry(codecRegistry)
  val collection: MongoCollection[InfoDB] = database.getCollection("test")


  val infoDB = InfoDB(1, "Postgres", "database", 2, Cord(233, 135)) // fake data

  val insertExe = Await.result(collection.insertOne(infoDB).toFuture(), 10.seconds) // execute action
  println(insertExe)

  val res = Await.result(collection.find().toFuture(), 10.seconds) // read all
  println(res)

}


object Repo {

  final case class Cord(x: Int, y: Int)

  final case class InfoDB(@BsonProperty("_id") id: Int,
                          @BsonProperty("name") dbName: String,
                          @BsonProperty("type") dbType: String,
                          count: Int,
                          info: Cord)

  val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(classOf[Cord], classOf[InfoDB]), DEFAULT_CODEC_REGISTRY)
}

