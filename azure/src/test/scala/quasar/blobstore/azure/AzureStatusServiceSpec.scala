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

import quasar.blobstore.BlobstoreStatus
import quasar.blobstore.azure.fixtures._
import quasar.blobstore.services.StatusService

import cats.effect.IO
import cats.effect.testing.specs2.CatsIO
import org.specs2.mutable.Specification

class AzureStatusServiceSpec extends Specification with CatsIO {

  def mkService(cfg: Config): IO[StatusService[IO]] =
    Azure.mkContainerClient[IO](cfg) map (c => AzureStatusService.mk[IO](c.value))

  def assertStatus(service: IO[StatusService[IO]], status: BlobstoreStatus) =
    service flatMap { svc =>
      svc.map(_ must_=== status)
    }

  def assertStatusNotOk(service: IO[StatusService[IO]]) =
    service flatMap { svc =>
      svc.map(_ must beLike {
        case BlobstoreStatus.notOk(_) => ok
      })
    }

  "status service" >> {

    "valid, accessible, public container returns ok" >> {
      assertStatus(mkService(PublicConfig), BlobstoreStatus.ok())
    }

    "non existing container returns not found" >> {
      assertStatus(mkService(NonExistingConfig), BlobstoreStatus.notFound())
    }

    "wrong credentials returns no access" >> {
      assertStatus(mkService(NoAccessConfig), BlobstoreStatus.noAccess())
    }

    "invalid config returns not ok" >> {
      assertStatusNotOk(mkService(InvalidConfig))
    }
  }
}
