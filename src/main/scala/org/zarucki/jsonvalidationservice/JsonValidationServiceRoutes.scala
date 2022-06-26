package org.zarucki.jsonvalidationservice

import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.implicits._
import io.circe.Json
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.zarucki.jsonvalidationservice.ActionReply.Actions

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

  def schemaManagementRoutes[F[_]: Concurrent](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    val schemaPath = Root / "schema"
    HttpRoutes.of[F] {
      case req @ POST -> `schemaPath` / schemaId =>
        val action = Actions.uploadSchema
          req.attemptAs[Json].foldF(
          _ => BadRequest(ActionReply(action, schemaId, Status.Error, "Invalid JSON".some)),
          _ => Ok(ActionReply(action, schemaId, Status.Success))
        )
      case _ @ GET -> `schemaPath` / _ => NotFound()
    }
  }
}