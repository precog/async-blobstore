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

import quasar.blobstore.azure.requests.UploadRequestArgs
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.PutService

import java.nio.ByteBuffer
import scala._

import cats.data.Kleisli
import cats.effect.{ContextShift, ConcurrentEffect}
import cats.syntax.functor._
import fs2.Stream
import com.azure.storage.blob.{BlobAsyncClient, BlobContainerAsyncClient}
import com.azure.storage.blob.models.ParallelTransferOptions
import reactor.core.publisher.Flux

object AzurePutService {

  def apply[F[_]: ContextShift: ConcurrentEffect](
      containerClient: BlobContainerAsyncClient,
      mkArgs: (Flux[ByteBuffer], BlobAsyncClient) => UploadRequestArgs)
      : PutService[F] =
    for {
      (blobPath, st) <- Kleisli.ask[F, (BlobPath, Stream[F, Byte])]
      blobClient <- Kleisli.liftF(converters.mkBlobClient(containerClient)(blobPath))
      flux = reactive.streamToFlux[F, ByteBuffer](st.chunks.map(_.toByteBuffer))
      args = mkArgs(flux, blobClient)
      statusCode <- Kleisli.liftF(requests.uploadRequest[F](args).map(_ => 200))
    } yield statusCode


  def mk[F[_]: ConcurrentEffect: ContextShift](containerClient: BlobContainerAsyncClient)
      : PutService[F] =
    AzurePutService(
      containerClient,
      (flux, blobClient) =>
        UploadRequestArgs(
          flux,
          blobClient,
          new ParallelTransferOptions(),
          overwrite = true))
}
