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

import slamdata.Predef._

import cats.data.Kleisli
import cats.effect.{Async, Sync}
import cats.syntax.flatMap._
import com.microsoft.azure.storage.blob._
import com.microsoft.azure.storage.blob.models._
import com.microsoft.rest.v2.Context

object requests {

  final case class ListBlobHierarchyArgs(containerURL: ContainerURL, marker: Option[String], delimiter: String, options: ListBlobsOptions, context: Context)

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def listRequest[F[_]: Async](args: ListBlobHierarchyArgs): F[ContainerListBlobHierarchySegmentResponse] =
    Sync[F].delay(args.containerURL.listBlobsHierarchySegment(args.marker.orNull, args.delimiter, args.options, args.context)) >>=
      rx.singleToAsync[F, ContainerListBlobHierarchySegmentResponse]

  def listRequestK[F[_]: Async]: Kleisli[F, ListBlobHierarchyArgs, ContainerListBlobHierarchySegmentResponse] =
    Kleisli(listRequest[F])

  final case class BlobPropsArgs(blobURL: BlobURL, blobAccessConditions: BlobAccessConditions, context: Context)

  def blobPropsRequest[F[_]: Async](args: BlobPropsArgs): F[BlobGetPropertiesResponse] =
    Sync[F].delay(args.blobURL.getProperties(args.blobAccessConditions, args.context)) >>=
      rx.singleToAsync[F, BlobGetPropertiesResponse]

  def blobPropsRequestK[F[_]: Async]: Kleisli[F, BlobPropsArgs, BlobGetPropertiesResponse] =
    Kleisli(blobPropsRequest[F])

  final case class DownloadArgs(blobURL: BlobURL, blobRange: BlobRange, blobAccessConditions: BlobAccessConditions, rangeGetContentMD5: Boolean, context: Context)

  def downloadRequest[F[_]: Async](args: DownloadArgs): F[DownloadResponse] =
    Sync[F].delay(args.blobURL.download(args.blobRange, args.blobAccessConditions, args.rangeGetContentMD5, args.context)) >>=
      rx.singleToAsync[F, DownloadResponse]

  def downloadRequestK[F[_]: Async]: Kleisli[F, DownloadArgs, DownloadResponse] =
    Kleisli(downloadRequest[F])

  final case class ContainerPropsArgs(containerURL: ContainerURL, leaseAccessConditions: LeaseAccessConditions, context: Context)

  def containerPropsRequest[F[_]: Async](args: ContainerPropsArgs): F[ContainerGetPropertiesResponse] =
    Sync[F].delay(args.containerURL.getProperties(args.leaseAccessConditions, args.context)) >>=
      rx.singleToAsync[F, ContainerGetPropertiesResponse]

  def containerPropsRequestK[F[_]: Async]: Kleisli[F, ContainerPropsArgs, ContainerGetPropertiesResponse] =
    Kleisli(containerPropsRequest[F])

}
