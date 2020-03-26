package com.github.jacobbishopxy

import org.bson.BsonType

/**
 * Created by Jacob Xie on 3/26/2020
 */
object Utilities {

  def intToBsonType(d: Int): BsonType = d match {
    case 1 => BsonType.DOUBLE
    case 2 => BsonType.STRING
    case 3 => BsonType.DOCUMENT
    case 4 => BsonType.ARRAY
    case 5 => BsonType.BINARY
    case 7 => BsonType.OBJECT_ID
    case 8 => BsonType.BOOLEAN
    case 9 => BsonType.DATE_TIME
    case 10 => BsonType.NULL
    case 11 => BsonType.REGULAR_EXPRESSION
    case 16 => BsonType.INT32
    case 17 => BsonType.TIMESTAMP
    case 18 => BsonType.INT64
    case 19 => BsonType.DECIMAL128
    case _ => throw new RuntimeException(s"intToBsonType unmatched item: $d")
  }

  def bsonTypeToInt(d: BsonType): Int = d match {
    case BsonType.DOUBLE => 1
    case BsonType.STRING => 2
    case BsonType.DOCUMENT => 3
    case BsonType.ARRAY => 4
    case BsonType.BINARY => 5
    case BsonType.OBJECT_ID => 7
    case BsonType.BOOLEAN => 8
    case BsonType.DATE_TIME => 9
    case BsonType.NULL => 10
    case BsonType.REGULAR_EXPRESSION => 11
    case BsonType.INT32 => 16
    case BsonType.TIMESTAMP => 17
    case BsonType.INT64 => 18
    case BsonType.DECIMAL128 => 19
    case _ => throw new RuntimeException(s"bsonTypeToInt unmatched item: $d")
  }

}
