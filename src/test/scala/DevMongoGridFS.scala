
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths, StandardOpenOption}

import scala.util.Success

import org.bson.types.ObjectId

import org.mongodb.scala.model.Filters
import org.mongodb.scala.gridfs._
import org.mongodb.scala.gridfs.helpers.AsynchronousChannelHelper.channelToOutputStream
import org.mongodb.scala.gridfs.helpers.AsyncStreamHelper.toAsyncInputStream
import org.mongodb.scala._
import DevMongoGridFSHelpers._

/**
 * Created by jacob on 7/5/2020
 *
 * https://github.com/mongodb/mongo-scala-driver/blob/master/examples/src/test/scala/tour/GridFSTour.scala
 */
object DevMongoGridFS extends App with DevMongoRepo {

  override val database: MongoDatabase = mongoClient.getDatabase("dev2")

  val gridFSBucket = GridFSBucket(database)

  // Get the input stream
  val streamToUploadFrom = toAsyncInputStream("MongoDB Tutorial...".getBytes(StandardCharsets.UTF_8))

  // Create some custom options
  val options = new GridFSUploadOptions()
    .chunkSizeBytes(1024 * 1204)
    .metadata(Document("type" -> "presentation"))

  val fileId = gridFSBucket
    .uploadFromStream("mongodb-tutorial", streamToUploadFrom, options)
    .headResult()
  streamToUploadFrom.close().headResult()

  /*
  OpenUploadStream Example
   */
  // Get some data to write
  val data = ByteBuffer.wrap("Data to upload into GridFS".getBytes(StandardCharsets.UTF_8))

  val uploadStream = gridFSBucket.openUploadStream("sampleData")
  uploadStream.write(data).headResult()
  uploadStream.close().headResult()

  /*
  Find documents
   */
  println("File names:")
  gridFSBucket
    .find()
    .results()
    .foreach(f => println(s" - ${f.getFilename}"))

  /*
  Find documents with a filter
   */
  gridFSBucket
    .find(Filters.equal("metadata.contentType", "image/png"))
    .results()
    .foreach(f => println(s" > ${f.getFilename}"))

  /*
  DownloadToStream
   */
  val outputPath = Paths.get("./tmp/mongodb-tutorial.txt")
  var streamToDownloadTo = AsynchronousFileChannel
    .open(
      outputPath,
      StandardOpenOption.CREATE_NEW,
      StandardOpenOption.WRITE,
      StandardOpenOption.DELETE_ON_CLOSE
    )
  gridFSBucket
    .downloadToStream(fileId, channelToOutputStream(streamToDownloadTo))
    .headResult()
  streamToDownloadTo.close()

  /*
  DownloadToStream by name
   */
  streamToDownloadTo = AsynchronousFileChannel
    .open(
      outputPath,
      StandardOpenOption.CREATE_NEW,
      StandardOpenOption.WRITE,
      StandardOpenOption.DELETE_ON_CLOSE
    )
  val downloadOptions = new GridFSDownloadOptions().revision(0)
  gridFSBucket
    .downloadToStream("mongodb-tutorial", channelToOutputStream(streamToDownloadTo), downloadOptions)
    .headResult()
  streamToDownloadTo.close()

  /*
  OpenDownloadStream
   */
  val dstByteBuffer = ByteBuffer.allocate(1024 * 1024)
  val downloadStream = gridFSBucket.openDownloadStream(fileId)
  downloadStream
    .read(dstByteBuffer)
    .map(result => {
      dstByteBuffer.flip()
      val bytes = new Array[Byte](result)
      dstByteBuffer.get(bytes)
      println(new String(bytes, StandardCharsets.UTF_8))
    })
    .headResult()

  /*
  OpenDownloadStream by name
   */
  println("By name")
  dstByteBuffer.clear()
  val downloadStreamByName = gridFSBucket.openDownloadStream("sampleData")
  downloadStreamByName
    .read(dstByteBuffer)
    .map(result => {
      dstByteBuffer.flip()
      val bytes = new Array[Byte](result)
      dstByteBuffer.get(bytes)
      println(new String(bytes, StandardCharsets.UTF_8))
    })
    .headResult()

  /*
  Rename
   */
  gridFSBucket.rename(fileId, "mongodbTutorial").andThen({
    case Success(r) => println("renamed")
  }).results()
  println("renamed")

  /*
  Delete
   */
  gridFSBucket.delete(fileId).results()
  println("deleted")

  // Final cleanup
  database.drop().results()
  println("Finished")
}
