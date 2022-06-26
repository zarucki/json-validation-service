package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._

class SchemaManagementSpec extends CatsEffectSuite {
  val testSchemaId = "test-schema"
  val ValidJson = "{}"
  val InvalidJson = "asdf"

  test("POST schema with valid json returns 200 OK") {
    assertIO(postJsonSchema(testSchemaId, ValidJson).map(_.status), Status.Ok)
  }

  // TODO: what if already exists?
  test("POST schema with valid json returns successful response") {
    assertIO(postJsonSchema(testSchemaId, ValidJson).flatMap(responseAsJson),
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
    assertIO(postJsonSchema(testSchemaId, InvalidJson).flatMap(responseAsJson),
      Right(json"""{
        "action":  "uploadSchema",
        "id":      $testSchemaId,
        "status":  "error",
        "message": "Invalid JSON"
      }"""))
  }

  test("GET schema with unknown schema id should return 404 Not Found.") {
    assertIO(getJsonSchema("unknown-id").map(_.status).value, Some(Status.NotFound))
  }

  test("GET request to unknown path should return None") {
    assertIO(JsonValidationServiceRoutes.schemaManagementRoutes[IO]().apply(GET(uri"/non-existing-path")).value, None)
  }

  private[this] def postJsonSchema(id: String, body: String) = {
    val postSchema = POST(body, uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO]().orNotFound(postSchema)
  }

  private[this] def getJsonSchema(id: String) = {
    val getSchema = GET(uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO]().apply(getSchema)
  }

  private[this] def uriForSchema(id: String) = Uri.unsafeFromString(s"/schema/$id")

  private[this] def responseAsJson(response: Response[IO]) = response.as[String].map(parse)

  private[this] def parse(str: String) = io.circe.jawn.parse(str)
}