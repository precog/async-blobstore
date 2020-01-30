/*
 * Copyright 2014–2020 SlamData Inc.
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

import quasar.blobstore.BlobstoreStatus

import java.lang.Throwable

import scala.{Byte, Option}
import scala.util.control.NonFatal

import cats.ApplicativeError
import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.instances.int._
import cats.syntax.applicativeError._
import cats.syntax.eq._
import cats.syntax.functor._
import cats.syntax.option._
import com.microsoft.azure.storage.blob.{DownloadResponse, ReliableDownloadOptions, StorageException}
import fs2.{Chunk, Stream}

object handlers {

  def recoverToBlobstoreStatus[F[_]: ApplicativeError[?[_], Throwable], A](fa: F[A]): F[BlobstoreStatus] =
    fa.map(_ => BlobstoreStatus.ok()).recover {
      case ex: StorageException if ex.statusCode === 403 => BlobstoreStatus.noAccess()
      case ex: StorageException if ex.statusCode === 404 => BlobstoreStatus.notFound()
      case ex: StorageException => BlobstoreStatus.notOk(ex.message())
      case NonFatal(t) => BlobstoreStatus.notOk(t.getMessage)
    }

  def recoverNotFound[F[_], A](fa: F[A])(implicit F: ApplicativeError[F, Throwable]): F[Option[A]] =
    F.recover(fa.map(_.some)) {
      case ex: StorageException if ex.statusCode() === 404 => none
    }

  def recoverStorageException[F[_], A](fa: F[A])(implicit F: ApplicativeError[F, Throwable]): F[Option[A]] =
    F.recover(fa.map(_.some)) {
      case ex: StorageException => none
    }

  def toByteStream[F[_]: ConcurrentEffect](reliableDownloadOptions: ReliableDownloadOptions, maxQueueSize: MaxQueueSize)(r: DownloadResponse): F[Stream[F, Byte]] =
    ConcurrentEffect[F].delay {
      for {
        buf <- rx.flowableToStream(r.body(reliableDownloadOptions), maxQueueSize.value)
        b <- Stream.chunk(Chunk.byteBuffer(buf))
      } yield b
    }

  def toByteStreamK[F[_]: ConcurrentEffect](reliableDownloadOptions: ReliableDownloadOptions, maxQueueSize: MaxQueueSize): Kleisli[F, DownloadResponse, Stream[F, Byte]] =
    Kleisli(toByteStream[F](reliableDownloadOptions, maxQueueSize))

}
