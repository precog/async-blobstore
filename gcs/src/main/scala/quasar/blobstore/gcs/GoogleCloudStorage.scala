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

import scala.{Array, Byte, Option}
import scala.Predef.String

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource}
import com.google.auth.oauth2.AccessToken
import org.http4s.Uri
import org.http4s.client.Client
import org.slf4s.Logging

object GoogleCloudStorage extends Logging {

  def mkContainerClient[F[_]: Concurrent: ConcurrentEffect: ContextShift](cfg: GoogleAuthConfig): Resource[F, Client[F]] =
    GCSClient(cfg)

  private def bucketUrl(bucket: Bucket) =
    Uri.unsafeFromString("https://storage.googleapis.com/storage/v1/b")
      .addSegment(bucket.value)

  def gcsStatusUrl(bucket: Bucket): Uri =
    bucketUrl(bucket)
      .addSegment("iam")

  def gcsDownloadUrl(bucket: Bucket, objectName: String): Uri =
    bucketUrl(bucket)
      .addSegment("o")
      .addSegment(objectName)
      .withQueryParam("alt", "media")

  def gcsListUrl(bucket: Bucket): Uri =
    bucketUrl(bucket)
      .addSegment("o")
      .withQueryParam("delimiter", "/")
      .withQueryParam("includeTrailingDelimiter", "true")

  def gcsPropsUrl(bucket: Bucket, objectName: String): Uri =
    bucketUrl(bucket)
      .addSegment("o")
      .addSegment(objectName)

  def getAccessToken[F[_]: Concurrent: ContextShift](auth: Array[Byte]): F[Option[AccessToken]] = GCSAccessToken.token(auth)
}
