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
