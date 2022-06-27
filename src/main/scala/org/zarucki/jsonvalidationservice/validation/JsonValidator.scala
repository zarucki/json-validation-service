package org.zarucki.jsonvalidationservice.validation

import cats.data.Validated

trait JsonValidator[F[_]] {
  def validateJsonAgainstSchema(json: fs2.Stream[F, Byte], schema: fs2.Stream[F, Byte]): F[Validated[String, Unit]]
}
