package testCase

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import better.files.File
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
 * Created by Jacob Xie on 7/5/2020
 */
object TestHttpFileHandling extends SprayJsonSupport {

  trait FS

  case class FileFS(n: String) extends FS

  case class FolderFS(n: String, c: List[FS]) extends FS


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


  def getFolderStructure(folderPath: String, removePathDir: Boolean = false): List[FS] = {
    File(folderPath).list.map { f =>
      if (f.isDirectory) {
        val fs = f.pathAsString
        println(fs)
        val ff = if (removePathDir) fs.replace(folderPath, "") else fs
        FolderFS(ff, getFolderStructure(fs, removePathDir))
      } else {
        val ff = if (removePathDir) f.pathAsString.replace(folderPath, "") else f.pathAsString
        FileFS(ff)
      }
    }.toList
  }


  val folderPath: String = ConfigFactory.load.getConfig("ei-backend").getString("mount.bank")


  def listFileStructure: Route = path("listFileStructure") {
    get {
      parameter("subFolderPath".?, "removeFolderDir".?) { (sfp, rfd) =>
        val rm = rfd.fold(false) { r => if (r == "true") true else false }
        val res = sfp match {
          case None => getFolderStructure(folderPath, rm)
          case Some(s) => getFolderStructure((File(folderPath) / s).pathAsString, rm)
        }
        complete(res.toJson)

      }
    }
  }

  val route: Route = extractRequestContext { ctx =>

    concat(
      listFileStructure
    )
  }
}


object TestHttpFileHandlingMain extends App {

  implicit val system: ActorSystem = ActorSystem("TestHttpFileHandling")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val (host, port) = ("localhost", 2020)
  val bindFuture = Http().bindAndHandle(TestHttpFileHandling.route, host, port)

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}

