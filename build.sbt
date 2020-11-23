import scala.collection.Seq

ThisBuild / crossScalaVersions := Seq("2.12.12", "2.13.3")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.head

ThisBuild / publishAsOSSProject := true

ThisBuild / githubRepository := "async-blobstore"

homepage in ThisBuild := Some(url("https://github.com/precog/async-blobstore"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/precog/async-blobstore"),
  "scm:git@github.com:precog/async-blobstore.git"))

val AwsSdkVersion = "2.9.1"
val Fs2Version = "2.4.5"
val MonixVersion = "3.3.0"
// Make sure this is the same for different sub projects
val NettyVersion = "4.1.52.Final"
val SpecsVersion = "4.10.5"

// Include to also publish a project's tests
lazy val publishTestsSettings = Seq(
  publishArtifact in (Test, packageBin) := true)

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core, azure, s3)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "async-blobstore-core",
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %% "monocle-core" % "1.6.0",
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-reactive-streams" % Fs2Version))

lazy val s3 = project
  .in(file("s3"))
  .dependsOn(core)
  .settings(
    name := "async-blobstore-s3",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % Fs2Version,
      //"io.netty" % "netty-all" % NettyVersion,
      "io.monix" %% "monix-catnap" % MonixVersion,
      "software.amazon.awssdk" % "netty-nio-client" % AwsSdkVersion,
      "software.amazon.awssdk" % "s3" % AwsSdkVersion))

lazy val azure = project
  .in(file("azure"))
  .dependsOn(core)
  .settings(
    name := "async-blobstore-azure",
    libraryDependencies ++= Seq(
      "ch.timo-schmid" %% "slf4s-api" % "1.7.26",
      //"io.netty" % "netty-all" % NettyVersion,
      "com.azure" % "azure-storage-blob" % "12.9.0",
      "com.azure" % "azure-identity" % "1.2.0",
      "io.projectreactor" %% "reactor-scala-extensions" % "0.6.0",
      "org.specs2" %% "specs2-core" % SpecsVersion % Test,
      "com.codecommit" %% "cats-effect-testing-specs2" % "0.4.1" % Test))
