package com.github.jacobbishopxy

import org.bson.BsonType
import spray.json._

/**
 * Created by Jacob Xie on 3/26/2020
 */
object Utilities {

  object MongoTypeMapping extends DefaultJsonProtocol {
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

    def intToString(d: Int): String = d match {
      case 1 => "double"
      case 2 => "string"
      case 3 => "object"
      case 4 => "array"
      case 5 => "binData"
      case 7 => "objectId"
      case 8 => "bool"
      case 9 => "date"
      case 10 => "null"
      case 11 => "regex"
      case 16 => "int"
      case 17 => "timestamp"
      case 18 => "long"
      case 19 => "decimal"
      case _ => throw new RuntimeException(s"intToBsonType unmatched item: $d")
    }

    def stringToInt(d: String): Int = d match {
      case "double" => 1
      case "string" => 2
      case "object" => 3
      case "array" => 4
      case "binData" => 5
      case "objectId" => 7
      case "bool" => 8
      case "date" => 9
      case "null" => 10
      case "regex" => 11
      case "int" => 16
      case "timestamp" => 17
      case "long" => 18
      case "decimal" => 19
      case _ => throw new RuntimeException(s"bsonTypeToInt unmatched item: $d")
    }

    def convertJsValueByType(jsValue: JsValue, stringType: String): Any = stringType match {
      case "double" => jsValue.convertTo[Double]
      case "string" => jsValue.convertTo[String]
      case "bool" => jsValue.convertTo[Boolean]
      case "date" => jsValue.convertTo[String]
      case "int" => jsValue.convertTo[Int]
    }

  }

}
