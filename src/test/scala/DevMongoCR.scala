import org.bson.{BsonInvalidOperationException, BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration._

/**
 * Created by Jacob Xie on 7/21/2020
 */
object DevMongoCR {

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
}
