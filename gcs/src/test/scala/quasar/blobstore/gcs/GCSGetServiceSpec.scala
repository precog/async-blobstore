/*
 * Copyright 2020 Precog Data
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.blobstore.gcs

import scala._
import scala.Predef._

import argonaut._, Argonaut._

import quasar.blobstore.paths.BlobPath
import quasar.blobstore.paths.PathElem
import quasar.blobstore.services.GetServiceResource

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsIO
import cats.syntax.applicative._

import org.slf4s.Logger
import org.slf4s.LoggerFactory

import org.specs2.matcher.{Matcher, MatchResult}
import org.specs2.mutable.Specification

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets.UTF_8


class GCSGetServiceSpec extends Specification with CatsIO {

  private val log: Logger = LoggerFactory("quasar.blobstore.gcs.GCSGetServiceSpec")

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val authCfgPath = Paths.get(getClass.getClassLoader.getResource(AUTH_FILE).toURI)
  val authCfgString = new String(Files.readAllBytes(authCfgPath), UTF_8)
  val authCfgJson: Json = Parse.parse(authCfgString) match {
    case Left(value) => Json.obj("malformed" := true)
    case Right(value) => value
  }

  val googleAuthCfg = Json.obj("authCfg" := authCfgJson)
  val goodConfig = googleAuthCfg.as[GoogleAuthConfig].toOption.get

  def mkGetService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, GetServiceResource[IO]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSGetService.mk[IO](log, client, bucket))

  def assertGet(
      service: Resource[IO, GetServiceResource[IO]],
      blobPath: BlobPath,
      matcher: Matcher[Array[Byte]])
      : IO[MatchResult[Array[Byte]]] =
    service use { svc =>
      svc(blobPath).use {
        case Some(s) => s.compile.to(Array).map(_ must matcher)
        case None => ko("Unexpected None").asInstanceOf[MatchResult[Array[Byte]]].pure[IO]
      }
    }

  def assertGetNone(
      service: Resource[IO, GetServiceResource[IO]],
      blobPath: BlobPath) =
    service flatMap { svc =>
      svc(blobPath).map {
        case Some(s) => s.compile.to(Array).map(a => {println("array: " + a.map(_.toChar).mkString ); a.isEmpty must_=== true}) //ko(s"Unexpected Some: $s")
        case None => ko("Unexpected None").asInstanceOf[MatchResult[Array[Byte]]].pure[IO]
      }
    }

    "get service" >> {

      "existing blobpath returns expected bytes" >> {
        val expected = "42\n".getBytes(UTF_8)

        assertGet(
            mkGetService(goodConfig, Bucket("bucket-8168b20d-a6f0-427f-a21b-232a2e8742e1")),
            BlobPath(List(PathElem("somefolder"), PathElem("nested"), PathElem("int.number.json"))),
            be_===(expected))
        }

      "non-existing blobpath returns none" >> {

        assertGetNone(
          mkGetService(goodConfig, Bucket("bucket-8168b20d-a6f0-427f-a21b-232a2e8742e1")),
          BlobPath(List(PathElem("testdata"), PathElem("notthere"))))
        }

    }

}
