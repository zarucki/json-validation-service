package org.zarucki.jsonvalidationservice.validation

import cats.data.{Validated, ValidatedNel}
import cats.effect.Async
import cats.syntax.all._
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.core.report.{ListProcessingReport, ProcessingMessage}
import com.github.fge.jsonschema.main.JsonSchemaFactory

import scala.jdk.CollectionConverters._

class JavaLibraryJsonValidator[F[_] : Async] extends JsonValidator[F] {
  private val objectMapper = new ObjectMapper()
    .setSerializationInclusion(Include.NON_NULL)
  private val validator = JsonSchemaFactory.byDefault().getValidator()

  override def validateJsonAgainstSchema(json: fs2.Stream[F, Byte], schema: fs2.Stream[F, Byte]): F[ValidatedNel[String, Unit]] = {
    val validate = for {
      schema <- bytesToJsonNode(schema)
      instance <- bytesToJsonNode(json)
    } yield {
      stripNulls(instance)
      validator.validate(schema, instance, true)
    }

    validate.compile.last.map {
      case Some(report: ListProcessingReport) =>
        if (report.isSuccess) {
          Validated.Valid(())
        } else {
          report.iterator().asScala
            .map(processingMessageToShortError)
            .map(_.invalidNel[Unit])
            .toList.combineAll
        }
      case _ => Validated.invalidNel("No report to parse.")
    }
  }

  private def processingMessageToShortError(pm: ProcessingMessage) = {
    val pmJson = pm.asJson()
    val maybePointer = for {
      instance <- Option(pmJson.get("instance"))
      pointer <- Option(instance.get("pointer"))
    } yield pointer.asText
    s"${maybePointer.getOrElse("")} : ${pm.getMessage}"
  }

  private def bytesToJsonNode(jsonStream: fs2.Stream[F, Byte]) =
    jsonStream.through(fs2.io.toInputStream).map(objectMapper.readTree)

  // TODO: probably there's better way to do this
  private def stripNulls(jsonNode: JsonNode): Unit = {
    val it = jsonNode.iterator()
    while (it.hasNext()) {
      val child = it.next()

      if (child.isNull) it.remove()
      else stripNulls(child)
    }
  }
}
