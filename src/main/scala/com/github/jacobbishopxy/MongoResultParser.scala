package com.github.jacobbishopxy


import com.github.jacobbishopxy.CatsEnriched.ValidatedNelType
import org.mongodb.scala.BulkWriteResult
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import spray.json._
import cats.data._

import scala.jdk.CollectionConverters._

/**
 * Created by Jacob Xie on 7/29/2020
 */
object MongoResultParser extends DefaultJsonProtocol {

  implicit object JsonWriterBulkWriteResult extends JsonWriter[BulkWriteResult] {
    override def write(obj: BulkWriteResult): JsValue =
      JsObject(
        "insertedCount" -> JsNumber(obj.getInsertedCount),
        "matchedCount" -> JsNumber(obj.getMatchedCount),
        "removedCount" -> JsNumber(obj.getDeletedCount),
        "modifiedCount" -> JsNumber(obj.getModifiedCount),
        "upserts" -> JsArray(
          obj.getUpserts.asScala.toVector.map(i =>
            JsObject(
              "id" -> JsString(i.getId.toString),
              "index" -> JsNumber(i.getIndex)
            ))
        ),
      )
  }

  implicit object JsonWriterDeleteResult extends JsonWriter[DeleteResult] {
    override def write(obj: DeleteResult): JsValue =
      JsObject(
        "deletedCount" -> JsNumber(obj.getDeletedCount)
      )
  }

  implicit object JsonWriterOptionDeleteResult extends JsonWriter[Option[DeleteResult]] {
    override def write(obj: Option[DeleteResult]): JsValue = obj match {
      case None => JsObject("deletedCount" -> JsNumber(0))
      case Some(v) => JsonWriterDeleteResult.write(v)
    }
  }

  implicit object JsonWriterUpdateResult extends JsonWriter[UpdateResult] {
    override def write(obj: UpdateResult): JsValue =
      JsObject(
        "matchedCount" -> JsNumber(obj.getMatchedCount),
        "modifiedCount" -> JsNumber(obj.getModifiedCount),
        "upsertedId" -> JsString(Option(obj.getUpsertedId).fold("null")(_.toString))
      )
  }

  implicit object JsonWriterValidatedNelJson extends JsonWriter[ValidatedNelType[JsValue]] {
    override def write(obj: ValidatedNelType[JsValue]): JsValue = obj match {
      case Validated.Invalid(v) => JsString(v.toString)
      case Validated.Valid(v) => v
    }
  }

}
