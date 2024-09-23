package de.lhns.httpproblem.http4s

import cats.effect.IO
import de.lhns.httpproblem.HttpProblem
import munit.CatsEffectSuite
import org.http4s.Request
import cats.syntax.all._
import de.lhns.httpproblem.http4s.json._

class Http4sHttpProblemSuite extends CatsEffectSuite {
  test("encode and decode body") {
    val problem = HttpProblem("urn:test:asdf")
      .withTitle("hello")
      .withDetail("detail")

    val request = Request[IO]().withEntity(problem)
    request.as[HttpProblem].map { decoded =>
      assertEquals(decoded, problem)
    }
  }
}
