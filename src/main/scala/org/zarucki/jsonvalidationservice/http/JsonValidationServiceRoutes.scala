package org.zarucki.jsonvalidationservice.http

import cats.data.Validated
import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, HttpRoutes, MediaType}
import org.typelevel.log4cats.Logger
import org.zarucki.jsonvalidationservice.http.ActionResponse.Actions
import org.zarucki.jsonvalidationservice.storage.JsonStorage
import org.zarucki.jsonvalidationservice.validation.JsonValidator
import org.typelevel.log4cats.syntax._

object JsonValidationServiceRoutes {
  private val jsonMediaType = MediaType.unsafeParse("application/json")

  def schemaManagementRoutes[F[_] : Concurrent](jsonStorage: JsonStorage[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val schemaPath = Root / "schema"
    HttpRoutes.of[F] {
      case req@POST -> `schemaPath` / schemaId =>
        val action = Actions.uploadSchema
        req.attemptAs[Json].foldF(
          _ => BadRequest(ActionResponse(action, schemaId, Status.Error, "Invalid JSON".some)),
          json =>
            for {
              _ <- jsonStorage.upsert(schemaId, json)
              response <- Created(ActionResponse(action, schemaId, Status.Success))
            } yield response
        )
      case GET -> `schemaPath` / schemaId =>
        for {
          maybeJsonSchemaStream <- jsonStorage.getStream(schemaId)
          response <- maybeJsonSchemaStream.fold(NotFound()) { json =>
            Ok(json, `Content-Type`(jsonMediaType, Charset.`UTF-8`))
          }
        } yield response
    }
  }

  def jsonValidationRoutes[F[_] : Concurrent : Logger](jsonStorage: JsonStorage[F], jsonValidator: JsonValidator[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "validate" / schemaId =>
        for {
          maybeJsonSchemaStream <- jsonStorage.getStream(schemaId)
          response <- maybeJsonSchemaStream.fold {
            warn"Couldn't find schema with id $schemaId".flatMap(_ => NotFound())
          } { schemaJson =>
            val action = Actions.validateDocument
            jsonValidator.validateJsonAgainstSchema(json = req.body, schema = schemaJson).flatMap {
              case Validated.Valid(_) =>
                Ok(ActionResponse(action, schemaId, Status.Success))
              case Validated.Invalid(errors) =>
                Ok(ActionResponse(action, schemaId, Status.Error, errors.mkString_("; ").some))
            }
          }
        } yield response
    }
  }
}
