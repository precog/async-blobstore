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

import quasar.blobstore.paths._

import java.lang.{Integer, SuppressWarnings}

import scala.{Array, Option, Some}
import scala.Predef.{genericArrayOps, String}

import cats.Applicative
import cats.data.Kleisli
import cats.effect.Sync
import cats.instances.string._
import cats.syntax.applicative._
import cats.syntax.eq._
import com.microsoft.azure.storage.blob.models.{BlobItem, BlobPrefix, ContainerListBlobHierarchySegmentResponse}
import com.microsoft.azure.storage.blob.{BlobListingDetails, BlobURL, ContainerURL, ListBlobsOptions}
import fs2.Stream

object converters {

  def blobPathToBlobURLK[F[_]: Sync](containerURL: ContainerURL): Kleisli[F, BlobPath, BlobURL] =
    Kleisli(mkBlobUrl[F](containerURL))

  def prefixPathToListBlobOptionsK[F[_]: Applicative](details: Option[BlobListingDetails], maxResults: Option[Integer])
      : Kleisli[F, PrefixPath, ListBlobsOptions] =
    Kleisli(p => mkListBlobsOptions(details, maxResults, Some(p)).pure[F])

  def toBlobstorePathsK[F[_]: Applicative]
      : Kleisli[F, ContainerListBlobHierarchySegmentResponse, Option[Stream[F, BlobstorePath]]] =
    Kleisli(toBlobstorePaths[F](_).pure[F])


  def mkListBlobsOptions(
      details: Option[BlobListingDetails],
      maxResults: Option[Integer],
      prefix: Option[PrefixPath])
      : ListBlobsOptions = {

    def updateMaxResults(o: ListBlobsOptions): ListBlobsOptions = maxResults.map(o.withMaxResults).getOrElse(o)
    def updatePrefix(o: ListBlobsOptions): ListBlobsOptions = prefix.map(p => o.withPrefix(normalizePrefix((pathToPrefix(p))))).getOrElse(o)
    def updateDetails(o: ListBlobsOptions): ListBlobsOptions = details.map(o.withDetails).getOrElse(o)
    def update(o: ListBlobsOptions): ListBlobsOptions = updateMaxResults(updatePrefix(updateDetails(o)))

    update(new ListBlobsOptions())
  }

  private def pathToPrefix(prefix: PrefixPath): String = {
    val names = prefix.path.map(_.value)
    val s = names.mkString("", "/", "/")
    if (s === "/") "" else s
  }

  def mkBlobUrl[F[_]: Sync](containerURL: ContainerURL)(path: BlobPath): F[BlobURL] =
    Sync[F].delay(containerURL.createBlobURL(blobPathToString(path)))

  private def blobPathToString(blobPath: BlobPath): String = {
    val names = blobPath.path.map(_.value)
    names.mkString("/")
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def normalizePrefix(name: String): String =
    if (name.endsWith("//")) normalizePrefix(name.substring(0, name.length - 1))
    else name

  def toBlobstorePaths[F[_]](r: ContainerListBlobHierarchySegmentResponse)
      : Option[Stream[F, BlobstorePath]] = {
    import scala.collection.JavaConverters._

    Option(r.body.segment).map { segm =>
      val l = segm.blobItems.asScala.map(blobItemToBlobPath) ++
        segm.blobPrefixes.asScala.map(blobPrefixToPrefixPath)
      Stream.emits(l).covary[F]
    }
  }

  def blobItemToBlobPath(blobItem: BlobItem): BlobstorePath =
    BlobPath(toPath(blobItem.name))

  def blobPrefixToPrefixPath(blobPrefix: BlobPrefix): BlobstorePath =
    PrefixPath(toPath(blobPrefix.name))

  def toPath(s: String): Path =
    s.split("""/""").map(PathElem(_)).toList
}
