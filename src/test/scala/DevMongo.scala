
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

  val observer = new Observer[Completed] {
    override def onNext(result: Completed): Unit = println("Inserted")
    override def onError(e: Throwable): Unit = println("Failed", e)
    override def onComplete(): Unit = println("Completed")
  }


  val infoDB = InfoDB(1, "Postgres", "database", 2, Cord(233, 135)) // fake data

  val observable: Observable[Completed] = collection.insertOne(infoDB) // insert action
  //  observable.subscribe(observer) // execute action

  val res = Await.result(collection.find().toFuture(), 10.seconds)
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

