package org.zarucki.jsonvalidationservice

import cats.effect.{Concurrent, IO}
import fs2.io.file.Files
import munit.CatsEffectSuite
import org.http4s.{Response, Status, Uri}
import org.http4s.dsl.io.{GET, POST}
import org.http4s.client.dsl.io._
import org.zarucki.jsonvalidationservice.storage.FileSystemJsonStorage

abstract class BaseSchemaSpec extends CatsEffectSuite {
  protected val testSchemaId = "test-schema"
  protected val path = java.nio.file.Path.of("test-schema-root")
  protected def jsonStorage[F[_] : Files : Concurrent] = new FileSystemJsonStorage[F](fs2.io.file.Path.fromNioPath(path))

  protected def postJsonSchema(id: String, body: String) = {
    val postSchema = POST(body, uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO](jsonStorage).orNotFound(postSchema)
  }

  protected def getJsonSchema(id: String) = {
    val getSchema = GET(uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO](jsonStorage).apply(getSchema)
  }

  protected def postAndGetTheSameSchema(id: String, schema: String) = {
    for {
      _ <- assertIO(postJsonSchema(id, schema).map(_.status), Status.Created)
      getResponse <- getJsonSchema(id).value
    } yield getResponse.getOrElse(Response.notFound)
  }

  protected def uriForSchema(id: String) = Uri.unsafeFromString(s"/schema/$id")

  protected def responseAsJson(response: Response[IO]) = response.as[String].map(parse)

  protected def parse(str: String) = io.circe.jawn.parse(str)
}
