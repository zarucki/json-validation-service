package org.zarucki.jsonvalidationservice

import cats.effect.{ExitCode, IO, IOApp}
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceServer
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceServer.JsonValidationServiceServerConf

object Main extends IOApp {
  def run(args: List[String]) = {
    import pureconfig._
    import pureconfig.generic.auto._

    ConfigSource.default.load[JsonValidationServiceServerConf] match {
      case Left(value) =>
        print(value.prettyPrint())
        IO(ExitCode.Error)
      case Right(conf) =>
        JsonValidationServiceServer.stream[IO](conf).compile.drain.as(ExitCode.Success)
    }
  }
}
