package com.github.jacobbishopxy.eiFile

import spray.json._

/**
 * Created by Jacob Xie on 7/9/2020
 */
trait Model {

  import Model._


  implicit object FSJsonSupport extends RootJsonWriter[FS] with DefaultJsonProtocol {
    override def write(obj: FS): JsValue = obj match {
      case FileFS(n) => n.toJson
      case FolderFS(n, c) => Map(n -> c.map(write)).toJson
    }
  }

  implicit object FSListJsonSupport extends RootJsonWriter[List[FS]] with DefaultJsonProtocol {
    override def write(obj: List[FS]): JsValue =
      obj.map(FSJsonSupport.write).toJson
  }

}


object Model {

  trait FS

  case class FileFS(n: String) extends FS

  case class FolderFS(n: String, c: List[FS]) extends FS


}
