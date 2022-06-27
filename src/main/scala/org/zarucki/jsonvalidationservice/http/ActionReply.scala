package org.zarucki.jsonvalidationservice.http

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class ActionReply(action: String, id: String, status: Status, message: Option[String] = None)

object ActionReply {
  implicit val actionReplyEncoder: Encoder[ActionReply] = deriveEncoder[ActionReply].mapJson(_.dropNullValues)
  implicit def actionReplyEntityEncoder[F[_]]: EntityEncoder[F, ActionReply] = jsonEncoderOf

  object Actions {
    val uploadSchema = "uploadSchema"
    val validateDocument = "validateDocument"
  }
}
