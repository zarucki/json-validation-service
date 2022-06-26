package org.zarucki.jsonvalidationservice

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

object JsonValidationServiceServer {

  def stream[F[_]: Async]: Stream[F, Nothing] = {
    val helloWorldAlg = HelloWorld.impl[F]

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract segments not checked
    // in the underlying routes.
    val httpApp = (
      JsonValidationServiceRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        JsonValidationServiceRoutes.schemaManagementRoutes[F]()
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
