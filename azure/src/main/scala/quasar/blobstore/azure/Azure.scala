/*
 * Copyright 2014–2019 SlamData Inc.
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

import java.net.URL

import scala._
import scala.Predef._
import collection.JavaConverters._

import cats.implicits._
import cats.effect.ConcurrentEffect

import com.microsoft.azure.storage.blob._
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.core.credential.{AccessToken, TokenRequestContext}

object Azure {

  def mkStdStorageUrl(name: AccountName): StorageUrl =
    StorageUrl(s"https://${name.value}.blob.core.windows.net/")

  def mkCredentials[F[_]: ConcurrentEffect](cred: Option[AzureCredentials]): F[ICredentials] =
    cred match {
      case None =>
        (new AnonymousCredentials(): ICredentials).pure[F]

      case Some(AzureCredentials.SharedKey(accountName, accountKey)) =>
        (new SharedKeyCredentials(accountName.value, accountKey.value): ICredentials).pure[F]

      case Some(AzureCredentials.ActiveDirectory(clientId, tenantId, clientSecret)) =>
        rx.publisherToStream[F, AccessToken](
            (new ClientSecretCredentialBuilder())
              .clientId(clientId.value)
              .tenantId(tenantId.value)
              .clientSecret(clientSecret.value)
              .build()
              .getToken((new TokenRequestContext()).setScopes(
                List("https://storage.azure.com/.default").asJava)))
          .evalTap(tk => ConcurrentEffect[F].delay(println(tk.getExpiresAt())))
          .compile
          .lastOrError
          .map(tk => new TokenCredentials(tk.getToken))
    }

  def mkContainerUrl[F[_]](cfg: Config)(implicit F: ConcurrentEffect[F]): F[ContainerURL] =
    F.catchNonFatal(new URL(cfg.storageUrl.value)) flatMap { url =>
      // Azure SDK changed its logging behavior in 10.3.0 (compared to 10.2.0).
      // In 10.2.0 the standard `new PipelineOptions` could be used, but in 10.3.0
      // this results in messages being logged to console in eg quasar's repl
      // (despite having no console logger configured).
      // exactly (e.g. `withLogger`) but adding `LoggingOptions` with
      // `disableDefaultLogging = true` seems to be fixing it.
      // As an aside, in case we need to look at this in the future,
      // Azure SDK has 2 loggers for some reason:
      // - `com.microsoft.azure.storage.blob.LoggingFactory`
      // - `Azure Storage Java SDK`

      mkCredentials(cfg.credentials)
        .map(creds =>
          new ServiceURL(
            url,
            StorageURL.createPipeline(
              creds,
              new PipelineOptions().withLoggingOptions(
                new LoggingOptions(3000, true)))))
        .map(_.createContainerURL(cfg.containerName.value))
    }
}
