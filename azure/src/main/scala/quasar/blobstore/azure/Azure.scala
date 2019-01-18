/*
 * Copyright 2014–2018 SlamData Inc.
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

import scala.{None, Option, Some, StringContext}

import cats.effect.Sync
import cats.syntax.functor._
import com.microsoft.azure.storage.blob._

object Azure {

  def mkStdStorageUrl(name: AccountName): StorageUrl =
    StorageUrl(s"https://${name.value}.blob.core.windows.net/")

  def mkCredentials(cred: Option[AzureCredentials]): ICredentials =
    cred match {
      case None => new AnonymousCredentials
      case Some(c) => new SharedKeyCredentials(c.accountName.value, c.accountKey.value)
    }

  def mkContainerUrl[F[_]](cfg: Config)(implicit F: Sync[F]): F[ContainerURL] =
    F.catchNonFatal(new URL(cfg.storageUrl.value)) map { url =>
      val serviceUrl = new ServiceURL(
        url,
        StorageURL.createPipeline(
          mkCredentials(cfg.credentials),
          // Azure SDK changed its logging behavior in 10.3.0 (compared to 10.2.0).
          // In 10.2.0 the standard `new PipelineOptions` could be used, but in 10.3.0
          // this results in messages being logged to console in eg quasar's repl
          // (despite having no console logger configured).
          // Still not having a full understanding of how this logging is supposed to work
          // exactly (e.g. `withLogger`) but adding `LoggingOptions` with
          // `disableDefaultLogging = true` seems to be fixing it.
          // As an aside, in case we need to look at this in the future,
          // Azure SDK has 2 loggers for some reason:
          // - `com.microsoft.azure.storage.blob.LoggingFactory`
          // - `Azure Storage Java SDK`
          new PipelineOptions().withLoggingOptions(
            new LoggingOptions(3000, true))))
      serviceUrl.createContainerURL(cfg.containerName.value)
    }
}
