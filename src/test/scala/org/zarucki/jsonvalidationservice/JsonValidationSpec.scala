package org.zarucki.jsonvalidationservice

import cats.effect.{Async, IO}
import io.circe.literal._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.zarucki.jsonvalidationservice.http.ActionResponse.Actions
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceRoutes
import org.zarucki.jsonvalidationservice.validation.JavaLibraryJsonValidator

import scala.io.Source

class JsonValidationSpec extends BaseSchemaSpec {

  test("POST request with config id that is not known") {
     assertIO(postValidate("test-config-unknown", json"""{}""".toString()).map(_.status), Status.NotFound)
  }

  test("POST request with config that is known and json that is valid") {
    for {
      _ <- assertIO(postJsonSchema(testSchemaId, exampleSchema).map(_.status), Status.Created)
      validateResponse = postValidate(testSchemaId, exampleValidObject)
      _ <- assertIO(validateResponse.map(_.status), Status.Ok)
      _ <- assertIO(validateResponse.flatMap(responseAsJson), Right(
        json"""{
          "action": ${Actions.validateDocument},
          "id": $testSchemaId,
          "status": "success"
        }"""))
    } yield ()
  }

  test("POST request with config that is known and json that is invalid") {
    for {
      _ <- assertIO(postJsonSchema(testSchemaId, exampleSchema).map(_.status), Status.Created)
      validateResponse = postValidate(testSchemaId, exampleInvalidObject)
      _ <- assertIO(validateResponse.map(_.status), Status.Ok) // TODO: should the status be ok?
      _ <- assertIO(validateResponse.flatMap(responseAsJson), Right(
        json"""{
          "action": ${Actions.validateDocument},
          "id": $testSchemaId,
          "status": "error",
          "message": "/chunks/size : instance type (string) does not match any allowed primitive type (allowed: [\"integer\"]); /timeout : numeric instance is greater than the required maximum (maximum: 32767, found: 50000)"
        }"""))
    } yield ()
  }

  private[this] def postValidate(id: String, body: String) = {
    val postSchema = POST(body, uriForValidation(id))
    JsonValidationServiceRoutes.jsonValidationRoutes[IO](jsonStorage, jsonValidator).orNotFound(postSchema)
  }

  private[this] def uriForValidation(id: String) = Uri.unsafeFromString(s"/validate/$id")

  private[this] def jsonValidator[F[_] : Async] = new JavaLibraryJsonValidator[F]
  private[this] lazy val exampleSchema = Source.fromResource("example-schema.json").mkString
  private[this] lazy val exampleValidObject = Source.fromResource("example-object-valid.json").mkString
  private[this] lazy val exampleInvalidObject = Source.fromResource("example-object-invalid.json").mkString
}
