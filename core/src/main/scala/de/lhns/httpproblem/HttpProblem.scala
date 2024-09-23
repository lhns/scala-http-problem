package de.lhns.httpproblem

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.net.URI

case class HttpProblem private (
    `type`: Option[URI],
    status: Option[Int],
    title: Option[String],
    detail: Option[String],
    instance: Option[String],
    extensions: Map[String, Json]
) {
  final def tpe: Option[URI] = `type`

  final def withType(uri: URI): HttpProblem = copy(`type` = Some(uri))

  final def withType(uri: String): HttpProblem = copy(`type` = Some(URI.create(uri)))

  final def withoutType: HttpProblem = copy(`type` = None)

  final def withStatus(status: Int): HttpProblem = copy(status = Some(status))

  final def withoutStatus: HttpProblem = copy(status = None)

  final def withTitle(title: String): HttpProblem = copy(title = Some(title))

  final def withoutTitle: HttpProblem = copy(title = None)

  final def withDetail(detail: String): HttpProblem = copy(detail = Some(detail))

  final def withoutDetail: HttpProblem = copy(detail = None)

  final def withInstance(instance: String): HttpProblem = copy(instance = Some(instance))

  final def withoutInstance: HttpProblem = copy(instance = None)

  final def withExtensions(extensions: Map[String, Json]): HttpProblem = copy(extensions = extensions)

  final def extension[A: Decoder](key: String): Option[A] =
    extensions.get(key).flatMap(Decoder[A].decodeJson(_).toOption)

  final def withExtension[A: Encoder](key: String, value: A): HttpProblem =
    copy(extensions = extensions + (key -> Encoder[A].apply(value)))

  final def withoutExtension(key: String): HttpProblem = copy(extensions = extensions - key)

  def errorMessage: String =
    (title.map(_ + `type`.fold("")(" (" + _ + ")")).toList ++
      detail.map(_ + instance.fold("")(" (" + _ + ")")).toList)
      .mkString(": ")

  @inline
  final def exception(cause: Option[Throwable] = None): HttpProblemException = HttpProblemException(this, cause)

  @inline
  final def throwException(cause: Option[Throwable] = None): Unit = throw exception(cause)

  final def toJson: Json = (this: HttpProblem).asJson

  /*final def toXml: String = TODO*/
}

object HttpProblem {
  val withoutType: HttpProblem = new HttpProblem(
    `type` = None,
    status = None,
    title = None,
    detail = None,
    instance = None,
    extensions = Map.empty
  )

  def apply(tpe: URI): HttpProblem =
    new HttpProblem(
      `type` = Some(tpe),
      status = None,
      title = None,
      detail = None,
      instance = None,
      extensions = Map.empty
    )

  def apply(tpe: String): HttpProblem =
    HttpProblem(URI.create(tpe))

  implicit val codec: Codec[HttpProblem] = {
    val reserved: Set[String] = Set(
      "type",
      "status",
      "title",
      "detail",
      "instance"
    )

    val httpProblemCodec: Codec[HttpProblem] = deriveCodec

    Codec.from[HttpProblem](
      Decoder.instance[HttpProblem] { cursor =>
        Decoder[JsonObject].apply(cursor).flatMap { obj =>
          val problemJson = obj.filterKeys(reserved.contains).add("extensions", JsonObject.empty.toJson).toJson
          val extensions = obj.filterKeys(!reserved.contains(_)).toMap
          httpProblemCodec
            .decodeJson(problemJson)
            .map(_.copy(extensions = extensions))
        }
      },
      Encoder.instance[HttpProblem] { problem =>
        Json
          .fromFields(problem.extensions)
          .deepMerge(httpProblemCodec(problem.copy(extensions = Map.empty)).mapObject(_.remove("extensions")))
          .dropNullValues
      }
    )
  }
}
