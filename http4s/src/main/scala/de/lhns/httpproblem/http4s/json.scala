package de.lhns.httpproblem.http4s

import cats.effect.Concurrent
import de.lhns.httpproblem.HttpProblem
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, EntityEncoder, MediaType}

object json {
  implicit def httpProblemEncoder[F[_]]: EntityEncoder[F, HttpProblem] =
    org.http4s.circe
      .jsonEncoderOf[F, HttpProblem]
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  implicit def httpProblemDecoder[F[_]: Concurrent]: EntityDecoder[F, HttpProblem] =
    org.http4s.circe.jsonOf[F, HttpProblem]
}
