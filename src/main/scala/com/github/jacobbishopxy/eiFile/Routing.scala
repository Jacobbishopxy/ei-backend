package com.github.jacobbishopxy.eiFile

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import better.files.File
import spray.json._


/**
 * Created by Jacob Xie on 7/9/2020
 */
object Routing extends SprayJsonSupport {

  import Repo._


  private val listFileStructure = path("listFileStructure") {
    get {
      parameter(Symbol("type"), "subFolderPath".?, "removeFolderDir".?) { (tp, sfp, rfd) =>

        val fp = folderPathMap.getOrElse(tp, throw new RuntimeException("type not found"))

        val rm = rfd.fold(false) { r => if (r == "true") true else false }
        val res = sfp match {
          case None => getFolderStructure(fp, rm)
          case Some(s) => getFolderStructure((File(fp) / s).pathAsString, rm)
        }
        complete(res.toJson)
      }
    }
  }


  val route: Route = pathPrefix("file") {
    concat(
      listFileStructure
    )
  }

}
