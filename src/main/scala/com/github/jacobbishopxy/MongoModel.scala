package com.github.jacobbishopxy

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

/**
 * Created by Jacob Xie on 4/1/2020
 */
object MongoModel {

  /**
   * validator support
   */

  final case class ColIdx(ascending: Boolean)
  final case class Col(fieldName: String,
                       nameAlias: String,
                       fieldType: Int,
                       indexOption: Option[ColIdx] = None,
                       description: Option[String] = None)
  final case class Cols(collectionName: String,
                        cols: List[Col])

  trait ValidatorActions
  case class AddEle(fieldName: String,
                    nameAlias: String,
                    fieldType: Int,
                    description: Option[String] = None) extends ValidatorActions
  case class DelEle(fieldName: String) extends ValidatorActions

  case class ValidatorContent(actions: List[ValidatorActions])


  trait ValidatorJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val colIdxFormat: RootJsonFormat[ColIdx] = jsonFormat1(ColIdx)
    implicit val colFormat: RootJsonFormat[Col] = jsonFormat5(Col)
    implicit val colsFormat: RootJsonFormat[Cols] = jsonFormat2(Cols)

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

    implicit val addEleFormat: RootJsonFormat[AddEle] = jsonFormat4(AddEle)
    implicit val delEleFormat: RootJsonFormat[DelEle] = jsonFormat1(DelEle)


    implicit object ValidatorActionsFormat extends JsonFormat[ValidatorActions] {
      override def write(obj: ValidatorActions): JsValue = obj match {
        case a @ AddEle(_, _, _, _) => a.toJson
        case a @ DelEle(_) => a.toJson
      }

      override def read(json: JsValue): ValidatorActions = json match {
        case JsObject(fields) =>
          if (fields.contains("nameAlias"))
            AddEle(
              fieldName = fields.get("fieldName").fold("")(_.toString),
              nameAlias = fields.get("nameAlias").fold("")(_.toString),
              fieldType = fields.get("filedType").fold(0)(_.toString.toInt),
              description = fields.get("description").fold(Option.empty[String])(i => Some(i.toString))
            ) else
            DelEle(
              fieldName = fields.get("fieldName").fold("")(_.toString),
            )
        case _ => throw new RuntimeException(s"Invalid JSON format $json")
      }
    }

    implicit val validatorContentFormat: RootJsonFormat[ValidatorContent] = jsonFormat1(ValidatorContent)
  }


  /**
   * query content support
   */

  final case class FilterOptions(eq: Option[JsValue],
                                 gt: Option[JsValue],
                                 lt: Option[JsValue],
                                 gte: Option[JsValue],
                                 lte: Option[JsValue])

  type ConjunctionsType = Map[String, FilterOptions]

  trait Conjunctions
  final case class AND(and: ConjunctionsType) extends Conjunctions
  final case class OR(or: ConjunctionsType) extends Conjunctions

  case class QueryContent(limit: Option[Int], filter: Option[Conjunctions])


  trait ConjunctionsJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val filterOptionsFormat: RootJsonFormat[FilterOptions] = jsonFormat5(FilterOptions)

    implicit object conjunctionsFormat extends RootJsonFormat[Conjunctions] {

      private def findSetFO(json: JsValue): ConjunctionsType =
        json.convertTo[ConjunctionsType]

      override def write(obj: Conjunctions): JsValue = obj match {
        case AND(and) => and.toJson
        case OR(or) => or.toJson
      }

      override def read(json: JsValue): Conjunctions = json match {
        case JsObject(fields) =>
          val res = fields.head
          if (res._1 == "and") AND(findSetFO(res._2))
          else if (res._1 == "or") OR(findSetFO(res._2))
          else throw new RuntimeException(s"Invalid JSON format $json")
        case _ => throw new RuntimeException(s"Invalid JSON format $json")
      }
    }

    implicit val queryContentFormat: RootJsonFormat[QueryContent] = jsonFormat2(QueryContent)
  }

}
