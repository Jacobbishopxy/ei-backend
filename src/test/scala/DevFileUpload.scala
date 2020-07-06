import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Framing
import akka.util.ByteString

import java.io.File


import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

/**
 * Created by Jacob Xie on 7/5/2020
 */
object DevFileUpload {

  def fd: Route = path("fileDownload") {
    complete(HttpEntity.fromFile(ContentTypes.`application/octet-stream`, new File("./README.md")))
  }

  def fu(implicit m: Materializer, e: ExecutionContextExecutor): Route = path("fileUpload") {
    fileUpload("csv") {
      case (metadata, byteSource) =>

        println("byteSource: ", byteSource)

        val sumF: Future[Int] =
        // sum the numbers as they arrive so that we can accept any size of file
          byteSource
            .via(Framing.delimiter(ByteString("\n"), 1024))
            .mapConcat(_.utf8String.split(",").toVector)
            .map(_.toInt)
            .runFold(0) { (acc, n) => acc + n }

        onSuccess(sumF) { sum => complete(s"Sum: $sum") }
    }
  }

  def fus(implicit m: Materializer, e: ExecutionContextExecutor): Route = path("fileUploads") {
    fileUploadAll("csv") {
      byteSources =>

        println("byteSources: ", byteSources)

        // accumulate the sum of each file
        val sumF = byteSources.foldLeft(Future.successful(0)) {
          case (accF, (metaData, byteSource)) =>
            // sum the numbers as they arrive
            val intF = byteSource
              .via(Framing.delimiter(ByteString("\n"), 1024))
              .mapConcat(_.utf8String.split(",").toVector)
              .map(_.toInt)
              .runFold(0) { (acc, n) => acc + n }

            accF.flatMap(acc => intF.map(acc + _))
        }

        onSuccess(sumF) { sum => complete(s"Sum: $sum") }
    }
  }


  def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile(fileInfo.getFileName, ".tmp")

  def suf: Route = path("storeUploadedFile") {
    storeUploadedFile("csv", tempDestination) {
      case (metadata, file) =>
        println(s"meta: $metadata")
        println("do sth w/ the file and file metadata ...")
        file.delete()
        complete(StatusCodes.OK)
    }
  }

  def sufs(implicit m: Materializer, e: ExecutionContextExecutor): Route = path("storeUploadedFiles") {
    storeUploadedFiles("csv", tempDestination) { files =>
      val finalStatus = files.foldLeft(StatusCodes.OK) {
        case (status, (metadata, file)) =>
          println(s"meta: $metadata")
          println("do sth w/ the file and file metadata ...")
          file.delete()
          status
      }
      complete(finalStatus)
    }
  }

  val route: Route = extractRequestContext { ctx =>
    implicit val materializer: Materializer = ctx.materializer
    implicit val ect: ExecutionContextExecutor = materializer.executionContext

    concat(
      fd,
      fu,
      fus,
      suf,
      sufs
    )
  }
}


object DevFileUploadMain extends App {

  implicit val system: ActorSystem = ActorSystem("DevFileUpload")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val (host, port) = ("localhost", 2020)
  val bindFuture = Http().bindAndHandle(DevFileUpload.route, host, port)

  println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}

