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

package quasar.blobstore.azure

import scala.{None, Some}

object fixtures {

  val AccountNameSlamdata = AccountName("slamdata")
  val StorageUrlSlamdata = Azure.mkStdStorageUrl(AccountNameSlamdata)

  val PublicConfig =
    new Config {
      val containerName = ContainerName("test")
      val credentials = None
      val storageUrl = StorageUrlSlamdata
      val maxQueueSize = None
    }

  val NonExistingConfig =
    new Config {
      val containerName = ContainerName("doesnotexist")
      val credentials = None
      val storageUrl = StorageUrlSlamdata
      val maxQueueSize = None
    }

  val NoAccessConfig =
    new Config {
      val containerName = ContainerName("doesnotexist")
      val credentials = Some(AzureCredentials.SharedKey(AccountNameSlamdata, AccountKey("nope")))
      val storageUrl = StorageUrlSlamdata
      val maxQueueSize = None
    }

    val InvalidConfig =
    new Config {
      val containerName = ContainerName("doesnotexist")
      val credentials = Some(AzureCredentials.SharedKey(AccountName(""), AccountKey("")))
      val storageUrl = StorageUrlSlamdata
      val maxQueueSize = None
    }
}
