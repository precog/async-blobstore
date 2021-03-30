package quasar.blobstore.gcs

import scala.{Array, Byte, Some, StringContext, None}

import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.GetService

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsIO
import cats.syntax.applicative._


import org.specs2.matcher.{Matcher, MatchResult}
import org.specs2.mutable.Specification


class GCSGetServiceSpec extends Specification with CatsIO {

  def mkListService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, GetService[IO]] =
    GoogleCloudStorage.mkContainerClient[IO].map(client => GCSGetService.mk[IO](client, bucket, cfg))

  def assertGet(
      service: IO[GetService[IO]],
      blobPath: BlobPath,
      matcher: Matcher[Array[Byte]])
      : IO[MatchResult[Array[Byte]]] =
    service flatMap { svc =>
      svc(blobPath).flatMap {
        case Some(s) => s.compile.to(Array).map(_ must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[Array[Byte]]].pure[IO]
      }
    }

  def assertGetNone(
      service: IO[GetService[IO]],
      blobPath: BlobPath) =
    service flatMap { svc =>
      svc(blobPath).map {
        case Some(s) => ko(s"Unexpected Some: $s")
        case None => ok
      }
    }

    // "get service" >> {

    //   "existing blobpath returns expected bytes" >> {
    //     val expected = "[1, 2]\n[3, 4]\n".getBytes(StandardCharsets.UTF_8)

    //     assertGet(
    //         mkService(PublicConfig),
    //         BlobPath(List(PathElem("testdata"), PathElem("lines.json"))),
    //         be_===(expected))
    //     }

    //   "non-existing blobpath returns none" >> {
    //     assertGetNone(
    //       mkService(PublicConfig),
    //       BlobPath(List(PathElem("testdata"), PathElem("notthere"))))
    //     }

    // }

}