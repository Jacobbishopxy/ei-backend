package com.github.jacobbishopxy

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import scala.util.{Failure, Success, Try}

/**
 * Created by Jacob Xie on 4/20/2020
 */
object MongoQueryModel {

  final case class SimpleLogic($not: Option[JsValue] = None,
                               $in: Option[Seq[JsValue]] = None,
                               $nin: Option[Seq[JsValue]] = None,
                               $regx: Option[JsValue] = None,
                               $gt: Option[JsValue] = None,
                               $gte: Option[JsValue] = None,
                               $lt: Option[JsValue] = None,
                               $lte: Option[JsValue] = None,
                               $bt: Option[(JsValue, JsValue)] = None,
                               $exists: Option[JsValue] = None,
                               $distinct: Option[JsValue] = None)


  trait QueryType

  case class SimpleQuery(d: Map[String, JsValue]) extends QueryType

  case class ComplexQuery(d: Map[String, SimpleLogic]) extends QueryType

  case class OR($or: Seq[QueryType]) extends QueryType

  case class AND($and: Seq[QueryType]) extends QueryType


  trait QueryModelSupport extends SprayJsonSupport with DefaultJsonProtocol {

    private def extractSeqJsValue(json: JsValue, key: String): Seq[QueryType] =
      json.asJsObject.getFields(key).flatMap {
        _.convertTo[List[JsValue]].map(QueryTypeFormat.read)
      }

    implicit object AnyJsonFormat extends JsonFormat[Any] {
      override def write(value: Any): JsValue = value match {
        case n: Int => JsNumber(n)
        case s: String => JsString(s)
        case b: Boolean if b => JsTrue
        case b: Boolean if !b => JsFalse
        case _ => throw new RuntimeException(s"AnyJsonFormat write failed: ${value.toString}")
      }

      override def read(value: JsValue): Any = value match {
        case JsNumber(n) => n.intValue
        case JsString(s) => s
        case JsTrue => true
        case JsFalse => false
        case _ => throw new RuntimeException(s"AnyJsonFormat read failed: ${value.toString}")
      }
    }

    implicit val simpleLogicFormat: RootJsonFormat[SimpleLogic] = jsonFormat11(SimpleLogic)

    implicit object QueryTypeFormat extends RootJsonFormat[QueryType] {
      override def write(obj: QueryType): JsValue = obj match {
        case SimpleQuery(d) => d.toJson
        case ComplexQuery(d) => d.toJson
        case OR(d) => Map("$or" -> d.map(write).toJson).toJson
        case AND(d) => Map("$and" -> d.map(write).toJson).toJson
        case e: Throwable => throw new RuntimeException(s"Invalid $obj, $e")
      }

      override def read(json: JsValue): QueryType = json match {
        case JsObject(fields) =>
          val res = fields.head // $and/$or only shows at the beginning of each nested structure
          if (res._1 == "$and") AND(extractSeqJsValue(json, "$and"))
          else if (res._1 == "$or") OR(extractSeqJsValue(json, "$or"))
          else {
            try {
              ComplexQuery(json.convertTo[Map[String, SimpleLogic]])
            } catch {
              case _: Throwable => try {
                SimpleQuery(json.convertTo[Map[String, JsValue]])
              } catch {
                case e: Throwable => throw new RuntimeException(e)
              }
            }
          }
        case _ => throw new RuntimeException(s"Invalid JSON format $json")
      }
    }

  }

}

