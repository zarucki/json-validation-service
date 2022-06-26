package org.zarucki.jsonvalidationservice

import io.circe.{Encoder, Json}

sealed trait Status

object Status {
  case object Success extends Status
  case object Error extends Status

  implicit val statusEncoder: Encoder[Status] = (a: Status) => Json.fromString(a.toString.toLowerCase())
}
