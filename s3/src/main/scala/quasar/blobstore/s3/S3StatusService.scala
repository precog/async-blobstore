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

import quasar.blobstore.services.StatusService

import cats.effect.syntax.bracket._
import cats.effect.{ContextShift, Concurrent}
import cats.implicits._

import monix.catnap.syntax._

import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadBucketRequest

object S3StatusService {
  def apply[F[_]: Concurrent: ContextShift](
    client: S3AsyncClient,
    bucket: Bucket): StatusService[F] =
    Concurrent[F].delay(
      client.headBucket(HeadBucketRequest.builder.bucket(bucket.value).build))
      .futureLift
      .guarantee(ContextShift[F].shift)
      .map(r => converters.responseToStatus(r.sdkHttpResponse))
}
