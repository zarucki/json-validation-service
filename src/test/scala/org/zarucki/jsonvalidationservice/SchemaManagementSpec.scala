package org.zarucki.jsonvalidationservice

import cats.effect.{Concurrent, IO}
import fs2.io.file.Files
import io.circe.literal._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.zarucki.jsonvalidationservice.storage.FileSystemJsonStorage

class SchemaManagementSpec extends CatsEffectSuite {
  private[this] val testSchemaId = "test-schema"
  private[this] val path = java.nio.file.Path.of("test-schema-root")
  private[this] def jsonStorage[F[_] : Files : Concurrent] = new FileSystemJsonStorage[F](fs2.io.file.Path.fromNioPath(path))

  test("POST schema with valid json returns successful response and 201 created") {
    val response = postJsonSchema(testSchemaId, json"{}".toString())
    for {
      _ <- assertIO(response.map(_.status), Status.Created)
      _ <- assertIO(response.flatMap(responseAsJson),
        Right(
          json"""{
        "action":  "uploadSchema",
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
        "action":  "uploadSchema",
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
    val response = postAndGetTheSameSchema(json"""{"schema": true}""".toString())
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
      postAndGetTheSameSchema(json"""{}""".toString()).map(_.contentType),
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
      _ <- assertIO(postAndGetTheSameSchema(json"""{"schema": true}""".toString()).flatMap(responseAsJson),
        Right(
          json"""{
          "schema": true
        }""")
      )
      _ <- assertIO(postAndGetTheSameSchema(json"""{"schema": false}""".toString()).flatMap(responseAsJson),
        Right(
          json"""{
          "schema": false
        }""")
      )
    } yield ()
  }

  private[this] def postJsonSchema(id: String, body: String) = {
    val postSchema = POST(body, uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO](jsonStorage).orNotFound(postSchema)
  }

  private[this] def getJsonSchema(id: String) = {
    val getSchema = GET(uriForSchema(id))
    JsonValidationServiceRoutes.schemaManagementRoutes[IO](jsonStorage).apply(getSchema)
  }

  private[this] def postAndGetTheSameSchema(schema: String) = {
    for {
      _ <- assertIO(postJsonSchema(testSchemaId, schema).map(_.status), Status.Created)
      getResponse <- getJsonSchema(testSchemaId).value
    } yield getResponse.getOrElse(Response.notFound)
  }

  private[this] def uriForSchema(id: String) = Uri.unsafeFromString(s"/schema/$id")

  private[this] def responseAsJson(response: Response[IO]) = response.as[String].map(parse)

  private[this] def parse(str: String) = io.circe.jawn.parse(str)
}