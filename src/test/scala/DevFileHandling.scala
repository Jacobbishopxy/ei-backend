
import com.typesafe.config.ConfigFactory
import better.files._
import spray.json._


/**
 * Created by Jacob Xie on 7/9/2020
 */
object DevFileHandling {


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


  def getFolderStructure(folderPath: String): List[FS] = {
    File(folderPath).list.map { f =>
      if (f.isDirectory) FolderFS(f.pathAsString, getFolderStructure(f.pathAsString))
      else FileFS(f.pathAsString)
    }.toList
  }


}

object DevFileHandlingMain extends App {

  import DevFileHandling._

  private val folderPath = ConfigFactory.load.getConfig("ei-backend").getString("mount.bank")

  val foo = getFolderStructure(folderPath).toJson

  println(foo)

}