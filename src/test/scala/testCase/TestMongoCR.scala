package testCase

import com.typesafe.config.{Config, ConfigFactory}
import org.bson.{BsonInvalidOperationException, BsonReader, BsonWriter}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 7/22/2020
 */
object TestMongoCR extends App {

  sealed abstract class Category(n: String) {
    def name: String = n
  }
  object Category {
    case object EmbedLink extends Category("embedLink")
    case object Text extends Category("text")
    case object TargetPrice extends Category("targetPrice")
  }


  implicit class CategoryFinder(n: String) {
    def category: Category = n match {
      case "embedLink" => Category.EmbedLink
      case "text" => Category.Text
      case "targetPrice" => Category.TargetPrice
    }
  }


  //  println(Category.EmbedLink)
  //  println("embedLink".category)
  //  println("embedLink".category == Category.EmbedLink)

  object CaseObjectEnumCodecProvider extends CodecProvider {
    def isCaseObjectEnum[T](clazz: Class[T]): Boolean = {
      clazz.isInstance(Category.EmbedLink) ||
        clazz.isInstance(Category.Text) ||
        clazz.isInstance(Category.TargetPrice)
    }

    override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
      if (isCaseObjectEnum(clazz)) {
        CaseObjectEnumCodec.asInstanceOf[Codec[T]]
      } else {
        null
      }
    }

    object CaseObjectEnumCodec extends Codec[Category] {
      val identifier = "_t"

      override def decode(reader: BsonReader, decoderContext: DecoderContext): Category = {
        reader.readStartDocument()
        val enumName = reader.readString(identifier)
        reader.readEndDocument()
        enumName match {
          case s: String => s.category
          case _ => throw new BsonInvalidOperationException(s"$enumName is an invalid value for a CaseObjectEnum object")
        }
      }

      override def encode(writer: BsonWriter, value: Category, encoderContext: EncoderContext): Unit = {
        val name = value.name
        writer.writeStartDocument()
        writer.writeString(identifier, name)
        writer.writeEndDocument()
      }

      override def getEncoderClass: Class[Category] = Category.getClass.asInstanceOf[Class[Category]]
    }
  }

  case class Anchor(identity: String,
                    category: Category,
                    symbol: Option[String],
                    date: Option[String])

  val cr = fromRegistries(
    fromProviders(
      CaseObjectEnumCodecProvider,
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
  val md = Anchor("#1", Category.Text, Some("000001"), None)
  val res2 = Await.result(collection.insertOne(md).toFuture(), 10.seconds)
  println(res2)

}

object MongoCRHelper {
  trait EnumClz[T] {
    def clzString(clz: T): String

    def stringToClz(n: String): T
  }

  class CaseObjectEnumCodec[T](clz: EnumClz[T], identifier: String) extends Codec[T] {
    override def decode(reader: BsonReader, decoderContext: DecoderContext): T = {
      reader.readStartDocument()
      val enumName = reader.readString(identifier)
      reader.readEndDocument()

      enumName match {
        case s: String => clz.stringToClz(s)
        case _ => throw new BsonInvalidOperationException(s"$enumName is an invalid value for a CaseObjectEnum object")
      }
    }

    override def encode(writer: BsonWriter, value: T, encoderContext: EncoderContext): Unit = {
      val name = clz.clzString(value)
      writer.writeStartDocument()
      writer.writeString(identifier, name)
      writer.writeEndDocument()
    }

    override def getEncoderClass: Class[T] = clz.getClass.asInstanceOf[Class[T]]
  }

  trait EnumCodecProvider[E] extends CodecProvider {

    def isCaseObjectEnum[T](clazz: Class[T]): Boolean

    val enumCodec: CaseObjectEnumCodec[E]

    override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
      if (isCaseObjectEnum(clazz)) enumCodec.asInstanceOf[Codec[T]]
      else null
    }
  }

}

object TestMongoCRPro extends App {

  import MongoCRHelper._

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

    override def clzString(category: Category): String = category.name

    override def stringToClz(n: String): Category = n.category
  }

  object CategoryCodecProvider extends EnumCodecProvider[Category] {
    override val enumCodec =
      new CaseObjectEnumCodec[Category](new CategoryEnumClz, "_t")

    override def isCaseObjectEnum[T](clazz: Class[T]): Boolean =
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