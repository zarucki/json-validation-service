package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.client.dsl.io._

class SchemaManagementSpec extends CatsEffectSuite {
  val testSchemaId = "test-schema"
  val ValidJson = "{}"
  val InvalidJson = "asdf"

  test("POST schema with valid json returns 200 OK") {
    assertIO(postJsonSchema(testSchemaId, ValidJson).map(_.status), Status.Ok)
  }

  // TODO: what if already exists?
  test("POST schema with valid json returns successful response") {
    assertIO(postJsonSchema(testSchemaId, ValidJson).flatMap(_.as[String].map(parse)),
      Right(json"""{
        "action":  "uploadSchema",
        "id":      $testSchemaId,
        "status":  "success"
      }"""))
  }

  test("POST schema with not proper json returns 400 Bad Request") {
    assertIO(postJsonSchema(testSchemaId, InvalidJson).map(_.status), Status.BadRequest)
  }

  test("POST schema with not proper json returns error response with information about invalid JSON") {
    assertIO(postJsonSchema(testSchemaId, InvalidJson).flatMap(_.as[String].map(parse)),
      Right(json"""{
        "action":  "uploadSchema",
        "id":      $testSchemaId,
        "status":  "error",
        "message": "Invalid JSON"
      }"""))
  }

  private[this] def postJsonSchema(id: String, body: String): IO[Response[IO]] = {
    val uri = Uri.unsafeFromString(s"/schema/$id")
    val postSchema = POST(body, uri)
    JsonValidationServiceRoutes.schemaManagementRoutes[IO]().orNotFound(postSchema)
  }

  private[this] def parse(str: String) = io.circe.jawn.parse(str)
}