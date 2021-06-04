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

import quasar.blobstore.BlobstoreStatus

import java.lang.Throwable
import scala.Option
import scala.util.control.NonFatal

import cats.ApplicativeError
import cats.effect.Sync
import cats.implicits._
import org.slf4s.Logger

object handlers {

  def recoverToNone[F[_]: Sync, A](log: Logger, fa: F[Option[A]]): F[Option[A]] =
    fa.recoverWith {
      case NonFatal(ex) =>
        Sync[F].delay(log.debug("Recovered exception", ex)) *> none[A].pure[F]
    }

  def recoverToBlobstoreStatus[F[_]: ApplicativeError[?[_], Throwable]](fa: F[BlobstoreStatus]): F[BlobstoreStatus] =
    fa.recover {
      case GCSAccessError(_) => BlobstoreStatus.noAccess()
      case NonFatal(t) => BlobstoreStatus.notOk(t.getMessage)
    }

  def reraiseAsGCSAccessError[F[_], A](fa: F[A])(implicit F: ApplicativeError[F, Throwable]): F[A] =
    fa.recoverWith {
      case NonFatal(t) => F.raiseError(GCSAccessError(t.getMessage))
    }

}
