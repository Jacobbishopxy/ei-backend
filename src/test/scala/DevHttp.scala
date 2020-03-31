
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Created by Jacob Xie on 3/30/2020
 */
object DevHttp1 extends App with DevHttpRepo {

  /**
   * This demo shows Http Post and convert to Bson document
   */

  /* post json
  {
      "limit": 10,
      "filter": {
          "and": {
              "date": {
                  "gt": 20180101,
                  "lte": 20201231
              },
              "username": {
                  "eq": "J"
              }
          }
      }
  }
   */

  /* result
  And Filter{filters=[And Filter{filters=[Operator Filter{fieldName='date', operator='$gt', value=20180101}, Operator Filter{fieldName='date', operator='$lte', value=20201231}]}, And Filter{filters=[Filter{fieldName='username', value="J"}]}]}
   */

  import org.mongodb.scala.bson.BsonDocument
  import org.mongodb.scala.bson.conversions.Bson
  import org.mongodb.scala.model.Filters


  final case class FilterOptions(eq: Option[JsValue],
                                 gt: Option[JsValue],
                                 lt: Option[JsValue],
                                 gte: Option[JsValue],
                                 lte: Option[JsValue])

  type ConjunctionsType = Map[String, FilterOptions]

  trait Conjunctions
  final case class AND(and: ConjunctionsType) extends Conjunctions
  final case class OR(or: ConjunctionsType) extends Conjunctions

  case class QueryContent(limit: Option[Int], filter: Option[Conjunctions])

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val filterOptionsFormat: RootJsonFormat[FilterOptions] = jsonFormat5(FilterOptions)

    implicit object conjunctionsFormat extends RootJsonFormat[Conjunctions] {

      private def findSetFO(json: JsValue): ConjunctionsType =
        json.convertTo[ConjunctionsType]

      override def write(obj: Conjunctions): JsValue = obj match {
        case AND(and) => and.toJson
        case OR(or) => or.toJson
      }

      override def read(json: JsValue): Conjunctions = json match {
        case JsObject(fields) =>
          val res = fields.head
          if (res._1 == "and") AND(findSetFO(res._2))
          else if (res._1 == "or") OR(findSetFO(res._2))
          else throw new RuntimeException("Invalid JSON format")
        case _ => throw new RuntimeException("Invalid JSON format")
      }
    }

    implicit val queryContentFormat: RootJsonFormat[QueryContent] = jsonFormat2(QueryContent)
  }

  class JsonService extends Directives with JsonSupport {

    val route: Route =
      concat(
        post {
          entity(as[QueryContent]) { qc =>
            val limit = qc.limit
            val filter = qc.filter match {
              case Some(v) => getFilter(v)
              case None => BsonDocument()
            }

            complete(filter.toString)
          }
        }
      )

    private def extractFilter(name: String, filterOptions: FilterOptions): Bson = {
      val eq = filterOptions.eq.fold(Option.empty[Bson]) { i => Some(Filters.eq(name, i)) }
      val gt = filterOptions.gt.fold(Option.empty[Bson]) { i => Some(Filters.gt(name, i)) }
      val lt = filterOptions.lt.fold(Option.empty[Bson]) { i => Some(Filters.lt(name, i)) }
      val gte = filterOptions.gte.fold(Option.empty[Bson]) { i => Some(Filters.gte(name, i)) }
      val lte = filterOptions.lte.fold(Option.empty[Bson]) { i => Some(Filters.lte(name, i)) }

      val r = List(eq, gt, lt, gte, lte).foldLeft(List.empty[Bson]) {
        case (acc, ob) => ob match {
          case None => acc
          case Some(v) => acc :+ v
        }
      }
      Filters.and(r: _*)
    }

    private def getFilter(filter: Conjunctions): Bson = filter match {
      case AND(and) =>
        val res = and.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
        Filters.and(res: _*)
      case OR(or) =>
        val res = or.map { case (name, filterOptions) => extractFilter(name, filterOptions) }.toList
        Filters.or(res: _*)
    }

  }

  run(new JsonService().route)
}


trait DevHttpRepo {

  implicit val system: ActorSystem = ActorSystem("Server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val (host, port) = ("0.0.0.0", 2000)

  def run(route: Route): Unit = {

    val bindFuture: Future[Http.ServerBinding] = Http().bindAndHandle(route, host, port)

    bindFuture.failed.foreach { ex =>
      println(ex, s"Failed to bind to $host, $port")
    }
  }

}

