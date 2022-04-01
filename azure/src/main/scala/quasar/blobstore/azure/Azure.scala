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

package quasar.blobstore.azure

import quasar.blobstore.azure.AzureCredentials.ActiveDirectory
import quasar.blobstore.CompatConverters.All._

import java.time.Instant
import scala._
import scala.Predef._
import scala.concurrent.duration.MILLISECONDS

import cats._
import cats.implicits._
import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import cats.effect.concurrent.Ref
import com.azure.core.credential.{AccessToken, TokenCredential, TokenRequestContext}
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.storage.blob._
import com.azure.storage.common.StorageSharedKeyCredential
import org.slf4s.Logging
import reactor.core.publisher.Mono

object Azure extends Logging {

  def mkStdStorageUrl(name: AccountName): StorageUrl =
    StorageUrl(s"https://${name.value}.blob.core.windows.net/")

  def getAccessToken[F[_]: ConcurrentEffect: ContextShift](ad: ActiveDirectory): F[Expires[AccessToken]] = {
    val mkBuilder =
      Sync[F].delay {
        (new ClientSecretCredentialBuilder)
          .clientId(ad.clientId.value)
          .tenantId(ad.tenantId.value)
          .clientSecret(ad.clientSecret.value)
          .build()
          .getToken((new TokenRequestContext)
            .setScopes(List("https://storage.azure.com/.default").asJava))
      }

    mkBuilder
      .flatMap(reactive.monoToAsync[F, AccessToken])
      .map(t => Expires(t, t.getExpiresAt()))
  }

  def setCredentials[F[_]: ConcurrentEffect: ContextShift](
      cred: AzureCredentials,
      builder: BlobContainerClientBuilder)
      : F[Expires[BlobContainerClientBuilder]] =
    ConcurrentEffect[F].delay {
      cred match {
        case AzureCredentials.SharedKey(accountName, accountKey) =>
          Expires.never(
            builder
              .credential(new StorageSharedKeyCredential(accountName.value, accountKey.value))).pure[F]
        case ad @ AzureCredentials.ActiveDirectory(_, _, _) =>
          Functor[F].compose[Expires].map(getAccessToken[F](ad)) { token =>
            builder.credential(new TokenCredential {
              def getToken(ctx: TokenRequestContext): Mono[AccessToken] =
                Mono.just(token)
            })
          }
      }
    }.flatten

    def mkBuilder[F[_]](cfg: Config)(implicit F: Sync[F]): F[BlobContainerClientBuilder] =
      F.catchNonFatal {
        new BlobContainerClientBuilder()
          .endpoint(cfg.storageUrl.value)
          .containerName(cfg.containerName.value)
          //TODO
          // .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
      }

  def mkContainerClient[F[_]: ContextShift](
      cfg: Config)(
      implicit F: ConcurrentEffect[F])
      : F[Expires[BlobContainerAsyncClient]] =
    for {
      builder <- mkBuilder(cfg)
      newBuilder <-
        cfg.credentials
          .map(setCredentials[F](_, builder))
          .getOrElse(Expires.never(builder).pure[F])
      ec <- F.delay(newBuilder.map(_.buildAsyncClient()))
    } yield ec

  def refContainerClient[F[_]: ConcurrentEffect: ContextShift: Timer](cfg: Config)
      : F[(Ref[F, Expires[BlobContainerAsyncClient]], F[Unit])] =
    for {
      containerClient <- mkContainerClient(cfg)
      ref <- Ref.of[F, Expires[BlobContainerAsyncClient]](containerClient)
      refresh = for {
        epochNow <- Timer[F].clock.realTime(MILLISECONDS)
        now = Instant.ofEpochMilli(epochNow)
        expiresAt <- ref.get.map(_.expiresAt)

        _ <- debug(s"Credentials expire on: ${expiresAt}")

        refresh = mkContainerClient(cfg).flatMap(ref.set(_))

        _ <-
          if (now.isAfter(expiresAt.toInstant))
            debug("Credentials expired, renewing...") *> refresh *> debug("Renewed credentials")
          else
            debug("Credentials are still valid.")
        } yield ()
    } yield (ref, refresh)

  private def debug[F[_]: Sync](str: String): F[Unit] =
    Sync[F].delay(log.debug(str))
}
