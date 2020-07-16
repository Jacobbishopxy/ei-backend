package com.github.jacobbishopxy.eiFile

import spray.json._

/**
 * Created by Jacob Xie on 7/9/2020
 */
trait Model extends DefaultJsonProtocol {

  import Model._

  /*
  FS json support
   */

  implicit object FSJsonSupport extends RootJsonWriter[FS] {
    override def write(obj: FS): JsValue = obj match {
      case FileFS(n) => n.toJson
      case FolderFS(n, c) => Map(n -> c.map(write)).toJson
    }
  }

  implicit object FSListJsonSupport extends RootJsonWriter[List[FS]] {
    override def write(obj: List[FS]): JsValue =
      obj.map(FSJsonSupport.write).toJson
  }

  /*
  ProFS json support
   */

  implicit object ProFSJsonSupport extends RootJsonWriter[ProFS] {
    override def write(obj: ProFS): JsValue = obj match {
      case ProFileFS(n, v, l) =>
        JsObject(
          "name" -> JsString(n),
          "size" -> JsNumber(v),
          "lastModifiedTime" -> JsString(l)
        )
      case ProFolderFS(n, c) =>
        JsObject(
          "name" -> JsString(n),
          "children" -> JsArray(c.map(write).toVector)
        )
    }
  }

  implicit object PFSListJsonSupport extends RootJsonWriter[List[ProFS]] {
    override def write(obj: List[ProFS]): JsValue =
      obj.map(ProFSJsonSupport.write).toJson
  }

  implicit object ProFolderFSJsonSupport extends RootJsonWriter[ProFolderFS] {
    override def write(obj: ProFolderFS): JsValue =
      JsObject(
        "name" -> JsString(obj.name),
        "children" -> obj.children.toJson
      )
  }

}


object Model {

  trait FS

  case class FileFS(n: String) extends FS

  case class FolderFS(n: String, c: List[FS]) extends FS

  trait ProFS

  case class ProFileFS(name: String, size: Long, lastModifiedTime: String) extends ProFS

  case class ProFolderFS(name: String, children: List[ProFS]) extends ProFS

}
