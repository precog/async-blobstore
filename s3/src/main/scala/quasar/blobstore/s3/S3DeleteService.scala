/*
 * Copyright 2014–2019 SlamData Inc.
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

package quasar.blobstore.s3

import scala._

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.DeleteService

import cats.data.Kleisli
import cats.effect.{ContextShift, Concurrent, Timer}
import cats.effect.syntax.bracket._
import cats.implicits._

import monix.catnap.syntax._

import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  DeleteObjectResponse,
  DeleteObjectRequest
}

object S3DeleteService {
  def apply[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    bucket: Bucket): DeleteService[F] =
    for {
      blobPath <- Kleisli.ask[F, BlobPath]
      objectKey = ObjectKey(blobPath.path.map(_.value).intercalate("/"))
      response <- Kleisli.liftF(deleteObject(client, bucket, objectKey))
      statusCode = response.sdkHttpResponse.statusCode
      statusText = response.sdkHttpResponse.statusText
    } yield statusCode match {
      case 200 => BlobstoreStatus.Ok
      case 404 => BlobstoreStatus.NotFound
      case 403 => BlobstoreStatus.NoAccess
      case other => BlobstoreStatus.NotOk(s"Status $other: $statusText")
    }

  private def deleteObject[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    bucket: Bucket,
    key: ObjectKey): F[DeleteObjectResponse] =
    Concurrent[F].delay(
      client.deleteObject(
        DeleteObjectRequest
          .builder
          .bucket(bucket.value)
          .key(key.value)
          .build))
      .futureLift
      .guarantee(ContextShift[F].shift)
}
