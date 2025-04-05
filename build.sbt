lazy val scalaVersions = Seq("3.3.5", "2.13.16")

ThisBuild / scalaVersion := scalaVersions.head
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization := "de.lhns"
name := (core.projectRefs.head / name).value

val V = new {
  val circe = "0.14.12"
  val http4s = "0.23.30"
  val logbackClassic = "1.5.18"
  val munitCatsEffect = "2.1.0"
  val sttpOpenapiCirceYaml = "0.11.7"
  val tapir = "1.11.23"
}

lazy val commonSettings: SettingsDefinition = Def.settings(
  version := {
    val Tag = "refs/tags/v?([0-9]+(?:\\.[0-9]+)+(?:[+-].*)?)".r
    sys.env
      .get("CI_VERSION")
      .collect { case Tag(tag) => tag }
      .getOrElse("0.0.1-SNAPSHOT")
  },
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),
  homepage := scmInfo.value.map(_.browseUrl),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/lhns/scala-http-problem"),
      "scm:git@github.com:lhns/scala-http-problem.git"
    )
  ),
  developers := List(
    Developer(
      id = "lhns",
      name = "Pierre Kisters",
      email = "pierrekisters@gmail.com",
      url = url("https://github.com/lhns/")
    )
  ),
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % V.logbackClassic % Test,
    "org.typelevel" %%% "munit-cats-effect" % V.munitCatsEffect % Test
  ),
  testFrameworks += new TestFramework("munit.Framework"),
  Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
  Compile / doc / sources := Seq.empty,
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value,
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    sonatypeCredentialHost.value,
    username,
    password
  )).toList
)

lazy val root: Project =
  project
    .in(file("."))
    .settings(commonSettings)
    .settings(
      publishArtifact := false,
      publish / skip := true
    )
    .aggregate(core.projectRefs: _*)
    .aggregate(http4s.projectRefs: _*)
    .aggregate(tapir.projectRefs: _*)

lazy val core = projectMatrix
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "scala-http-problem",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % V.circe,
      "io.circe" %%% "circe-generic" % V.circe,
      "io.circe" %%% "circe-parser" % V.circe,
      "org.http4s" %%% "http4s-core" % V.http4s % Test
    )
  )
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions)

lazy val http4s = projectMatrix
  .in(file("http4s"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-http-problem-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-circe" % V.http4s
    )
  )
  .jvmPlatform(scalaVersions)
  .jsPlatform(scalaVersions)

lazy val tapir = projectMatrix
  .in(file("tapir"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "scala-http-problem-tapir",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % V.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-openapi-docs" % V.tapir % Test
    )
  )
  .jvmPlatform(
    scalaVersions,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % V.sttpOpenapiCirceYaml % Test
    )
  )
  .jsPlatform(scalaVersions)
