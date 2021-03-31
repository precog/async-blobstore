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

import quasar.blobstore.paths._

import scala.Boolean
import scala.Predef.{genericArrayOps, String}
import scala.collection.immutable.List
import scala.util.{Either, Left, Right}

import cats.syntax.functor._
import fs2._

object converters {

  def prefixPathToQueryParamValue(p: PrefixPath): String = {
    val s = p.path.map(_.value).mkString("/")
    if (s == "") s else s + "/"
  }

  def stripDelim(s: String): Either[String, String] =
    if (s.endsWith("/")) Right(s.substring(0, s.length - 1))
    else Left(s)

  def gcsFileToBlobstorePath(f: GCSFile): BlobstorePath =
    stripDelim(f.name) match {
      case Right(s) => PrefixPath(toPath(s))
      case Left(s) => BlobPath(toPath(s))
    }

  def gcsListingsToBlobstorePaths(l: GCSListings, exclude: BlobstorePath => Boolean): Stream[Pure, BlobstorePath] =
    Stream[Pure, BlobstorePath](l.list.flatMap { f =>
      val path = gcsFileToBlobstorePath(f)
      if (exclude(path)) List.empty[BlobstorePath]
      else List(path)
    }: _*)

  def toPath(s: String): Path =
    s.split("""/""").map(PathElem(_)).view.toList

  def blobPathToString(blobPath: BlobPath): String = {
    val names = blobPath.path.map(_.value)
    names.mkString("/")
  }

}
