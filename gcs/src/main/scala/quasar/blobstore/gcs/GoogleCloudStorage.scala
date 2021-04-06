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

import java.lang.RuntimeException
import scala.{Array, Byte, Option}
import scala.Predef.String

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource}
import com.google.auth.oauth2.AccessToken
import org.http4s.Uri
import org.http4s.client.Client
import org.slf4s.Logging

object GoogleCloudStorage extends Logging {

  final case class GCSAccessError(message: String) extends RuntimeException(message)

  def mkContainerClient[F[_]: Concurrent: ConcurrentEffect: ContextShift](cfg: GoogleAuthConfig): Resource[F, Client[F]] =
    GCSClient(cfg)

  def gcsStatusUrl(bucket: Bucket): Uri = {
    //TODO: fix this
    val statusUri = Uri.fromString("https://storage.googleapis.com/storage/v1/b/"+ bucket.value +"/iam").toOption.get
    statusUri
  }

  def gcsDownloadUrl(bucket: Bucket, objectName: String): Uri = {
    //TODO: fix this
    val downloadUri = Uri.fromString("https://storage.googleapis.com/storage/v1/b/" + bucket.value + "/o/" + objectName + "?alt=media").toOption.get
    downloadUri
  }

  def gcsListUrl(bucket: Bucket): Uri = {
    //TODO: fix this
    val listUri = Uri.unsafeFromString("https://storage.googleapis.com/storage/v1/b/" + bucket.value + "/o?delimiter=/&includeTrailingDelimiter=true") //.toOption.get
    listUri
  }

  def gcsGetUrl(bucket: Bucket, objectName: String): Uri = {
    //TODO: fix this
    val listUri = Uri.fromString("https://storage.googleapis.com/storage/v1/b/" + bucket.value + "/o/" + objectName + "?alt=media").toOption.get
    listUri
  }

  def gcsPropsUrl(bucket: Bucket, objectName: String): Uri = {
    //TODO: fix this
    val listUri = Uri.fromString("https://storage.googleapis.com/storage/v1/b/" + bucket.value + "/o/" + objectName).toOption.get
    listUri
  }

  def getAccessToken[F[_]: Concurrent: ContextShift](auth: Array[Byte]): F[Option[AccessToken]] = GCSAccessToken.token(auth)
}
