package testCase

import org.mongodb.scala._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 3/18/2020
 */
object TestGridLayoutMongo extends App {

  import com.github.jacobbishopxy.eiDashboard.Model._
  import com.github.jacobbishopxy.eiDashboard.Repo._


  val fakeLayouts = Seq(
    GridModel(Coordinate("0", 0, 0, 100, 200), Content("test1", "table", "demo link", None)),
    GridModel(Coordinate("1", 2, 2, 200, 250), Content("test2", "embedLink", "http://example.com", None)),
  )
  val fakeData: GridLayout = GridLayout("600036.SH", "test", fakeLayouts)

  val observable: Observable[Completed] = getCollection("bank", "test").insertOne(fakeData)

  val res = Await.result(observable.toFuture(), 10.seconds)

  println(res)
}
