
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters
import org.mongodb.scala.gridfs._
import org.mongodb.scala._

import DevMongoGridFSHelpers._

/**
 * Created by jacob on 7/5/2020
 *
 * https://mongodb.github.io/mongo-java-driver/4.1/driver-scala/tutorials/gridfs/
 */
object DevMongoGridFS extends App with DevMongoRepo {

  override val database: MongoDatabase = mongoClient.getDatabase("dev2")

  /*
  Create a GridFS Bucket

  GridFS stores files in two collections: a `chunks` collection stores the file chunks, and a `files` collection
  stores file metadata. The two collections are in a common bucket and the collection names are prefixed with
  the bucket name
   */
  val gridFSBucket: GridFSBucket = GridFSBucket(database, "files")

  /*
  Upload to GridFS

  The `GridFSBucket.uploadFromObservable` methods read the contents of a `Observable[ByteBuffer]` and save it to
  the `GridFSBucket`.

  You can use the `GridFSUploadOptions` to configure the chunk size or include additional metadata.
   */

  // Get the input stream
  val observableToUploadFrom: Observable[ByteBuffer] = Observable(
    Seq(ByteBuffer.wrap("MongoDB Tutorial..".getBytes(StandardCharsets.UTF_8)))
  )

  // Create some custom options
  val options: GridFSUploadOptions = new GridFSUploadOptions()
    .chunkSizeBytes(358400)
    .metadata(Document("type" -> "presentation"))

  val fileId: ObjectId = gridFSBucket
    .uploadFromObservable("mongodb-tutorial", observableToUploadFrom, options)
    .headResult()

  /*
  Find Files Stored in GridFS

  To find the files stored in the `GridFSBucket` use the `find` method.
   */

  // print out the filename of each file stored:
  gridFSBucket
    .find()
    .results()
    .foreach(f => println(s" - ${f.getFilename}"))

  // provide a custom filter to limit the results returned
  gridFSBucket
    .find(Filters.equal("metadata.contentType", "image/png"))
    .results()
    .foreach(f => println(s" > ${f.getFilename}"))

  /*
  Download from GridFS

  The `downloadToObservable` methods return a `Observable[ByteBuffer]` that reads the contents from MongoDB.
   */

  // download a file by its file `_id`
  val downloadById: Seq[ByteBuffer] = gridFSBucket
    .downloadToObservable(fileId).results()

  // download a file by its filename
  val downloadOptions: GridFSDownloadOptions = new GridFSDownloadOptions().revision(0)
  val downloadByName: Seq[ByteBuffer] = gridFSBucket
    .downloadToObservable("mongodb-tutorial", downloadOptions).results()

  /*
  Rename files
   */

  gridFSBucket.rename(fileId, "mongodbTutorial").printResults()

  /*
  Delete files
   */

  gridFSBucket.delete(fileId).printResults()


}
