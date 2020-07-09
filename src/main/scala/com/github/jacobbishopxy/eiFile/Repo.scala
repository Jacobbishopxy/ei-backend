package com.github.jacobbishopxy.eiFile

import better.files.File
import com.typesafe.config.ConfigFactory

/**
 * Created by Jacob Xie on 7/9/2020
 */
object Repo extends Model {

  import Model._

  val bankFolderPath: String = ConfigFactory.load.getConfig("ei-backend").getString("mount.bank")

  val folderPathMap = Map(
    "bank" -> bankFolderPath
  )


  def getFolderStructure(folderPath: String, removePathDir: Boolean = false): List[FS] = {
    File(folderPath).list.map { f =>
      if (f.isDirectory) {
        val fs = f.pathAsString.replaceAll(" ", "\\ ")
        val ff = if (removePathDir) fs.replace(folderPath, "") else fs
        FolderFS(ff, getFolderStructure(fs, removePathDir))
      } else {
        val ff = (if (removePathDir) f.pathAsString.replace(folderPath, "") else f.pathAsString).replaceAll(" ", "\\ ")
        FileFS(ff)
      }
    }.toList
  }


}
