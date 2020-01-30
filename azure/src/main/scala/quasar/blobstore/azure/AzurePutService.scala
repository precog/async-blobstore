/*
 * Copyright 2014–2020 SlamData Inc.
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

import scala._

import quasar.blobstore.azure.requests.UploadRequestArgs
import quasar.blobstore.paths.BlobPath
import quasar.blobstore.services.PutService

import com.microsoft.azure.storage.blob._

import java.nio.ByteBuffer

import cats.effect.{ContextShift, ConcurrentEffect}
import cats.data.Kleisli
import fs2.Stream

object AzurePutService {
  private val ChunkLength = 10 * 1024 * 1024
  private val Buffers = 2

  def apply[F[_]: ContextShift: ConcurrentEffect](
    containerURL: ContainerURL,
    blockSize: Int,
    numBuffers: Int,
    transferOptions: TransferManagerUploadToBlockBlobOptions)
      : PutService[F] =
    for {
      (blobPath, st) <- Kleisli.ask[F, (BlobPath, Stream[F, Byte])]
      url <- Kleisli.liftF(converters.mkBlobUrl(containerURL)(blobPath))
      blockURL = url.toBlockBlobURL
      flowable = rx.streamToFlowable[F, ByteBuffer](st.chunks.map(_.toByteBuffer))
      args = UploadRequestArgs(flowable, blockURL, blockSize, numBuffers, transferOptions)
      upload <- Kleisli.liftF(requests.uploadRequest(args))
    } yield upload.statusCode

  def mk[F[_]: ConcurrentEffect: ContextShift](containerURL: ContainerURL)
      : PutService[F] =
    AzurePutService(
      containerURL,
      ChunkLength,
      Buffers,
      TransferManagerUploadToBlockBlobOptions.DEFAULT)
}
