package de.lhns.httpproblem.tapir

import de.lhns.httpproblem.HttpProblem
import sttp.model.{MediaType, StatusCode}
import sttp.tapir.EndpointIO.Example
import sttp.tapir._
import sttp.tapir.json.circe.circeCodec

import java.net.URI

object json {
  implicit val httpProblemSchema: Schema[HttpProblem] = {
    object Surrogate {
      case class HttpProblem(
          `type`: Option[URI],
          status: Option[Int],
          title: Option[String],
          detail: Option[String],
          instance: Option[String]
      )
    }
    implicit val uriSchema: Schema[URI] = Schema.string[URI].format("uri")
    Schema.derived[Surrogate.HttpProblem].as[HttpProblem]
  }

  private def httpProblemSummary(tpe: Option[URI], status: Option[Int]): String =
    s"Problem Details${if (tpe.isDefined) s" with type ${tpe.get}"
      else if (status.isDefined) s" with status code ${status.get}"
      else ""}"

  def example(problem: HttpProblem): Example[HttpProblem] = Example.of(
    value = problem,
    name = problem.tpe.map(_.toString),
    summary = Some(httpProblemSummary(problem.tpe, problem.status))
  )

  val ApplicationProblemJsonMediaType: MediaType = MediaType("application", "problem+json")

  case class ProblemJsonCodecFormat() extends CodecFormat {
    override val mediaType: MediaType = ApplicationProblemJsonMediaType
  }

  val problemJsonBody: EndpointIO.Body[String, HttpProblem] = {
    stringBodyUtf8AnyFormat(circeCodec[HttpProblem].format(ProblemJsonCodecFormat()))
  }

  def httpProblems(problemTemplates: HttpProblem*): EndpointOutput.OneOf[HttpProblem, HttpProblem] = {
    val variants = problemTemplates
      .groupBy(_.status.getOrElse(StatusCode.InternalServerError.code))
      .map { case (status, problemStatusTemplates) =>
        oneOfVariantValueMatcher(
          StatusCode(status),
          problemJsonBody
            .description(httpProblemSummary(None, Some(status)))
            .examples(problemStatusTemplates.map(example).toList)
        ) {
          case problem: HttpProblem if problem.status.getOrElse(StatusCode.InternalServerError.code) == status =>
            problemStatusTemplates.exists(_.isTemplateOf(problem))
        }
      }
      .toList

    oneOf[HttpProblem](
      variants.head,
      variants.tail: _*
    )
  }
}
