package de.lhns.httpproblem.tapir

import de.lhns.httpproblem.HttpProblem
import munit.FunSuite
import json._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter

class TapirHttpProblemSuite extends FunSuite {
  test("validate schema") {
    val testEndpoint = endpoint
      .errorOut(
        httpProblems(
          HttpProblem("urn:problem:invalid-header").withStatus(StatusCode.BadRequest.code),
          HttpProblem.withoutType.withStatus(StatusCode.InternalServerError.code)
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
}
