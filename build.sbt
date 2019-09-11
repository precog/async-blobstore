import scala.collection.Seq

ThisBuild / publishAsOSSProject := true

homepage in ThisBuild := Some(url("https://github.com/slamdata/async-blobstore"))

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/slamdata/async-blobstore"),
  "scm:git@github.com:slamdata/async-blobstore.git"))

// Include to also publish a project's tests
lazy val publishTestsSettings = Seq(
  publishArtifact in (Test, packageBin) := true)

lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(core, azure)
  .enablePlugins(AutomateHeaderPlugin)

lazy val core = project
  .in(file("core"))
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "async-blobstore-core",
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %% "monocle-core" % "2.0.0",
      "co.fs2" %% "fs2-core" % "1.0.5"))
  .enablePlugins(AutomateHeaderPlugin)

lazy val azure = project
  .in(file("azure"))
  .dependsOn(core)
  .settings(addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  .settings(
    name := "async-blobstore-azure",
    libraryDependencies ++= Seq(
      "com.microsoft.azure" % "azure-storage-blob" % "10.5.0",
      "eu.timepit" %% "refined" % "0.9.9",
      // netty-all isn't strictly necessary but takes advantage of native libs.
      // Azure doesn't pull in libs like netty-transport-native-kqueue,
      // netty-transport-native-unix-common and netty-transport-native-epoll.
      // Keep nettyVersion in sync with the version that Azure pulls in.
      "io.netty" % "netty-all" % "4.1.38.Final",
      "io.reactivex.rxjava2" % "rxjava" % "2.2.2"))
  .enablePlugins(AutomateHeaderPlugin)
