package com.github.jacobbishopxy.eiDashboard

import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import spray.json._

/**
 * Created by Jacob Xie on 7/6/2020
 */
object Model extends DefaultJsonProtocol {

  case class Coordinate(i: String, x: Int, y: Int, h: Int, w: Int)

  case class Content(title: String,
                     contentType: String,
                     contentData: String,
                     contentConfig: Option[String])

  case class GridModel(coordinate: Coordinate, content: Content)

  case class GridLayout(template: String, panel: String, layouts: Seq[GridModel])

  case class GridTemplatePanel(template: String, panel: String)

  val CRGridLayout: CodecRegistry =
    fromRegistries(fromProviders(
      classOf[Coordinate],
      classOf[Content],
      classOf[GridModel],
      classOf[GridLayout],
      classOf[GridTemplatePanel],
    ), DEFAULT_CODEC_REGISTRY)


  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat5(Coordinate)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat4(Content)
  implicit val gridModelFormat: RootJsonFormat[GridModel] = jsonFormat2(GridModel)
  implicit val gridLayoutFormat: RootJsonFormat[GridLayout] = jsonFormat3(GridLayout)
  implicit val gridTPFormat: RootJsonFormat[GridTemplatePanel] = jsonFormat2(GridTemplatePanel)

}

object ProModel extends DefaultJsonProtocol {

  /*
  anchor
      |___store
      |___element
   */

  import Namespace._

  sealed abstract class DB(n: String) {
    def name: String = n
  }
  object DB {
    case object Template extends DB(DbName.template)
    case object Industry extends DB(DbName.industry)
    case object Market extends DB(DbName.market)
    case object Bank extends DB(DbName.bank)
  }


  sealed abstract class Category(n: String) {
    def name: String = n
  }
  object Category {
    case object EmbedLink extends Category(CategoryName.embedLink)
    case object Text extends Category(CategoryName.text)
    case object TargetPrice extends Category(CategoryName.targetPrice)
    case object Image extends Category(CategoryName.image)
    case object FileList extends Category(CategoryName.fileList)
    case object FileManager extends Category(CategoryName.fileManager)
    case object EditableTable extends Category(CategoryName.editableTable)
    case object Table extends Category(CategoryName.table)
    case object Lines extends Category(CategoryName.lines)
    case object Histogram extends Category(CategoryName.histogram)
    case object Pie extends Category(CategoryName.pie)
    case object Scatter extends Category(CategoryName.scatter)
    case object Heatmap extends Category(CategoryName.heatmap)
    case object Box extends Category(CategoryName.box)
    case object Tree extends Category(CategoryName.tree)
    case object TreeMap extends Category(CategoryName.treeMap)
  }

  object DbFinder {
    implicit class Finder(n: String) {
      def db: DB = n match {
        case DbName.template => DB.Template
        case DbName.industry => DB.Industry
        case DbName.market => DB.Market
        case DbName.bank => DB.Bank
        case _ => throw new RuntimeException(s"database: $n not found!")
      }
    }
  }

  object CategoryFinder {
    implicit class Finder(n: String) {
      def category: Category = n match {
        case CategoryName.embedLink => Category.EmbedLink
        case CategoryName.text => Category.Text
        case CategoryName.targetPrice => Category.TargetPrice
        case CategoryName.image => Category.Image
        case CategoryName.fileList => Category.FileList
        case CategoryName.fileManager => Category.FileManager
        case CategoryName.editableTable => Category.EditableTable
        case CategoryName.table => Category.Table
        case CategoryName.lines => Category.Lines
        case CategoryName.histogram => Category.Histogram
        case CategoryName.pie => Category.Pie
        case CategoryName.scatter => Category.Scatter
        case CategoryName.heatmap => Category.Heatmap
        case CategoryName.box => Category.Box
        case CategoryName.tree => Category.Tree
        case CategoryName.treeMap => Category.TreeMap
        case _ => throw new RuntimeException(s"category: $n not found!")
      }
    }
  }


  case class AnchorKey(identity: String, category: Category)
  case class AnchorConfig(symbol: Option[String], date: Option[String])
  case class Anchor(anchorKey: AnchorKey, anchorConfig: Option[AnchorConfig])

  case class Content(data: String, config: Option[Map[String, Any]])
  case class Store(anchorKey: AnchorKey, anchorConfig: Option[AnchorConfig], content: Content)

  case class Coordinate(x: Int, y: Int, h: Int, w: Int)
  case class Element(anchorKey: AnchorKey, coordinate: Coordinate)

  case class TemplatePanel(template: String, panel: String)
  case class Layout(templatePanel: TemplatePanel, layouts: Seq[Element])

  case class LayoutWithStore(templatePanel: TemplatePanel,
                             layouts: Seq[Element],
                             stores: Seq[Store])

  case class DbCollection(db: DB, collectionName: String)

}

trait ProModel extends DefaultJsonProtocol {

  import ProModel._
  import Namespace.EnumIdentifierName
  import com.github.jacobbishopxy.MongoEnumHelper._

  // codecRegistry for mongodb driver

  class CategoryEnumClz extends EnumClz[Category] {

    import CategoryFinder._

    override def clzToString(clz: Category): String = clz.name

    override def stringToClz(n: String): Category = n.category
  }

  object CategoryCodecProvider extends MongoEnumCodecProvider[Category] {
    override val enumCodec: MongoEnumCodec[Category] =
      MongoEnumCodec[Category](new CategoryEnumClz, EnumIdentifierName.category)

    override def isEnum[T](clazz: Class[T]): Boolean = {
      clazz.isInstance(Category.EmbedLink) ||
        clazz.isInstance(Category.Text) ||
        clazz.isInstance(Category.TargetPrice) ||
        clazz.isInstance(Category.Image) ||
        clazz.isInstance(Category.FileList) ||
        clazz.isInstance(Category.FileManager) ||
        clazz.isInstance(Category.EditableTable) ||
        clazz.isInstance(Category.Table) ||
        clazz.isInstance(Category.Lines) ||
        clazz.isInstance(Category.Histogram) ||
        clazz.isInstance(Category.Pie) ||
        clazz.isInstance(Category.Scatter) ||
        clazz.isInstance(Category.Heatmap) ||
        clazz.isInstance(Category.Box) ||
        clazz.isInstance(Category.Tree) ||
        clazz.isInstance(Category.TreeMap)
    }
  }

  val CR: CodecRegistry = fromRegistries(fromProviders(
    CategoryCodecProvider,
    classOf[AnchorKey],
    classOf[AnchorConfig],
    classOf[Anchor],
    classOf[Content],
    classOf[Store],
    classOf[Coordinate],
    classOf[Element],
    classOf[TemplatePanel],
    classOf[Layout],
  ), DEFAULT_CODEC_REGISTRY)


  // json support for akka http

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
    }

    def read(value: JsValue): Any = value match {
      case JsNumber(n) => n.toDouble
      case JsString(s) => s
      case JsTrue => true
      case JsFalse => false
    }
  }

  implicit object JsonSupportDb extends JsonFormat[DB] {

    import DbFinder._

    override def write(obj: DB): JsValue = JsString(obj.name)

    override def read(json: JsValue): DB = json match {
      case JsString(v) => v.db
      case _ => throw new RuntimeException(s"AnyJsonFormat write failed: ${json.toString}")
    }
  }

  implicit object JsonSupportCategory extends JsonFormat[Category] {

    import CategoryFinder._

    override def write(obj: Category): JsValue = JsString(obj.name)

    override def read(json: JsValue): Category = json match {
      case JsString(v) => v.category
      case _ => throw new RuntimeException(s"AnyJsonFormat write failed: ${json.toString}")
    }
  }

  implicit val anchorKeyFormat: RootJsonFormat[AnchorKey] = jsonFormat2(AnchorKey)
  implicit val anchorConfigFormat: RootJsonFormat[AnchorConfig] = jsonFormat2(AnchorConfig)
  implicit val anchorFormat: RootJsonFormat[Anchor] = jsonFormat2(Anchor)
  implicit val contentFormat: RootJsonFormat[Content] = jsonFormat2(Content)
  implicit val storeFormat: RootJsonFormat[Store] = jsonFormat3(Store)
  implicit val coordinateFormat: RootJsonFormat[Coordinate] = jsonFormat4(Coordinate)
  implicit val elementFormat: RootJsonFormat[Element] = jsonFormat2(Element)
  implicit val templatePanelFormat: RootJsonFormat[TemplatePanel] = jsonFormat2(TemplatePanel)
  implicit val layoutFormat: RootJsonFormat[Layout] = jsonFormat2(Layout)
  implicit val layoutWithStoreFormat: RootJsonFormat[LayoutWithStore] = jsonFormat3(LayoutWithStore)

}

