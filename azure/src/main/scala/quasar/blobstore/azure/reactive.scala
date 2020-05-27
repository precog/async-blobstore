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

import cats.effect._
import fs2.Stream
import fs2.interop.reactivestreams._
import reactor.core.publisher.{Flux, Mono}
import reactor.core.scala.publisher.ScalaConverters._

object reactive {

  def streamToFlux[F[_]: ConcurrentEffect, A](s: Stream[F, A]): Flux[A] =
    Flux.from(s.toUnicastPublisher())

  def monoToAsync[F[_]: ContextShift, A](
      mono: Mono[A])(
      implicit F: Async[F])
      : F[A] =
    Async.fromFuture(F.delay(mono.asScala.toFuture))

  def fluxToStream[F[_]: ConcurrentEffect: ContextShift, A](
      flux: Flux[A])
      : Stream[F, A] =
    fromPublisher[F, A](flux)
}
