package org.zarucki.jsonvalidationservice.http

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import org.zarucki.jsonvalidationservice.storage.FileSystemJsonStorage
import org.zarucki.jsonvalidationservice.validation.JavaLibraryJsonValidator

object JsonValidationServiceServer {

  def stream[F[_] : Async]: Stream[F, Nothing] = {
    val path = java.nio.file.Path.of("schema-root")

    val jsonStorage = new FileSystemJsonStorage[F](fs2.io.file.Path.fromNioPath(path)) // TODO: make this path configurable
    val jsonValidator = new JavaLibraryJsonValidator[F]()

    val httpApp = (
      JsonValidationServiceRoutes.schemaManagementRoutes[F](jsonStorage) <+>
        JsonValidationServiceRoutes.jsonValidationRoutes[F](jsonStorage, jsonValidator)
      ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    for {
      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
          Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
