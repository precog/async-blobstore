import scala.collection.Seq

ThisBuild / crossScalaVersions := Seq("2.12.10", "2.13.1")
ThisBuild / scalaVersion := "2.12.10"

ThisBuild / publishAsOSSProject := true

homepage in ThisBuild := Some(url("https://github.com/slamdata/async-blobstore"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/slamdata/async-blobstore"),
  "scm:git@github.com:slamdata/async-blobstore.git"))

val AwsSdkVersion = "2.9.1"
val Fs2Version = "2.2.1"
val MonixVersion = "3.0.0"
// Make sure this is the same for different sub projectss
val NettyVersion = "4.1.44.Final"

// Include to also publish a project's tests
lazy val publishTestsSettings = Seq(
  publishArtifact in (Test, packageBin) := true)

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core, azure, s3)
  .enablePlugins(AutomateHeaderPlugin)

lazy val core = project
  .in(file("core"))
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "async-blobstore-core",
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %% "monocle-core" % "1.6.0",
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-reactive-streams" % Fs2Version))
  .enablePlugins(AutomateHeaderPlugin)

lazy val s3 = project
  .in(file("s3"))
  .dependsOn(core)
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "async-blobstore-s3",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % Fs2Version,
      "io.netty" % "netty-all" % NettyVersion,
      "io.monix" %% "monix-catnap" % MonixVersion,
      "software.amazon.awssdk" % "netty-nio-client" % AwsSdkVersion,
      "software.amazon.awssdk" % "s3" % AwsSdkVersion))
  .enablePlugins(AutomateHeaderPlugin)

lazy val azure = project
  .in(file("azure"))
  .dependsOn(core)
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "async-blobstore-azure",
    libraryDependencies ++= Seq(
      "ch.timo-schmid" %% "slf4s-api" % "1.7.26",
      "io.netty" % "netty-all" % NettyVersion,
      "com.microsoft.azure" % "azure-storage-blob" % "10.5.0",
      "com.azure" % "azure-identity" % "1.0.4",
      "eu.timepit" %% "refined" % "0.9.9",
      "io.reactivex.rxjava2" % "rxjava" % "2.2.2"))
  .enablePlugins(AutomateHeaderPlugin)
