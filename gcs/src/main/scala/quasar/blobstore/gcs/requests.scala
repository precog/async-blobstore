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

// import scala.Option
// import scala.Predef.String

// import quasar.blobstore.paths.PrefixPath

// import cats.data.Kleisli
// import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Sync}
// import cats.Applicative

// import org.http4s.{Response, Request}
// import org.http4s.client.Client

// import fs2.Stream

// import java.lang.{Integer}

object requests {

  // type BucketStatus = String

  // final case class ContainerPropsArgs[F[_]](
  //   client: Resource[F, Client[F]],
  //   request: Request[F])

  // def containerPropsRequest[F[_]: Concurrent: ContextShift](args: ContainerPropsArgs[F]): F[Response[F]] = scala.Predef.???
  //   //Sync[F].delay(args.client.map(c => c.)
  //   // Sync[F].delay(args.client.getPropertiesWithResponse(args.leaseId)) >>=
  //   //   reactive.monoToAsync[F, Response[BlobContainerProperties]]

  // def containerPropsRequestK[F[_]: Concurrent: ContextShift]
  //     : Kleisli[F, ContainerPropsArgs[F], Response[F]] =
  //   Kleisli(containerPropsRequest[F])


  // final case class BlobItem(name: String)
  // final case class ListBlobsOptions(delimiter: Option[PrefixPath], maxResultsPerPage: Option[Integer])

  // final case class ListBlobHierarchyArgs[F[_]](
  //   containerClient: Client[F],
  //   delimiter: String,
  //   options: ListBlobsOptions,
  //   bucket: Bucket,
  //   config: GoogleAuthConfig)

  // def listRequest[F[_]: ConcurrentEffect: ContextShift](
  //     args: ListBlobHierarchyArgs[F])
  //   : F[Stream[F, BlobItem]] =
  //     Sync[F].delay(
  //       reactive.fluxToStream[F, BlobItem](
  //         GoogleCloudStorage.mkContainerClient[F].map(client => GCSListService(client, args.bucket, args.config))))
  //         // args.containerClient.listBlobsByHierarchy(
  //         //   args.delimiter,
  //         //   args.options)))

  // def listRequestK[F[_]: ConcurrentEffect: ContextShift: Applicative]
  //     : Kleisli[F, ListBlobHierarchyArgs[F], Stream[F, BlobItem]] =
  //   Kleisli(listRequest[F])


}