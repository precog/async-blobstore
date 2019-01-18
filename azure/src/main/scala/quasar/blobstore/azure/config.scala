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

import slamdata.Predef._

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Positive

final case class ContainerName(value: String)
final case class StorageUrl(value: String)

final case class AccountName(value: String)
final case class AccountKey(value: String)

final case class AzureCredentials(accountName: AccountName, accountKey: AccountKey)

final case class MaxQueueSize(value: Int Refined Positive)

object MaxQueueSize {
  def default: MaxQueueSize = MaxQueueSize(10)
}

trait Config {
  def containerName: ContainerName
  def credentials: Option[AzureCredentials]
  def storageUrl: StorageUrl
  def maxQueueSize: Option[MaxQueueSize]
}

final case class DefaultConfig(
    override val containerName: ContainerName,
    override val credentials: Option[AzureCredentials],
    override val storageUrl: StorageUrl,
    override val maxQueueSize: Option[MaxQueueSize]) extends Config