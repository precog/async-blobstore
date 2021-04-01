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

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets.UTF_8

class GCSStatusServiceSpec extends Specification with CatsIO {

  import GoogleAuthConfig.gbqConfigCodecJson

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  //val BAD_AUTH_FILE="bad-auth-file.json"

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

  def mkStatusService(cfg: GoogleAuthConfig, bucket: Bucket): Resource[IO, StatusService[IO]] =
    GoogleCloudStorage.mkContainerClient[IO](cfg).map(client => GCSStatusService(client, bucket, cfg))

  def assertStatus(service: IO[BlobstoreStatus], status: BlobstoreStatus) =
    service.map(s => s must_=== status)

  // def assertStatusError(service: IO[StatusService[IO]]) =
  //   service flatMap { svc =>
  //     svc.map(_ must beLike {
  //       case BlobstoreStatus.notOk(_) => ok
  //     })
  //   }

  "status service" >> {

    "accessible container returns Ok" >> {
      assertStatus(
        mkStatusService(getConfig(AUTH_FILE), Bucket("precog-test-bucket")).use(a => a.map(b => b)),
        BlobstoreStatus.ok())
    }

    "non-existant container returns Not Found" >> {
      assertStatus(
        mkStatusService(getConfig(AUTH_FILE), Bucket("nonexistantbucket")).use(a => a.map(b => b)),
        BlobstoreStatus.notOk("Error: Not Found") )
    }

    // "wrong credentials returns no access" >> {
    //   assertStatus(
    //     mkStatusService(getConfig(BAD_AUTH_FILE), Bucket("precog-test-bucket")).use(a => a.map(b => b)),
    //     BlobstoreStatus.noAccess())
    // }

    // "invalid config returns not ok" >> {
    //   assertStatusNotOk(mkService(InvalidConfig))
    // }
  }

}
