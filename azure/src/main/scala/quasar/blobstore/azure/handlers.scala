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

import quasar.blobstore.BlobstoreStatus

import java.lang.Throwable
import java.nio.ByteBuffer
import scala.{Byte, Int, None, Option, StringContext, Unit}
import scala.util.control.NonFatal

import cats.ApplicativeError
import cats.data.Kleisli
import cats.effect.Sync
import cats.instances.int._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.eq._
import cats.syntax.functor._
import cats.syntax.option._
import com.azure.storage.blob.models.BlobStorageException
import fs2.{Chunk, Stream}

object handlers {

  def recoverToBlobstoreStatus[F[_]: ApplicativeError[?[_], Throwable]](fa: F[BlobstoreStatus]): F[BlobstoreStatus] =
    fa.recover {
      case ex: BlobStorageException if ex.getStatusCode === 403 =>
        BlobstoreStatus.noAccess()
      case ex: BlobStorageException if ex.getStatusCode === 404 =>
        BlobstoreStatus.notFound()
      case ex: BlobStorageException =>
        BlobstoreStatus.notOk(s"Status: ${ex.getStatusCode} Message:${ex.getMessage}")
      case NonFatal(t) => BlobstoreStatus.notOk(t.getMessage)
    }

  def recoverToStatusCode[F[_]: ApplicativeError[?[_], Throwable]](fa: F[Int]): F[Int] =
    fa.recover {
      case ex: BlobStorageException => ex.getStatusCode()
      case NonFatal(_) => 500
    }

  def recoverToNone[F[_], A](fa: F[A])(implicit F: ApplicativeError[F, Throwable]): F[Option[A]] =
    F.recover(fa.map(_.some)) {
      case NonFatal(_) => None
    }

  def raiseInnerStreamError[F[_]: Sync, A](
      s: Stream[F,A])
      : F[Stream[F, A]] =
    s.compile.drain.handleErrorWith(_.raiseError[F, Unit]) *> s.pure[F]

  def raiseInnerStreamErrorK[F[_]: Sync, A]: Kleisli[F, Stream[F, A], Stream[F, A]] =
    Kleisli(raiseInnerStreamError[F, A])

  def recoverStorageException[F[_], A](fa: F[A])(implicit F: ApplicativeError[F, Throwable]): F[Option[A]] =
    F.recover(fa.map(_.some)) {
      case _: BlobStorageException => none
    }

  def toByteStream[F[_]: Sync](s: Stream[F, ByteBuffer]): F[Stream[F, Byte]] =
    Sync[F].delay {
      s flatMap (buf => Stream.chunk(Chunk.byteBuffer(buf)))
    }

  def toByteStreamK[F[_]: Sync]: Kleisli[F, Stream[F, ByteBuffer], Stream[F, Byte]] =
    Kleisli(toByteStream[F])

}
