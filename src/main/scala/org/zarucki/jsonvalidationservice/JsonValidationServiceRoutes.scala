package org.zarucki.jsonvalidationservice

import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.implicits._
import fs2.io.file.Files
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.zarucki.jsonvalidationservice.ActionReply.Actions
import org.zarucki.jsonvalidationservice.storage.FileSystemJsonStorage

object JsonValidationServiceRoutes {

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def schemaManagementRoutes[F[_]: Files : Concurrent](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    val path = java.nio.file.Path.of("schema-root")
    val jsonStorage = new FileSystemJsonStorage[F](fs2.io.file.Path.fromNioPath(path))

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
        // TODO: need to set proper content type
        for {
          maybeJsonStream <- jsonStorage.getStream(schemaId)
          response <- maybeJsonStream.fold(NotFound())(json => Ok(json))
        } yield response
    }
  }
}