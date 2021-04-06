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

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.services.StatusService

import argonaut._, Argonaut._

import cats.effect.{IO, Resource}
import cats.effect.testing.specs2.CatsIO

import org.specs2.mutable.Specification
import org.specs2.matcher.{MatchResult}

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets.UTF_8

class GCSStatusServiceSpec extends Specification with CatsIO {

  import GoogleAuthConfig.gbqConfigCodecJson

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val BAD_AUTH_FILE="bad-auth-file.json"

  def getConfig(authFileName: String): GoogleAuthConfig = {
    val authCfgPath = Paths.get(getClass.getClassLoader.getResource(authFileName).toURI)
    val authCfgString = new String(Files.readAllBytes(authCfgPath), UTF_8)
    val authCfgJson: Json = Parse.parse(authCfgString) match {
      case Left(value) => Json.obj("malformed" := true)
      case Right(value) => value
    }
    val googleAuthCfg = Json.obj("authCfg" := authCfgJson)
    googleAuthCfg.as[GoogleAuthConfig].toOption.get
  }

  def mkService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, StatusService[IO]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSStatusService(client, bucket, cfg))

  def assertStatus(
      service: Resource[IO, StatusService[IO]],
      status: BlobstoreStatus)
      : IO[MatchResult[BlobstoreStatus]] =
    service.use {svc => svc.map(b => b)}.map(s => s must_=== status)

  def assertStatusNotOk(
      service: Resource[IO, StatusService[IO]])
      : IO[MatchResult[BlobstoreStatus]] =
    service use { svc =>
      svc.map(_ must beLike {
        case BlobstoreStatus.notOk(_) => ok
      })
    }

  "status service" >> {

    "valid, accessible, public container returns ok" >> {
      assertStatus(
        mkService(getConfig(AUTH_FILE), Bucket("precog-test-bucket")),
        BlobstoreStatus.ok())
     }

    "non existing container returns not found" >> {
      assertStatus(
        mkService(getConfig(AUTH_FILE), Bucket("nonexisting-bucket")),
        BlobstoreStatus.notFound())
    }

    "wrong credentials returns no access" >> {
      assertStatus(
        mkService(getConfig(BAD_AUTH_FILE), Bucket("precog-test-bucket")),
        BlobstoreStatus.noAccess())
    }

    // "invalid config returns not ok" >> {
    //   assertStatus(
    //     mkService(getConfig(AUTH_FILE), Bucket("&^%")),
    //     BlobstoreStatus.noAccess())
    // }
  }

}
