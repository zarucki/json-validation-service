package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.zarucki.jsonvalidationservice.ActionReply.Actions

class SchemaManagementSpec extends BaseSchemaSpec {
  test("POST schema with valid json returns successful response and 201 created") {
    val response = postJsonSchema(testSchemaId, json"{}".toString())
    for {
      _ <- assertIO(response.map(_.status), Status.Created)
      _ <- assertIO(response.flatMap(responseAsJson),
        Right(
          json"""{
        "action":  ${Actions.uploadSchema},
        "id":      $testSchemaId,
        "status":  "success"
      }"""))
    } yield ()
  }

  test("POST schema with not proper json returns error response with information about invalid JSON and 400 Bad Request") {
    val InvalidJson = "asdf"
    val response = postJsonSchema(testSchemaId, InvalidJson)
    for {
      _ <- assertIO(response.map(_.status), Status.BadRequest)
      _ <- assertIO(response.flatMap(responseAsJson),
      Right(json"""{
        "action":  ${Actions.uploadSchema},
        "id":      $testSchemaId,
        "status":  "error",
        "message": "Invalid JSON"
      }"""))
    } yield ()
  }

  test("GET schema with unknown schema id should return 404 Not Found.") {
    assertIO(getJsonSchema("unknown-id").map(_.status).value, Some(Status.NotFound))
  }

  test("GET request to known schema id should return that schema and 200 OK") {
    val response = postAndGetTheSameSchema(testSchemaId, json"""{"schema": true}""".toString())
    for {
      _ <- assertIO(response.map(_.status), Status.Ok)
      _ <- assertIO(response.flatMap(responseAsJson),
      Right(
      json"""{
          "schema": true
        }""")
      )
    } yield ()
  }

  test("GET request to known schema id should return content type") {
    assertIO(
      postAndGetTheSameSchema(testSchemaId, json"""{}""".toString()).map(_.contentType),
      Some(`Content-Type`(MediaType.unsafeParse("application/json"), Charset.`UTF-8`))
    )
  }

  test("GET request to unknown path should return None") {
    assertIO(
      JsonValidationServiceRoutes.schemaManagementRoutes[IO](jsonStorage).apply(GET(uri"/non-existing-path")).value,
      None
    )
  }

  test("POST to existing schema overwrites it.") {
    for {
      _ <- assertIO(
        postAndGetTheSameSchema(testSchemaId, json"""{"schema": true}""".toString()).flatMap(responseAsJson),
        Right(
          json"""{
          "schema": true
        }""")
      )
      _ <- assertIO(
        postAndGetTheSameSchema(testSchemaId, json"""{"schema": false}""".toString()).flatMap(responseAsJson),
        Right(
          json"""{
          "schema": false
        }""")
      )
    } yield ()
  }
}