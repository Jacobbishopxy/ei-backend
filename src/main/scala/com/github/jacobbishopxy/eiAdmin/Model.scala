package com.github.jacobbishopxy.eiAdmin

import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 * Created by Jacob Xie on 3/23/2020
 */
object Model {

  final case class Col(name: String,
                       alias: String,
                       colType: Int,
                       isIndex: Boolean = false,
                       description: Option[String] = None)
  final case class Cols(name: String, cols: List[Col])

  implicit val colFormat: RootJsonFormat[Col] = jsonFormat5(Col)
  implicit val colsFormat: RootJsonFormat[Cols] = jsonFormat2(Cols)


  trait ValidatorActions
  case class AddEle(name: String, `type`: Int) extends ValidatorActions
  case class DelEle(name: String) extends ValidatorActions


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


  trait FilterOptions
  case class EQL(name: String) extends FilterOptions
  case class GT(name: String) extends FilterOptions
  case class LT(name: String) extends FilterOptions
  case class GTE(name: String) extends FilterOptions
  case class LTE(name: String) extends FilterOptions
  case object AND extends FilterOptions
  case object OR extends FilterOptions

  // todo: generator for mongo conditions

}
