package org.zarucki.jsonvalidationservice.storage

import io.circe.Json

trait JsonStorage[F[_]] {
  def upsert(id: String, json: Json): F[Unit]
  def getStream(id: String): F[Option[fs2.Stream[F, Byte]]]
}
