import org.bson.{BsonInvalidOperationException, BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection

import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * Created by Jacob Xie on 7/21/2020
 */
object DevMongoCR extends App with DevMongoRepo {

  sealed trait CaseObjectEnum
  object CaseObjectEnum {
    case object Alpha extends CaseObjectEnum
    case object Bravo extends CaseObjectEnum
    case object Charlie extends CaseObjectEnum
  }

  object CaseObjectEnumCodecProvider extends CodecProvider {
    def isCaseObjectEnum[T](clazz: Class[T]): Boolean = {
      clazz.isInstance(CaseObjectEnum.Alpha) ||
        clazz.isInstance(CaseObjectEnum.Bravo) ||
        clazz.isInstance(CaseObjectEnum.Charlie)
    }

    override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
      if (isCaseObjectEnum(clazz)) {
        CaseObjectEnumCodec.asInstanceOf[Codec[T]]
      } else {
        null
      }
    }

    object CaseObjectEnumCodec extends Codec[CaseObjectEnum] {
      val identifier = "_t"

      override def decode(reader: BsonReader, decoderContext: DecoderContext): CaseObjectEnum = {
        reader.readStartDocument()
        val enumName = reader.readString(identifier)
        reader.readEndDocument()
        enumName match {
          case "Alpha" => CaseObjectEnum.Alpha
          case "Bravo" => CaseObjectEnum.Bravo
          case "Charlie" => CaseObjectEnum.Charlie
          case _ => throw new BsonInvalidOperationException(s"$enumName is an invalid value for a CaseObjectEnum object")
        }
      }

      override def encode(writer: BsonWriter, value: CaseObjectEnum, encoderContext: EncoderContext): Unit = {
        val name = value match {
          case CaseObjectEnum.Alpha => "Alpha"
          case CaseObjectEnum.Bravo => "Bravo"
          case CaseObjectEnum.Charlie => "Charlie"
        }
        writer.writeStartDocument()
        writer.writeString(identifier, name)
        writer.writeEndDocument()
      }

      override def getEncoderClass: Class[CaseObjectEnum] = CaseObjectEnum.getClass.asInstanceOf[Class[CaseObjectEnum]]
    }
  }

  case class ContainsMyEnum(myEnum: CaseObjectEnum)

  val cr = fromRegistries(fromProviders(
    CaseObjectEnumCodecProvider,
    classOf[ContainsMyEnum],
  ), DEFAULT_CODEC_REGISTRY)

  val db = database.withCodecRegistry(cr)
  val collection: MongoCollection[ContainsMyEnum] = db.getCollection("cr-test")

  val res1 = Await.result(collection.find().toFuture(), 10.seconds)
  println(res1)

  val md = ContainsMyEnum(CaseObjectEnum.Alpha)
  val res2 = Await.result(collection.insertOne(md).toFuture(), 10.seconds)
  println(res2)
}
