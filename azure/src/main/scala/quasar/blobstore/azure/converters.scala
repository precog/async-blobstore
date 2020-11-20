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
import quasar.blobstore.paths._

import java.lang.{Integer, SuppressWarnings}
import scala.{Array, Option, Some}
import scala.Predef.{genericArrayOps, String}
import scala.StringContext

import cats.Applicative
import cats.data.Kleisli
import cats.effect.Sync
import cats.instances.string._
import cats.syntax.applicative._
import cats.syntax.eq._
import com.azure.core.http.rest.Response
import com.azure.storage.blob.{BlobAsyncClient, BlobContainerAsyncClient}
import com.azure.storage.blob.models.{BlobItem, BlobListDetails, ListBlobsOptions}
import fs2.Stream

object converters {

  def responseToBlobstoreStatus[A](response: Response[A]): BlobstoreStatus =
    response.getStatusCode() match {
      case 200 => BlobstoreStatus.ok()
      case 202 => BlobstoreStatus.ok()
      case 401 => BlobstoreStatus.noAccess()
      case 403 => BlobstoreStatus.noAccess()
      case 404 => BlobstoreStatus.notFound()
      case other => BlobstoreStatus.notOk(s"Azure returned status $other")
    }

  def responseToBlobstoreStatusK[F[_]: Applicative, A]: Kleisli[F, Response[A], BlobstoreStatus] =
    Kleisli(r => responseToBlobstoreStatus[A](r).pure[F])

  def blobPathToBlobClientK[F[_]: Sync](containerClient: BlobContainerAsyncClient): Kleisli[F, BlobPath, BlobAsyncClient] =
    Kleisli(mkBlobClient[F](containerClient))

  def prefixPathToListBlobOptionsK[F[_]: Applicative](details: Option[BlobListDetails], maxResults: Option[Integer])
      : Kleisli[F, PrefixPath, ListBlobsOptions] =
    Kleisli(p => mkListBlobsOptions(details, maxResults, Some(p)).pure[F])

  def toBlobstorePaths[F[_]](
      s: Stream[F, BlobItem])
      : Stream[F, BlobstorePath] =
    s.map(blobItemToBlobPath)

  def toBlobstorePathsK[F[_]: Applicative]
      : Kleisli[F, Stream[F, BlobItem], Stream[F, BlobstorePath]] =
    Kleisli(s => toBlobstorePaths(s).pure[F])

  def mkListBlobsOptions(
      details: Option[BlobListDetails],
      maxResults: Option[Integer],
      prefix: Option[PrefixPath])
      : ListBlobsOptions = {

    def updateMaxResults(o: ListBlobsOptions): ListBlobsOptions =
      maxResults.map(o.setMaxResultsPerPage).getOrElse(o)

    def updatePrefix(o: ListBlobsOptions): ListBlobsOptions =
      prefix.map(p => o.setPrefix(normalizePrefix((pathToPrefix(p))))).getOrElse(o)

    def updateDetails(o: ListBlobsOptions): ListBlobsOptions =
      details.map(o.setDetails).getOrElse(o)

    def update(o: ListBlobsOptions): ListBlobsOptions =
      updateMaxResults(updatePrefix(updateDetails(o)))

    update(new ListBlobsOptions())
  }

  private def pathToPrefix(prefix: PrefixPath): String = {
    val names = prefix.path.map(_.value)
    val s = names.mkString("", "/", "/")
    if (s === "/") "" else s
  }

  def mkBlobClient[F[_]: Sync](containerClient: BlobContainerAsyncClient)(path: BlobPath): F[BlobAsyncClient] =
    Sync[F].delay(containerClient.getBlobAsyncClient(blobPathToString(path)))

  private def blobPathToString(blobPath: BlobPath): String = {
    val names = blobPath.path.map(_.value)
    names.mkString("/")
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def normalizePrefix(name: String): String =
    if (name.endsWith("//")) normalizePrefix(name.substring(0, name.length - 1))
    else name

  def blobItemToBlobPath(blobItem: BlobItem): BlobstorePath = {
    val isPrefix =
      if (blobItem.isPrefix() == null) false
      else blobItem.isPrefix().booleanValue()
    if (isPrefix) PrefixPath(toPath(blobItem.getName))
    else BlobPath(toPath(blobItem.getName))
  }

  def toPath(s: String): Path =
    s.split("""/""").map(PathElem(_)).view.toList
}
