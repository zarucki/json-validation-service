package org.zarucki.jsonvalidationservice.http

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class ActionResponse(action: String, id: String, status: Status, message: Option[String] = None)

object ActionResponse {
  implicit val actionResponseEncoder: Encoder[ActionResponse] = deriveEncoder[ActionResponse].mapJson(_.dropNullValues)
  implicit def actionResponseEntityEncoder[F[_]]: EntityEncoder[F, ActionResponse] = jsonEncoderOf

  object Actions {
    val uploadSchema = "uploadSchema"
    val validateDocument = "validateDocument"
  }
}
