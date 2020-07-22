package testCase

import com.typesafe.config.{Config, ConfigFactory}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 7/22/2020
 */
object TestMongoCR extends App {

  import com.github.jacobbishopxy.MongoEnumHelper._

  sealed abstract class Category(n: String) {
    def name: String = n
  }
  object Category {
    case object EmbedLink extends Category("embedLink")
    case object Text extends Category("text")
    case object TargetPrice extends Category("targetPrice")
  }


  object Finder {
    implicit class CategoryFinder(n: String) {
      def category: Category = n match {
        case "embedLink" => Category.EmbedLink
        case "text" => Category.Text
        case "targetPrice" => Category.TargetPrice
      }
    }
  }


  class CategoryEnumClz extends EnumClz[Category] {

    import Finder._

    override def clzToString(category: Category): String = category.name

    override def stringToClz(n: String): Category = n.category
  }

  object CategoryCodecProvider extends MongoEnumCodecProvider[Category] {
    override val enumCodec =
      new MongoEnumCodec[Category](new CategoryEnumClz, "_t")

    override def isEnum[T](clazz: Class[T]): Boolean =
      clazz.isInstance(Category.EmbedLink) ||
        clazz.isInstance(Category.Text) ||
        clazz.isInstance(Category.TargetPrice)

  }

  case class Anchor(identity: String,
                    category: Category,
                    symbol: Option[String],
                    date: Option[String])

  val cr = fromRegistries(
    fromProviders(
      CategoryCodecProvider,
      classOf[Anchor],
    ), DEFAULT_CODEC_REGISTRY)


  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo-dev.dev")
  val mongoClient: MongoClient = MongoClient(mongoUrl)
  val database: MongoDatabase = mongoClient.getDatabase("dev")

  val db = database.withCodecRegistry(cr)
  val collection: MongoCollection[Anchor] = db.getCollection("cr-test")

  // query
  val res1 = Await.result(collection.find().toFuture(), 10.seconds)
  println(res1)

  // mutate
  val md = Anchor("#2", Category.TargetPrice, Some("000001"), Some("20200101"))
  val res2 = Await.result(collection.insertOne(md).toFuture(), 10.seconds)
  println(res2)

}