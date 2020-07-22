package com.github.jacobbishopxy

import org.bson.{BsonInvalidOperationException, BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}

/**
 * Created by Jacob Xie on 7/22/2020
 */
object MongoEnumHelper {
  trait EnumClz[T] {
    def clzToString(clz: T): String

    def stringToClz(n: String): T
  }

  class MongoEnumCodec[T](clz: EnumClz[T], identifier: String) extends Codec[T] {
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
      val name = clz.clzToString(value)
      writer.writeStartDocument()
      writer.writeString(identifier, name)
      writer.writeEndDocument()
    }

    override def getEncoderClass: Class[T] = clz.getClass.asInstanceOf[Class[T]]
  }

  object MongoEnumCodec {
    def apply[T](clz: EnumClz[T], identifier: String): MongoEnumCodec[T] =
      new MongoEnumCodec(clz, identifier)
  }

  trait MongoEnumCodecProvider[E] extends CodecProvider {

    def isEnum[T](clazz: Class[T]): Boolean

    val enumCodec: MongoEnumCodec[E]

    override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] =
      if (isEnum(clazz)) enumCodec.asInstanceOf[Codec[T]] else null
  }
}

