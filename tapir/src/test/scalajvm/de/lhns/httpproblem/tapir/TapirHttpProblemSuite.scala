package de.lhns.httpproblem.tapir

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import de.lhns.httpproblem.HttpProblem
import de.lhns.httpproblem.http4s.json._
import de.lhns.httpproblem.tapir.json._
import munit.FunSuite
import org.http4s.{Method, Request}
import sttp.apispec.openapi.circe.yaml._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

class TapirHttpProblemSuite extends FunSuite {
  object Problems {
    val invalidHeader: HttpProblem = HttpProblem("urn:problem:invalid-header").withStatus(StatusCode.BadRequest.code)
    val invalidBody: HttpProblem = HttpProblem("urn:problem:invalid-body").withStatus(StatusCode.BadRequest.code)
    val other: HttpProblem = HttpProblem.withoutType.withStatus(StatusCode.InternalServerError.code)
  }

  test("validate schema") {
    val testEndpoint = endpoint
      .errorOut(
        httpProblems(
          Problems.invalidHeader,
          Problems.other
        )
      )

    val openApi = OpenAPIDocsInterpreter()
      .toOpenAPI(
        testEndpoint,
        "test",
        "0.0.1"
      )
      .openapi("3.0.3")

    val expected =
      """openapi: 3.0.3
        |info:
        |  title: test
        |  version: 0.0.1
        |paths:
        |  /:
        |    get:
        |      operationId: getRoot
        |      responses:
        |        '200':
        |          description: ''
        |        '400':
        |          description: Problem Details with status code 400
        |          content:
        |            application/problem+json:
        |              schema:
        |                $ref: '#/components/schemas/HttpProblem'
        |              examples:
        |                urn:problem:invalid-header:
        |                  summary: Problem Details with type urn:problem:invalid-header
        |                  value:
        |                    type: urn:problem:invalid-header
        |                    status: 400
        |        '500':
        |          description: Problem Details with status code 500
        |          content:
        |            application/problem+json:
        |              schema:
        |                $ref: '#/components/schemas/HttpProblem'
        |              examples:
        |                Example:
        |                  summary: Problem Details with status code 500
        |                  value:
        |                    status: 500
        |        default:
        |          description: Problem Details
        |          content:
        |            application/problem+json:
        |              schema:
        |                $ref: '#/components/schemas/HttpProblem'
        |components:
        |  schemas:
        |    HttpProblem:
        |      title: HttpProblem
        |      type: object
        |      properties:
        |        type:
        |          type: string
        |          format: uri
        |        status:
        |          type: integer
        |          format: int32
        |        title:
        |          type: string
        |        detail:
        |          type: string
        |        instance:
        |          type: string
        |""".stripMargin

    def normalizeString(string: String): String =
      string.trim.replace("\r\n", "\n")

    assertEquals(normalizeString(expected), normalizeString(openApi.toYaml))
  }

  test("error handling of known error") {
    val testEndpoint = Http4sServerInterpreter[IO]().toRoutes {
      endpoint
        .errorOut(
          httpProblems(
            Problems.invalidHeader,
            Problems.other
          )
        )
        .serverLogicPure[IO] { _ =>
          Left(Problems.invalidHeader)
        }
    }

    val response =
      testEndpoint(Request(Method.POST)).semiflatMap(_.as[HttpProblem]).value.unsafeRunSync()(IORuntime.global).get

    assertEquals(response, Problems.invalidHeader)
  }

  test("error handling of unknown error") {
    val testEndpoint = Http4sServerInterpreter[IO]().toRoutes {
      endpoint
        .errorOut(
          httpProblems(
            Problems.invalidHeader,
            Problems.other
          )
        )
        .serverLogicPure[IO] { _ =>
          Left(Problems.invalidBody)
        }
    }

    val response =
      testEndpoint(Request(Method.POST)).semiflatMap(_.as[HttpProblem]).value.unsafeRunSync()(IORuntime.global).get

    assertEquals(response, Problems.invalidBody)
  }
}
