package org.zarucki.jsonvalidationservice

import cats.effect.{ExitCode, IO, IOApp, Sync}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceServer
import org.zarucki.jsonvalidationservice.http.JsonValidationServiceServer.JsonValidationServiceServerConf

object Main extends IOApp {
  def run(args: List[String]) = {
    import pureconfig._
    import pureconfig.generic.auto._

    implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

    ConfigSource.default.load[JsonValidationServiceServerConf] match {
      case Left(value) =>
        Logger[IO].error(s"Couldn't load conifg. ${value.prettyPrint()}").as(ExitCode.Error)
      case Right(conf) =>
        for {
          _ <- Logger[IO].info(s"Loaded config: $conf.")
          _ <- JsonValidationServiceServer.stream[IO](conf).compile.drain
        } yield ExitCode.Success
    }
  }
}
