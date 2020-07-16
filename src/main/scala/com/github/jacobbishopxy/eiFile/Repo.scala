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


  private val pathRemoveRoot = (r: String, f: String, rm: Boolean) =>
    if (rm) f.replace(r, "").replace("\\", "") else f

  def getFolderStructure(folderPath: String, removePathDir: Boolean = false): List[FS] = {
    File(folderPath).list.map { f =>
      val fs = f.pathAsString
      val ff = pathRemoveRoot(folderPath, fs, removePathDir)

      if (f.isDirectory)
        FolderFS(ff, getFolderStructure(fs, removePathDir))
      else
        FileFS(ff)
    }.toList
  }

  def getProFolderStructure(folderPath: String, removePathDir: Boolean = false): ProFolderFS = {

    def getListProFS(fp: String): List[ProFS] = {
      File(fp).list.map { f =>
        val fs = f.pathAsString
        val ff = pathRemoveRoot(fp, fs, removePathDir)

        if (f.isDirectory)
          ProFolderFS(ff, getListProFS(fs))
        else
          ProFileFS(ff, f.size, f.lastModifiedTime.toString)
      }
    }.toList

    ProFolderFS(pathRemoveRoot(folderPath, folderPath, removePathDir), getListProFS(folderPath))
  }

}

