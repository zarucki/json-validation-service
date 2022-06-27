package org.zarucki.jsonvalidationservice

import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.Json
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, HttpRoutes, MediaType}
import org.zarucki.jsonvalidationservice.ActionReply.Actions
import org.zarucki.jsonvalidationservice.storage.JsonStorage

object JsonValidationServiceRoutes {
  private val jsonMediaType = MediaType.unsafeParse("application/json")

  def schemaManagementRoutes[F[_]: Concurrent](jsonStorage: JsonStorage[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    val schemaPath = Root / "schema"
    HttpRoutes.of[F] {
      case req @ POST -> `schemaPath` / schemaId =>
        val action = Actions.uploadSchema
          req.attemptAs[Json].foldF(
          _ => BadRequest(ActionReply(action, schemaId, Status.Error, "Invalid JSON".some)),
          json =>
              for {
                _ <- jsonStorage.upsert(schemaId, json)
                response <- Created(ActionReply(action, schemaId, Status.Success))
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

  def jsonValidationRoutes[F[_] : Concurrent](jsonStorage: JsonStorage[F]): HttpRoutes[F] = {

    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpRoutes.of[F] {
      case _ @ POST -> Root / "validate" / schemaId =>
        for {
          maybeJsonSchemaStream <- jsonStorage.getStream(schemaId)
          response <- maybeJsonSchemaStream.fold(NotFound()) { _ =>
            Ok(ActionReply(Actions.validateDocument, schemaId, Status.Success))
          }
        } yield response
    }
  }
}