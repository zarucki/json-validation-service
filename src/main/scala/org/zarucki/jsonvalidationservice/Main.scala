package org.zarucki.jsonvalidationservice

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    JsonvalidationserviceServer.stream[IO].compile.drain.as(ExitCode.Success)
}
