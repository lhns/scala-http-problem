package de.lhns.httpproblem

case class HttpProblemException(problem: HttpProblem, cause: Option[Throwable] = None)
    extends RuntimeException(problem.errorMessage, cause.orNull)
