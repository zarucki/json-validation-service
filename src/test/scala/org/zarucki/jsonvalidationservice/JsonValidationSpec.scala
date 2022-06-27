package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.zarucki.jsonvalidationservice.ActionReply.Actions

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
          "status": "error"
        }"""))
    } yield ()
  }

  private[this] def postValidate(id: String, body: String) = {
    val postSchema = POST(body, uriForValidation(id))
    JsonValidationServiceRoutes.jsonValidationRoutes[IO](jsonStorage).orNotFound(postSchema)
  }

  private[this] def uriForValidation(id: String) = Uri.unsafeFromString(s"/validate/$id")

  private[this] lazy val exampleSchema = Source.fromResource("example-schema.json").mkString
  private[this] lazy val exampleValidObject = Source.fromResource("example-object-valid.json").mkString
  private[this] lazy val exampleInvalidObject = Source.fromResource("example-object-valid.json").mkString
}
