package com.github.jacobbishopxy

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


/**
 * Created by Jacob Xie on 4/1/2020
 */
object MongoModel {

  /**
   * base data structure
   */

  final case class IndexOption(ascending: Boolean)

  final case class FieldInfo(fieldName: String,
                             nameAlias: String,
                             fieldType: Int,
                             indexOption: Option[IndexOption] = None, // primary key if not None
                             description: Option[String] = None)

  final case class CollectionInfo(collectionName: String,
                                  fields: List[FieldInfo])

  trait ValidatorActions

  case class AddField(fieldName: String,
                      nameAlias: String,
                      fieldType: Int,
                      description: Option[String] = None) extends ValidatorActions

  case class DelField(fieldName: String) extends ValidatorActions

  case class ValidatorContent(actions: List[ValidatorActions])

  /**
   * validator json support
   */

  final case class MongoValidatorJsonSchemaProperty(bsonType: String,
                                                    title: String,
                                                    description: String)

  final case class MongoValidatorJsonSchema(bsonType: String,
                                            required: Seq[String],
                                            properties: Map[String, MongoValidatorJsonSchemaProperty])

  final case class MongoValidator($jsonSchema: MongoValidatorJsonSchema)

  final case class MongoCollectionValidator(validator: MongoValidator)

  final case class MongoIndex(key: Map[String, Int],
                              name: String,
                              ns: String,
                              unique: Option[Boolean],
                              v: Int)

  trait MongoJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val mongoValidatorJsonSchemaPropertyFormat: RootJsonFormat[MongoValidatorJsonSchemaProperty] =
      jsonFormat3(MongoValidatorJsonSchemaProperty)
    implicit val mongoValidatorJsonSchemaFormat: RootJsonFormat[MongoValidatorJsonSchema] =
      jsonFormat3(MongoValidatorJsonSchema)
    implicit val mongoValidatorFormat: RootJsonFormat[MongoValidator] =
      jsonFormat1(MongoValidator)
    implicit val mongoCollectionValidatorFormat: RootJsonFormat[MongoCollectionValidator] =
      jsonFormat1(MongoCollectionValidator)
    implicit val mongoIndexFormat: RootJsonFormat[MongoIndex] =
      jsonFormat5(MongoIndex)

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
  }

  trait ValidatorJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val indexOptionFormat: RootJsonFormat[IndexOption] = jsonFormat1(IndexOption)
    implicit val fieldInfoFormat: RootJsonFormat[FieldInfo] = jsonFormat5(FieldInfo)
    implicit val collectionInfoFormat: RootJsonFormat[CollectionInfo] = jsonFormat2(CollectionInfo)
    implicit val addFieldFormat: RootJsonFormat[AddField] = jsonFormat4(AddField)
    implicit val delFieldFormat: RootJsonFormat[DelField] = jsonFormat1(DelField)

    implicit object ValidatorActionsFormat extends JsonFormat[ValidatorActions] {
      override def write(obj: ValidatorActions): JsValue = obj match {
        case a @ AddField(_, _, _, _) => a.toJson
        case a @ DelField(_) => a.toJson
      }

      override def read(json: JsValue): ValidatorActions = json match {
        case JsObject(fields) =>
          if (fields.contains("nameAlias"))
            AddField(
              fieldName = fields
                .get("fieldName")
                .map(_.convertTo[String])
                .getOrElse(throw new RuntimeException("fieldName required")),
              nameAlias = fields
                .get("nameAlias")
                .map(_.convertTo[String])
                .getOrElse(throw new RuntimeException("nameAlias required")),
              fieldType = fields
                .get("fieldType")
                .map(_.convertTo[Int])
                .getOrElse(throw new RuntimeException("fieldType required")),
              description = fields
                .get("description")
                .map(_.convertTo[String])
            )
          else DelField(
            fieldName = fields
              .get("fieldName")
              .map(_.convertTo[String])
              .getOrElse(throw new RuntimeException("fieldName required")),
          )
        case _ => throw new RuntimeException(s"Invalid JSON format $json")
      }
    }

    implicit val validatorContentFormat: RootJsonFormat[ValidatorContent] = jsonFormat1(ValidatorContent)
  }


  /**
   * query content json support
   */

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

  final case class QueryContent(limit: Option[Int], filter: Option[QueryType])


  trait QueryJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    private def extractSeqJsValue(json: JsValue, key: String): Seq[QueryType] =
      json.asJsObject.getFields(key).flatMap {
        _.convertTo[List[JsValue]].map(QueryTypeFormat.read)
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

    implicit val queryContentFormat: RootJsonFormat[QueryContent] = jsonFormat2(QueryContent)
  }

}

