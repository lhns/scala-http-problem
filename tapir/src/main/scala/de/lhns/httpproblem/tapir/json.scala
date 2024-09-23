package de.lhns.httpproblem.tapir

import de.lhns.httpproblem.HttpProblem
import sttp.model.StatusCode
import sttp.tapir.EndpointIO.Example
import sttp.tapir.Schema.SName
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._

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

  def httpProblems(problems: HttpProblem*): EndpointOutput.OneOf[HttpProblem, HttpProblem] = {
    val variants = problems
      .groupBy(_.status.getOrElse(StatusCode.InternalServerError.code))
      .map { case (status, problems) =>
        oneOfVariantValueMatcher(
          StatusCode(status),
          jsonBody[HttpProblem]
            .description(httpProblemSummary(None, Some(status)))
            .examples(problems.map(example).toList)
        ) {
          case problem: HttpProblem if problem.status.getOrElse(StatusCode.InternalServerError.code) == status =>
            true
        }
      }
      .toList

    oneOf[HttpProblem](
      variants.head,
      variants.tail: _*
    )
  }
}
