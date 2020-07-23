package com.github.jacobbishopxy.eiDashboard

/**
 * Created by Jacob Xie on 7/21/2020
 */
object Namespace {

  object ConfigName {
    val config = "research"
  }

  object DbName {
    val template = "template"
    val industry = "industry"
    val market = "market"
    val bank = "bank"
  }


  object CategoryName {

    val embedLink = "embedLink"
    val text = "text"
    val targetPrice = "targetPrice"
    val image = "image"
    val fileList = "fileList"
    val fileManager = "fileManager"
    val editableTable = "editableTable"
    val table = "table"
    val lines = "lines"
    val histogram = "histogram"
    val pie = "pie"
    val scatter = "scatter"
    val heatmap = "heatmap"
    val box = "box"
    val tree = "tree"
    val treeMap = "treeMap"
  }


  object FieldName {
    val db = "db"
    val collection = "collection"
    val template = "template"
    val panel = "panel"
    val templatePanel = "templatePanel"
    val anchor = "anchor"

    val identity = "identity"
    val category = "category"
    val symbol = "symbol"
    val date = "date"
  }


  object EnumIdentifierName {
    val category = "_enum_category"
  }


  object RouteName {
    val layouts = "layouts"
    val layout = "layout"
    val store = "store"

    val templateLayout = "template-layout"
    val industryStore = "industry-store"
  }
}
