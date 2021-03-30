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

import scala.Predef.println

import quasar.blobstore.services.GetService

import cats.effect.{ConcurrentEffect, ContextShift}
import cats.data.Kleisli
import cats.implicits._

import org.http4s.{
  AuthScheme,
  Credentials,
  Method,
  Request
}
import org.http4s.client.Client
import org.http4s.headers.{Authorization}

object GCSGetService {

  def mk[F[_]: ConcurrentEffect: ContextShift](
      client: Client[F],
      bucket: Bucket,
      config: GoogleAuthConfig)
      : GetService[F] =
    Kleisli { blobPath =>
      for {
        accessToken <- GoogleCloudStorage.getAccessToken(config.serviceAccountAuthBytes)
        _ = println("accessToken: " + accessToken)
        bearerToken = Authorization(Credentials.Token(AuthScheme.Bearer, accessToken.getTokenValue))
        _ = println("bearerToken: " + bearerToken)
        // Get file listings and use them in the gcsGetUrl
        listUrl <- GoogleCloudStorage.gcsGetUrl(bucket, "stuff").pure[F]
        _ = println("listUrl: " + listUrl)
        destReq = Request[F](Method.GET, listUrl).withHeaders(bearerToken)
        _ = println("destReq: " + destReq)
        x <- client.run(destReq).use {r => r.pure[F] }
      } yield x.body.some
    }
}