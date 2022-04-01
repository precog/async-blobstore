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

import java.nio.ByteBuffer
import scala.{Boolean, Byte}
import scala.Predef._

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.azure.core.http.rest.Response
import com.azure.storage.blob._
import com.azure.storage.blob.models._
import fs2.{Chunk, Stream}
import reactor.core.publisher.Flux

object requests {

  final case class ListBlobHierarchyArgs(
    containerClient: BlobContainerAsyncClient,
    delimiter: String,
    options: ListBlobsOptions)

  def listRequest[F[_]: ConcurrentEffect: ContextShift](
      args: ListBlobHierarchyArgs)
    : F[Stream[F, BlobItem]] =
      Sync[F].delay(
        reactive.fluxToStream[F, BlobItem](
          args.containerClient.listBlobsByHierarchy(
            args.delimiter,
            args.options)))

  def listRequestK[F[_]: ConcurrentEffect: ContextShift]
      : Kleisli[F, ListBlobHierarchyArgs, Stream[F, BlobItem]] =
    Kleisli(listRequest[F])

  final case class BlobPropsArgs(
    blobClient: BlobAsyncClient,
    blobRequestConditions: BlobRequestConditions)

  def blobPropsRequest[F[_]: ConcurrentEffect: ContextShift](
      args: BlobPropsArgs)
      : F[Response[BlobProperties]] =
    Sync[F].delay(args.blobClient.getPropertiesWithResponse(args.blobRequestConditions)) >>=
      reactive.monoToAsync[F, Response[BlobProperties]]

  def blobPropsRequestK[F[_]: ConcurrentEffect: ContextShift]
      : Kleisli[F, BlobPropsArgs, Response[BlobProperties]] =
    Kleisli(blobPropsRequest[F])

  final case class DownloadArgs(blobClient: BlobAsyncClient)

  def downloadRequest[F[_]: ConcurrentEffect: ContextShift](
      args: DownloadArgs)
      : F[Stream[F, Byte]] =
    Sync[F].delay(args.blobClient.download())
      .map(b => reactive.fluxToStream[F, ByteBuffer](b))
      .map(_.flatMap(buf => Stream.chunk(Chunk.byteBuffer(buf))))

  def downloadRequestK[F[_]: ConcurrentEffect: ContextShift]: Kleisli[F, DownloadArgs, Stream[F, Byte]] =
    Kleisli(downloadRequest[F])

  final case class ContainerPropsArgs(
    containerClient: BlobContainerAsyncClient,
    leaseId: String)

  def containerPropsRequest[F[_]: ConcurrentEffect: ContextShift](
      args: ContainerPropsArgs): F[Response[BlobContainerProperties]] =
    Sync[F].delay(args.containerClient.getPropertiesWithResponse(args.leaseId)) >>=
      reactive.monoToAsync[F, Response[BlobContainerProperties]]

  def containerPropsRequestK[F[_]: ConcurrentEffect: ContextShift]
      : Kleisli[F, ContainerPropsArgs, Response[BlobContainerProperties]] =
    Kleisli(containerPropsRequest[F])

  final case class UploadRequestArgs(
    bytes: Flux[ByteBuffer],
    blobClient: BlobAsyncClient,
    parallelTransferOptions: ParallelTransferOptions,
    overwrite: Boolean)

  def uploadRequest[F[_]: ConcurrentEffect: ContextShift](args: UploadRequestArgs)
      : F[BlockBlobItem] =
    Sync[F].delay(
      args.blobClient.upload(args.bytes, args.parallelTransferOptions, args.overwrite)
    ) >>= reactive.monoToAsync[F, BlockBlobItem]

  def uploadRequestK[F[_]: ConcurrentEffect: ContextShift]
      : Kleisli[F, UploadRequestArgs, BlockBlobItem] =
    Kleisli(uploadRequest[F])
}
