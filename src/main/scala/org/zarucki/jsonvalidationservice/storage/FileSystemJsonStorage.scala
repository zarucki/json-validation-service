package org.zarucki.jsonvalidationservice.storage
import cats.Applicative
import cats.effect.kernel.Concurrent
import cats.syntax.all._
import fs2.io.file.PosixPermission._
import fs2.io.file.{Files, Flags, Path, PosixPermissions}
import io.circe.Json
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

import java.nio.charset.StandardCharsets

class FileSystemJsonStorage[F[_] : Files : Concurrent : Logger](rootPath: Path) extends JsonStorage[F] {
  private val onlyOwnerAccessPosix = PosixPermissions(OwnerRead, OwnerWrite, OwnerExecute)

  override def upsert(id: String, json: Json): F[Unit] = {
    val jsonBytes = jsonStringWithoutNulls(json).getBytes(StandardCharsets.UTF_8)
    for {
      _ <- Files[F].createDirectories(pathForSchemaDirectory(id), onlyOwnerAccessPosix.some)
      schemaPath = pathForSchema(id)
      _ <- info"Writing ${jsonBytes.size} bytes to '$schemaPath' under root path '$rootPath'."
      _ <- fs2.Stream
        .emits(jsonBytes)
        .through(Files[F].writeAll(schemaPath, Flags.Write))
        .compile
        .drain
    } yield ()
  }

  override def getStream(id: String): F[Option[fs2.Stream[F, Byte]]] = {
    val schemaPath = pathForSchema(id)
    for {
      fileExists <- Files[F].exists(schemaPath)
      _ <- if (!fileExists) warn"Got request for non-existing id: $id" else Applicative[F].unit
      response = if (!fileExists) None else Files[F].readAll(schemaPath).some
    } yield response
  }

  private def pathForSchemaDirectory(id: String) = rootPath / id

  private def pathForSchema(id: String) = rootPath / id / "schema"

  private def jsonStringWithoutNulls(json: Json) = json.deepDropNullValues.toString()
}
