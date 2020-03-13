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

package quasar.blobstore.s3

import scala._
import scala.Predef._

import scala.concurrent.duration._

import quasar.blobstore.services.PutService
import quasar.blobstore.paths.BlobPath

import cats.data.Kleisli
import cats.effect.syntax.bracket._
import cats.effect.{ContextShift, Concurrent, ExitCase, Timer}
import cats.implicits._

import fs2.Stream

import monix.catnap.syntax._

import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.{
  AbortMultipartUploadRequest,
  AbortMultipartUploadResponse,
  CompleteMultipartUploadRequest,
  CompleteMultipartUploadResponse,
  CompletedMultipartUpload,
  CompletedPart,
  CreateMultipartUploadRequest,
  CreateMultipartUploadResponse,
  UploadPartRequest
}

object S3PutService {
  def apply[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    partSize: Int,
    bucket: Bucket): PutService[F] = Kleisli {
    case (blobPath, bytes) =>
      upload(client, partSize, bytes, bucket, blobPathToObjectKey(blobPath))
  }

  private def upload[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    partSize: Int,
    bytes: Stream[F, Byte],
    bucket: Bucket,
    key: ObjectKey)
      : F[Int] =
    Concurrent[F].bracketCase(startUpload(client, bucket, key))(createResponse =>
      for {
        parts <- uploadParts(client, bytes, createResponse.uploadId, partSize, bucket, key).compile.lastOrError
        completeResponse <- completeUpload(client, createResponse.uploadId, bucket, key, parts)
        statusCode = completeResponse.sdkHttpResponse.statusCode
      } yield statusCode) {
      case (createResponse, ExitCase.Canceled | ExitCase.Error(_)) =>
        abortUpload(client, createResponse.uploadId, bucket, key).void
      case (_, ExitCase.Completed) =>
        Concurrent[F].unit
    }

  private def startUpload[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    bucket: Bucket,
    key: ObjectKey): F[CreateMultipartUploadResponse] =
    Concurrent[F]
      .delay(
        client.createMultipartUpload(
          CreateMultipartUploadRequest.builder.bucket(bucket.value).key(key.value).build))
      .futureLift
      .guarantee(ContextShift[F].shift)

  private def uploadParts[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    bytes: Stream[F, Byte],
    uploadId: String,
    minChunkSize: Int,
    bucket: Bucket,
    key: ObjectKey): Stream[F, List[CompletedPart]] =
    (bytes.chunkMin(minChunkSize).zipWithIndex flatMap {
      case (byteChunk, n) => {
        // parts numbers must start at 1
        val partNumber = n.toInt + 1

        val uploadPartRequest =
          UploadPartRequest.builder
            .bucket(bucket.value)
            .uploadId(uploadId)
            .key(key.value)
            .partNumber(partNumber)
            .contentLength(byteChunk.size.toLong)
            .build

        val uploadPartResponse =
          Concurrent[F]
            .delay(
              client.uploadPart(
                uploadPartRequest,
                AsyncRequestBody.fromByteBuffer(byteChunk.toByteBuffer)))
            .futureLift

        // exponential backoff
        Stream.retry(uploadPartResponse, 500.milliseconds, (_ * 2), 5)
          .map(response =>
            CompletedPart.builder.partNumber(partNumber).eTag(response.eTag).build)
      }
    }).foldMap(_ :: Nil)

  private def completeUpload[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    uploadId: String,
    bucket: Bucket,
    key: ObjectKey,
    parts: List[CompletedPart]): F[CompleteMultipartUploadResponse] = {
    val multipartUpload =
      CompletedMultipartUpload.builder.parts(parts: _*).build

    val completeMultipartUploadRequest =
      CompleteMultipartUploadRequest.builder
        .bucket(bucket.value)
        .key(key.value)
        .uploadId(uploadId)
        .multipartUpload(multipartUpload)
        .build

    Concurrent[F]
      .delay(client.completeMultipartUpload(completeMultipartUploadRequest))
      .futureLift
      .guarantee(ContextShift[F].shift)
  }

  private def abortUpload[F[_]: Concurrent: ContextShift: Timer](
    client: S3AsyncClient,
    uploadId: String,
    bucket: Bucket,
    key: ObjectKey): F[AbortMultipartUploadResponse] =
    Concurrent[F]
      .delay(
        client.abortMultipartUpload(
          AbortMultipartUploadRequest.builder
            .uploadId(uploadId)
            .bucket(bucket.value)
            .key(key.value)
            .build))
      .futureLift
      .guarantee(ContextShift[F].shift)

  private def blobPathToObjectKey(blobPath: BlobPath): ObjectKey =
    ObjectKey(
      blobPath.path.map(_.value).intercalate("/"))
}
