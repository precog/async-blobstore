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

import scala.Predef.String
import scala.util.{Either, Left, Right}

import quasar.blobstore.paths._
import scala.collection.immutable.List
import fs2._

object converters {
  def stripDelim(s: String): Either[String, String] =
    if (s.endsWith("/")) Right(s.substring(0, s.length - 1))
    else Left(s)

  def gcsFileToBlobstorePath(f: GCSFile): BlobstorePath =
    stripDelim(f.name) match {
      case Right(s) => PrefixPath(List(PathElem(s)))
      case Left(s) => BlobPath(List(PathElem(s)))
    }

  def gcsListingsToBlobstorePaths(l: GCSListings): Stream[Pure, BlobstorePath] =
    Stream[Pure, BlobstorePath](l.list.map(gcsFileToBlobstorePath): _*)
}
