package com.github.jacobbishopxy

import cats._
import cats.data._
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by Jacob Xie on 7/29/2020
 */
object CatsEnriched {

  type ValidatedNelType[A] = ValidatedNel[Throwable, A]

  implicit class EnrichedFuture[A](future: Future[A])(implicit ec: ExecutionContext) {
    def toValidatedNel: Future[ValidatedNelType[A]] =
      future.map(Validated.valid).recover(Validated.invalidNel(_))
  }

}
