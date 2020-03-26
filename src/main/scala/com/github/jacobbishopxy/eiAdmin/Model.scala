package com.github.jacobbishopxy.eiAdmin

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

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



}
