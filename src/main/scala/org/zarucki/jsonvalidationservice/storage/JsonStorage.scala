package org.zarucki.jsonvalidationservice.storage

import io.circe.Json

trait JsonStorage[F[_]] {
  def upsert(id: String, json: Json): F[Unit]
  def get(id: String): F[Option[Json]]
}
