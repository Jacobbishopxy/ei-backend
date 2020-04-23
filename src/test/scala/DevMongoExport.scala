
import java.io.PrintWriter
import java.util.logging.Level

import scala.concurrent.{Future, Promise}

import com.mongodb.{ConnectionString, ReadPreference => JReadPreference}

import org.mongodb.scala._

/**
 * Created by Jacob Xie on 4/23/2020
 *
 *
 * An example program providing similar functionality as the `mongoexport` program
 *
 * As there is no core CSV library for Scala CSV export is an exercise left to the reader.
 *
 * Add mongo-scala-driver-alldep jar to your path or add to ./lib directory and then run as a shell program::
 *
 * {{{
 *  ./mongoexport.scala -u mongodb://localhost/test.testData > ./data/testData.json
 * }}}
 *
 * Alternatively, run the `main` method in an IDE and pass the arguments in.
 */
object DevMongoExport {

  val usage: String =
    """
      |Export MongoDB data to JSON files.
      |
      |Example:
      |  ./mongoexport.scala -u mongodb://localhost/test.testData > ./data/testData.json
      |
      |Options:
      |  --help                                produce help message
      |  --quiet                               silence all non error diagnostic messages
      |  -u [ --uri ] arg                      The connection URI - must contain a collection
      |                                        mongodb://[username:password@]host1[:port1][,host2[:port2]]/database.collection[?options]
      |                                        See: http://docs.mongodb.org/manual/reference/connection-string/
      |  -f [ --fields ] arg                   comma separated list of field names e.g. -f name,age
      |  -q [ --query ] arg                    query filter, as a JSON string, e.g. '{x:{$gt:1}}'
      |  -o [ --out ] arg                      output file; if not specified, stdout is used
      |  -k [ --slaveOk ] arg (=1)             use secondaries for export if available, default true
      |  --skip arg (=0)                       documents to skip, default 0
      |  --limit arg (=0)                      limit the numbers of documents
      |                                        returned, default all
      |  --sort arg                            sort order, as a JSON string, e.g.,
      |                                        '{x:1}'
              """.stripMargin


  /**
   * Exports JSON from the collection
   *
   * @param collection the collection to import into
   * @param output     the data source
   * @param options    the configuration options
   */
  def exportJson(collection: MongoCollection[Document], output: PrintWriter, options: Options): Future[Completed] = {
    val promise = Promise[Completed]()
    val cursor = collection.find(options.query)
    options.skip match {
      case None => cursor
      case Some(value) => cursor.skip(value)
    }
    options.limit match {
      case None => cursor
      case Some(value) => cursor.limit(value)
    }
    options.sort match {
      case None => cursor
      case Some(value) => cursor.sort(value)
    }

    output.write("")
    cursor.subscribe(
      (doc: Document) => output.println(doc.toJson()),
      (t: Throwable) => promise.failure(t),
      () => {
        output.close()
        promise.success(Completed())
      }
    )
    promise.future
  }

  /**
   * Recursively convert the args list into a Map of options
   *
   * @param map  - the initial option map
   * @param args - the args list
   * @return the parsed OptionMap
   */
  @scala.annotation.tailrec
  def parseArgs(map: Map[String, Any], args: List[String]): Map[String, Any] = {
    args match {
      case Nil => map
      case "--quiet" :: tail =>
        parseArgs(map ++ Map("quiet" -> true), tail)
      case "-u" :: value :: tail =>
        parseArgs(map ++ Map("uri" -> value), tail)
      case "--uri" :: value :: tail =>
        parseArgs(map ++ Map("uri" -> value), tail)
      case "-f" :: value :: tail =>
        parseArgs(map ++ Map("fields" -> value.split(",").toList), tail)
      case "--file" :: value :: tail =>
        parseArgs(map ++ Map("fields" -> value.split(",").toList), tail)
      case "-q" :: value :: tail =>
        parseArgs(map ++ Map("query" -> value), tail)
      case "--query" :: value :: tail =>
        parseArgs(map ++ Map("query" -> value), tail)
      case "-o" :: value :: tail =>
        parseArgs(map ++ Map("out" -> value), tail)
      case "--out" :: value :: tail =>
        parseArgs(map ++ Map("out" -> value), tail)
      case "-k" :: value :: tail =>
        parseArgs(map ++ Map("slaveOk" -> value), tail)
      case "--slaveOk" :: value :: tail =>
        parseArgs(map ++ Map("slaveOk" -> value), tail)
      case "--skip" :: value :: tail =>
        parseArgs(map ++ Map("skip" -> value), tail)
      case "--limit" :: value :: tail =>
        parseArgs(map ++ Map("limit" -> value), tail)
      case "--sort" :: value :: tail =>
        parseArgs(map ++ Map("sort" -> value), tail)
      case option :: tail =>
        Console.err.println("Unknown option " + option)
        Console.err.println(usage)
        sys.exit(1)
    }
  }

  /**
   * Convert the optionMap to an Options instance
   *
   * @param optionMap the parsed args options
   * @return Options instance
   */
  def getOptions(optionMap: Map[String, _]): Options = {
    val default = Options()
    val options = Options(
      quiet = optionMap.getOrElse("quiet", default.quiet).asInstanceOf[Boolean],
      uri = optionMap.get("uri") match {
        case None => default.uri
        case Some(value) => Some(value.asInstanceOf[String])
      },
      out = optionMap.get("out") match {
        case None => default.out
        case Some(value) => Some(value.asInstanceOf[String])
      },
      slaveOK = optionMap.getOrElse("slaveOK", default.slaveOK).asInstanceOf[Boolean],
      fields = optionMap.get("fields") match {
        case None => default.fields
        case Some(value) => Some(value.asInstanceOf[List[String]])
      },
      query = optionMap.get("query") match {
        case None => default.query
        case Some(value) => Document(value.asInstanceOf[String])
      },
      sort = optionMap.get("sort") match {
        case None => default.sort
        case Some(value) => Some(Document(value.asInstanceOf[String]))
      },
      skip = optionMap.get("skip") match {
        case None => default.skip
        case Some(value) => Some(value.asInstanceOf[Int])
      },
      limit = optionMap.get("limit") match {
        case None => default.limit
        case Some(value) => Some(value.asInstanceOf[Int])
      }
    )
    options
  }

  case class Options(quiet: Boolean = false,
                     uri: Option[String] = None,
                     out: Option[String] = None,
                     slaveOK: Boolean = true,
                     fields: Option[List[String]] = None,
                     query: Document = Document(),
                     sort: Option[Document] = None,
                     skip: Option[Int] = None,
                     limit: Option[Int] = None)

  def currentTime: Long = System.currentTimeMillis()

  /**
   * Shows a pinWheel in the console.err
   *
   * @param someFuture the future we are all waiting for
   */
  def showPinWheel(someFuture: Future[_]): Unit = {
    // Let the user know something is happening until futureOutput isCompleted
    val spinChars = List("|", "/", "-", "\\")
    while (!someFuture.isCompleted) {
      spinChars.foreach({
        case char =>
          Console.err.print(char)
          Thread sleep 200
          Console.err.print("\b")
      })
    }
    Console.err.println("")
  }

}

/**
 * The main export program
 * Outputs debug information to Console.err - as Console.out is probably redirected to a file
 *
 */
object DevMongoExportMain extends App {

  import DevMongoExport._


  if (args.length == 0 | args.contains("--help")) {
    Console.err.println(usage)
    sys.exit(1)
  }

  // Set the debug log level
  java.util.logging.Logger.getLogger("").getHandlers.foreach(h => h.setLevel(Level.WARNING))


  val optionMap = parseArgs(Map(), args.toList)
  val options = getOptions(optionMap)

  if (options.uri.isEmpty) {
    Console.err.println("Missing URI")
    Console.err.println(usage)
    sys.exit(1)
  }

  // Get URI
  val mongoClientURI = new ConnectionString(options.uri.get)
  if (Option(mongoClientURI.getCollection).isEmpty) {
    Console.err.println(s"Missing collection name in the URI eg:  mongodb://<hostInformation>/<database>.<collection>[?options]")
    Console.err.println(s"Current URI: $mongoClientURI")
    sys.exit(1)
  }

  // Get the collection
  val mongoClient = MongoClient(mongoClientURI.getConnectionString)
  val database = mongoClient.getDatabase(mongoClientURI.getDatabase)
  val readPreference: ReadPreference = if (options.slaveOK) {
    JReadPreference.secondaryPreferred()
  } else {
    JReadPreference.primaryPreferred()
  }
  val collection = database.getCollection(mongoClientURI.getCollection).withReadPreference(readPreference)

  // output
  val output: PrintWriter = options.out match {
    case None => new PrintWriter(System.out)
    case Some(fileName) => new PrintWriter(fileName)
  }


  // Export JSON
  if (!options.quiet) Console.err.println("Exporting...")
  val exporter = exportJson(collection, output, options)
  showPinWheel(exporter)
  val total = currentTime - executionStart
  if (!options.quiet) Console.err.println(s"Finished export: $total ms")
}

