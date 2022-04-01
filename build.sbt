import scala.collection.Seq

ThisBuild / crossScalaVersions := Seq("2.12.12", "2.13.3")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.head

ThisBuild / publishAsOSSProject := true

ThisBuild / githubRepository := "async-blobstore"

homepage in ThisBuild := Some(url("https://github.com/precog/async-blobstore"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/precog/async-blobstore"),
  "scm:git@github.com:precog/async-blobstore.git"))

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Sbt(
    List("decryptSecret gcs/src/test/resources/precog-ci-275718-9de94866bc77.json.enc"),
    name = Some("Decrypt gcp service account json key"))

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Sbt(
    List("decryptSecret gcs/src/test/resources/bad-auth-file.json.enc"),
    name = Some("Decrypt bad gcp service account json key"))

val AwsSdkVersion = "2.16.21"
val Fs2Version = "2.5.6"
val MonixVersion = "3.4.0"
val SpecsVersion = "4.10.6"
val Http4sVersion = "0.21.24"
val GoogleAuthLib = "0.25.0"
val ArgonautVersion = "6.3.2"
val Slf4sVersion = "1.7.26"
val Log4jVersion = "2.14.0"

// Include to also publish a project's tests
lazy val publishTestsSettings = Seq(
  publishArtifact in (Test, packageBin) := true)

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core, azure, gcs, s3)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "async-blobstore-core",
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %% "monocle-core" % "1.6.0",
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-reactive-streams" % Fs2Version,
      "org.specs2" %% "specs2-core" % SpecsVersion % Test,
      "ch.timo-schmid" %% "slf4s-api" % Slf4sVersion))

lazy val s3 = project
  .in(file("s3"))
  .dependsOn(core)
  .settings(
    name := "async-blobstore-s3",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % Fs2Version,
      "io.monix" %% "monix-catnap" % MonixVersion,
      "software.amazon.awssdk" % "netty-nio-client" % AwsSdkVersion,
      "software.amazon.awssdk" % "s3" % AwsSdkVersion))

lazy val azure = project
  .in(file("azure"))
  .dependsOn(core)
  .settings(
    name := "async-blobstore-azure",
    libraryDependencies ++= Seq(
      "com.azure" % "azure-storage-blob" % "12.9.0",
      "com.azure" % "azure-identity" % "1.2.0",
      "com.codecommit" %% "cats-effect-testing-specs2" % "0.4.1" % Test))

lazy val gcs = project
  .in(file("gcs"))
  .dependsOn(core)
  .settings(
    addCompilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
    scalacOptions += "-P:silencer:globalFilters=http4s-argonaut") // remove after bumping from http4s 0.21.24
  .settings(
    name := "async-blobstore-gcs",
    libraryDependencies ++= Seq(
      "com.google.auth" % "google-auth-library-oauth2-http" % GoogleAuthLib,
      "com.codecommit" %% "cats-effect-testing-specs2" % "0.4.1" % Test,
      "io.argonaut" %% "argonaut" % ArgonautVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4jVersion % Test,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4jVersion % Test,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-argonaut" % Http4sVersion))
