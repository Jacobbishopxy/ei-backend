package com.github.jacobbishopxy.eiFile

import better.files.File
import com.typesafe.config.ConfigFactory

/**
 * Created by Jacob Xie on 7/9/2020
 */
object Repo extends Model {

  import Model._

  private val cfg = ConfigFactory.load.getConfig("ei-backend")
  private val marketFolderPath = cfg.getString("mount.market")
  private val bankFolderPath = cfg.getString("mount.bank")

  val folderPathMap = Map(
    "market" -> marketFolderPath,
    "bank" -> bankFolderPath
  )


  def getFolderStructure(folderPath: String, removePathDir: Boolean = false): List[FS] = {
    File(folderPath).list.map { f =>
      if (f.isDirectory) {
        val fs = f.pathAsString
        val ff = if (removePathDir) fs.replace(folderPath, "") else fs
        FolderFS(ff, getFolderStructure(fs, removePathDir))
      } else {
        val ff = if (removePathDir) f.pathAsString.replace(folderPath, "") else f.pathAsString
        FileFS(ff)
      }
    }.toList
  }


}
