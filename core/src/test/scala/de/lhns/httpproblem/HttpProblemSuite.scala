package de.lhns.httpproblem

import cats.syntax.all._
import io.circe.Json
import munit.FunSuite
import org.http4s.Status

class HttpProblemSuite extends FunSuite {
  test("create http problem") {
    val problem = HttpProblem("urn:test:asdf")
      .withStatus(Status.BadRequest.code)
      .withTitle("hello")
      .withDetail("")
      .withExtension("asdf", Json.fromString("test"))

    assertEquals(
      problem.toJson.noSpaces,
      """{"type":"urn:test:asdf","status":400,"title":"hello","detail":"","asdf":"test"}"""
    )

    assertEquals(problem.toJson.as[HttpProblem], Right(problem))

    assertEquals(problem.exception().getMessage, "hello (urn:test:asdf): ")
  }
}
