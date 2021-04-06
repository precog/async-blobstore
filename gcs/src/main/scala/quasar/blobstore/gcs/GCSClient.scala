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

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Sync}
import cats.implicits._
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Request, Response}
import org.slf4s.Logger
import org.slf4s.LoggerFactory

object GCSClient {
  private val log: Logger = LoggerFactory("quasar.blobstore.gcs")

  private def traceLog[F[_]: Sync](s: String): F[Unit] =
    Sync[F].delay(log.trace(s))

  private def http4sLogger[F[_]: Concurrent](client: Client[F]): Client[F] =
    ResponseLogger.apply(logHeaders = true, logBody = false, logAction = Some(traceLog[F](_)))(
      RequestLogger.apply(logHeaders = true, logBody = true, logAction = Some(traceLog[F](_)))(
        client
      )
    )

  private def signRequest[F[_]: Concurrent: ContextShift](cfg: ServiceAccountConfig, req: Request[F]): F[Request[F]] = {
    for {
      accessToken <- GoogleCloudStorage.getAccessToken(cfg.serviceAccountAuthBytes)
      _ <- traceLog("accessToken: " + accessToken)
      bearerToken = Authorization(Credentials.Token(AuthScheme.Bearer, accessToken.getTokenValue))
      _ <- traceLog("bearerToken: " + bearerToken)
    } yield req.transformHeaders(_.put(bearerToken))
  }

  def sign[F[_]: Concurrent: ContextShift](cfg: ServiceAccountConfig)(client: Client[F]): Client[F] = {

    def signAndSubmit: Request[F] => Resource[F, Response[F]] =
      (req => Resource.suspend {
        handlers
          .reraiseAsGCSAccessError(signRequest[F](cfg, req))
          .map(client.run(_))
      })

    Client(signAndSubmit)
  }

  def apply[F[_]: ConcurrentEffect: ContextShift](cfg: ServiceAccountConfig)
      : Resource[F, Client[F]] =
    AsyncHttpClientBuilder[F]
      .map[F, Client[F]](sign(cfg))
      .map[F, Client[F]](http4sLogger)
}
