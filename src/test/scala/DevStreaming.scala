import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.util.Random
import scala.io.StdIn

/**
 * Created by Jacob Xie on 3/13/2020
 */
object DevStreaming extends App {

  implicit val system: ActorSystem = ActorSystem("DevStreaming")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val numbers = Source.fromIterator(() => Iterator.continually(Random.nextInt()))

  val route =
    path("random") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            numbers.map(n => ByteString(s"$n\n"))
          )
        )
      }
    }

  val port = 2020
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)

  println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
