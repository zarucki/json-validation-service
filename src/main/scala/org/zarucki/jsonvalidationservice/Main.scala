package org.zarucki.jsonvalidationservice

import cats.effect.{ExitCode, IO, IOApp}
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceServer

object Main extends IOApp {
  def run(args: List[String]) =
    JsonValidationServiceServer.stream[IO].compile.drain.as(ExitCode.Success)
}
