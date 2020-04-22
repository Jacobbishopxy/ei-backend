package testCase

import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala.Document
import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by Jacob Xie on 4/1/2020
 */
object TestMongoAPI extends App with TestMongoAPIRepo {

  import com.github.jacobbishopxy.MongoModel._
  import com.github.jacobbishopxy.MongoLoader

  val repo = new MongoLoader(mongoUrl, "dev")


  val filter = ComplexQuery(Map("name" -> SimpleLogic($in=Some(Seq("MZ").map(_.toJson)))))
  val conditions = QueryContent(Some(10), Some(filter))

  val foo: Future[Seq[Document]] = repo.fetchData("user", conditions)

  val res = Await.result(foo, 10.seconds)
  println(res)

}


trait TestMongoAPIRepo {

  val config: Config = ConfigFactory.load.getConfig("ei-backend")
  val mongoUrl: String = config.getString("mongo.url")

}
