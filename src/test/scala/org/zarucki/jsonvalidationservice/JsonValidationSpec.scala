package org.zarucki.jsonvalidationservice

import cats.effect.IO
import io.circe.literal._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.implicits._

class JsonValidationSpec extends BaseSchemaSpec {

  test("POST request with config id that is not known") {
     assertIO(postValidate("test-config-unknown", json"""{}""".toString()).map(_.status), Status.NotFound)
  }

  test("POST request with config that is known and json that is valid") {
    for {
      _ <- assertIO(postJsonSchema(testSchemaId, json"""{}""".toString()).map(_.status), Status.Created)
      validateResponse = postValidate(testSchemaId, json"""{}""".toString())
      _ <- assertIO(validateResponse.map(_.status), Status.Ok)
      _ <- assertIO(validateResponse.flatMap(responseAsJson), Right(
        json"""{
          "action": "validateDocument",
          "id": "test-config",
          "status": "success"
        }"""))
    } yield ()
  }

  protected def postValidate(id: String, body: String) = {
    val postSchema = POST(body, uriForValidation(id))
    JsonValidationServiceRoutes.jsonValidationRoutes[IO](jsonStorage).orNotFound(postSchema)
  }

  protected def uriForValidation(id: String) = Uri.unsafeFromString(s"/validate/$id")
}
