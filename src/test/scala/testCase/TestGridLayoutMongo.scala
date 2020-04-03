package testCase

import com.github.jacobbishopxy.eiGridLayout.Repo
import org.mongodb.scala._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Jacob Xie on 3/18/2020
 */
object TestGridLayoutMongo extends App {

  import Repo._

  val fakeLayouts = Seq(
    GridModel(Coordinate("0", 0, 0, 100, 200), Content("test1", "table", "demo link")),
    GridModel(Coordinate("1", 2, 2, 200, 250), Content("test2", "embedLink", "http://example.com")),
  )
  val fakeData: GridLayout = GridLayout("test", fakeLayouts)

  val observable: Observable[Completed] = collection.insertOne(fakeData)

  val res = Await.result(observable.toFuture(), 10.seconds)

  println(res)
}
