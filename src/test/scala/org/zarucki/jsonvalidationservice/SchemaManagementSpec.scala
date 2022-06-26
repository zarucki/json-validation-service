package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.client.dsl.io._

class SchemaManagementSpec extends CatsEffectSuite {

  test("POST schema with not proper json returns error about invalid JSON") {
    assertIO(postJsonSchema("test-schema", "asdf").flatMap(_.as[String]),
      json"""{
        "action":  "uploadSchema",
        "id":      "config-schema",
        "status":  "error",
        "message": "Invalid JSON"
      }""".toString())
  }

  private[this] def postJsonSchema(id: String, body: String): IO[Response[IO]] = {
    val uri = Uri.unsafeFromString(s"/schema/$id")
    val postSchema = POST(body, uri)
    JsonValidationServiceRoutes.schemaManagementRoutes[IO]().orNotFound(postSchema)
  }
}