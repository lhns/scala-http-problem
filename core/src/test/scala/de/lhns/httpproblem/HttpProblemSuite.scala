package de.lhns.httpproblem

import cats.syntax.all._
import io.circe.Json
import munit.FunSuite
import org.http4s.{Status, Uri}

class HttpProblemSuite extends FunSuite {
  test("create http problem") {
    val problem = HttpProblem(
      `type` = "urn:test:asdf",
      status = Status.BadRequest.code.some,
      title = "hello".some,
      detail = "".some,
      extensions = Map("asdf" -> Json.fromString("test"))
    )

    assertEquals(
      problem.toJson.noSpaces,
      """{"type":"urn:test:asdf","status":400,"title":"hello","detail":"","asdf":"test"}"""
    )

    assertEquals(problem.toJson.as[HttpProblem], Right(problem))

    assertEquals(problem.exception().getMessage, "hello (urn:test:asdf): ")
  }
}
