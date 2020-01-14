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

import scala.Predef._

final case class Bucket(value: String)

final case class AccessKey(value: String)
final case class SecretKey(value: String)
final case class Region(value: String)

final case class ObjectKey(value: String)

final case class S3Credentials(accessKey: AccessKey, secretKey: SecretKey, region: Region)

final case class S3Config(bucket: Bucket, credentials: S3Credentials)

