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

  private val getFolderPathByType = (fType: String) =>
    folderPathMap.getOrElse(fType, throw new RuntimeException("type not found"))

  private val getRmOption = (rm: Option[String]) =>
    rm.fold(false) { r => if (r == "true") true else false }


  private val listFileStructure = path("listFileStructure") {
    get {
      parameter(Symbol("type"), "subFolderPath".?, "removeFolderDir".?) { (tp, sfp, rfd) =>
        val fp = getFolderPathByType(tp)
        val rm = getRmOption(rfd)
        val res = sfp match {
          case None => getFolderStructure(fp, rm)
          case Some(s) => getFolderStructure((File(fp) / s).pathAsString, rm)
        }
        complete(res.toJson)
      }
    }
  }

  private val listProFileStructure = path("listProFileStructure") {
    get {
      parameter(Symbol("type"), "subFolderPath".?, "removeFolderDir".?) { (tp, sfp, rfd) =>
        val fp = getFolderPathByType(tp)
        val rm = getRmOption(rfd)
        val res = sfp match {
          case None => getProFolderStructure(fp, rm)
          case Some(s) => getProFolderStructure((File(fp) / s).pathAsString, rm)
        }
        complete(res.toJson)
      }
    }
  }


  val route: Route = pathPrefix("file") {
    concat(
      listFileStructure,
      listProFileStructure
    )
  }

}
