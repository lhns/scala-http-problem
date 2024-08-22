package de.lhns.httpproblem.http4s

import cats.effect.IO
import de.lhns.httpproblem.HttpProblem
import munit.CatsEffectSuite
import org.http4s.Request
import cats.syntax.all._
import de.lhns.httpproblem.http4s.json._

class HttpProblemHttp4sSuite extends CatsEffectSuite {
  test("encode and decode body") {
    val problem = HttpProblem(
      `type` = "urn:test:asdf",
      title = "hello".some,
      detail = "detail".some
    )
    val request = Request[IO]().withEntity(problem)
    request.as[HttpProblem].map { decoded =>
      assertEquals(decoded, problem)
    }
  }
}
