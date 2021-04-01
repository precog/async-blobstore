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

import scala.Predef._

import argonaut._, Argonaut._

import cats.effect.IO
import cats.effect.testing.specs2.CatsIO
import org.specs2.mutable.Specification

import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets.UTF_8

import org.specs2.mutable.Specification

import scala.util.Left
import scala.util.Right

class GCSAccessTokenSpec extends Specification with CatsIO {

  import GoogleAuthConfig.gbqConfigCodecJson

  val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
  val authCfgPath = Paths.get(getClass.getClassLoader.getResource(AUTH_FILE).toURI)
  val authCfgString = new String(Files.readAllBytes(authCfgPath), UTF_8)
  val authCfgJson: Json = Parse.parse(authCfgString) match {
    case Left(value) => Json.obj("malformed" := true)
    case Right(value) => value
  }

  val googleAuthCfg = Json.obj("authCfg" := authCfgJson)
  val goodConfig = googleAuthCfg.as[GoogleAuthConfig].toOption.get

  "access token service" >> {
    "get valid token" >> {
       GCSAccessToken.token[IO](goodConfig.serviceAccountAuthBytes).map(tkn => tkn.getTokenValue.length > 0 )
    }
  }

}
