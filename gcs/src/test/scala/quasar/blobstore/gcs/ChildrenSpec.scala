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

import scala.Some
import scala.Predef.println

import cats.effect.testing.specs2.CatsIO
import cats.effect.{IO, Resource}


import org.http4s.client.Client

import org.slf4s.{Logger, LoggerFactory}
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class ChildrenSpec extends Specification with CatsIO {

    implicit val ec = ExecutionContext.global
    implicit val cs = IO.contextShift(ec)

    val log: Logger = LoggerFactory("quasar.blobstore.gcs.GCSCommonSpec")

    val AUTH_FILE="precog-ci-275718-9de94866bc77.json"
    val bucket = Bucket("precog-test-bucket")
    val goodConfig = common.getAuthFileAsJson(AUTH_FILE)

    def mkListService(cfg: ServiceAccountConfig, bucket: Bucket): Resource[IO, Client [IO]] =
      GoogleCloudStorage.mkContainerClient[IO](cfg) //.map(client => GCSListService(log, client, bucket))
      //TODO: use children instead?


  "lists all resources at the root of the bucket, one per request" >>  {
    mkListService(goodConfig, bucket).map(client => {
      val x = children(client, bucket).map(o => {
        o match {
          case Some(value) => value.compile.toList
          case _ => scala.Predef.???
        }
      })

      //expecting to see dummy x returned in children.apply
      println("x: " + x.unsafeRunSync.unsafeRunSync )
      true must_=== false  
    })
  }

}