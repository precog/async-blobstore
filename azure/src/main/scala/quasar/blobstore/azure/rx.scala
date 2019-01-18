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

import java.lang.{SuppressWarnings, Throwable}

import scala.{Array, Int, Option, Some, Unit}
import scala.util.{Either, Left, Right}

import cats.effect._
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import fs2.{RaiseThrowable, Stream}
import fs2.concurrent.Queue
import io.reactivex.{Flowable, Single, SingleObserver}
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.subscribers.DefaultSubscriber

object rx {
  final class AsyncSubscriber[F[_]: Sync, A](cb: Either[Throwable, Option[F[A]]] => Unit) extends DefaultSubscriber[A] {

    override def onStart(): Unit =
      request(1)

    def onNext(a: A): Unit =
      cb(Right(Some(Sync[F].delay(request(1)).as(a))))

    def onError(t: Throwable): Unit =
      cb(Left(t))

    def onComplete: Unit =
      cb(Right(none))
  }

  final class AsyncObserver[A] extends DisposableSingleObserver[A] {

    @SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var"))
    private var callback: Either[Throwable, A] => Unit = _

    def setCallback(cb: Either[Throwable, A] => Unit): Unit = {
      this.callback = cb
    }

    override def onStart(): Unit = ()

    @SuppressWarnings(Array("org.wartremover.warts.Equals"))
    override def onSuccess(a: A) =
      if (callback != null) callback(Right(a))
      else ()

    @SuppressWarnings(Array("org.wartremover.warts.Equals"))
    override def onError(t: Throwable) =
      if (callback != null) callback(Left(t))
      else ()
  }

  def flowableToStream[F[_]: ConcurrentEffect: RaiseThrowable, A](
      f: Flowable[A],
      maxQueueSize: Int Refined Positive): Stream[F, A] =
    handlerToStream(flowableToHandler(f), maxQueueSize)

  def flowableToHandler[F[_]: Sync, A](flowable: Flowable[A]): (Either[Throwable, Option[F[A]]] => Unit) => F[Unit] = { cb =>
    val sub = new AsyncSubscriber[F, A](cb)
    Sync[F].delay(flowable.subscribe(sub))
  }

  def handlerToStream[F[_], A](
      handler: (Either[Throwable, Option[F[A]]] => Unit) => F[Unit],
      maxQueueSize: Int Refined Positive)(
      implicit F: ConcurrentEffect[F]): Stream[F, A] =
    (for {
      q <- Stream.eval(Queue.bounded[F, Either[Throwable, Option[F[A]]]](maxQueueSize.value))
      _ <- Stream.eval(handler(enqueueEvent(q)))
      a <- q.dequeue.rethrow.unNoneTerminate
    } yield Stream.eval(a)).flatten

  def enqueueEvent[F[_]: Effect, A](q: Queue[F, A])(event: A): Unit =
    Effect[F].runAsync(q.enqueue1(event))(_ => IO.unit).unsafeRunSync

  def mkAsync[F[_], A](
      observer: AsyncObserver[A], f: SingleObserver[A] => Unit)(
      implicit F: Async[F]): F[A] =
    F.async[A] { cb: (Either[Throwable, A] => Unit) =>
      observer.setCallback(cb)
      f(observer)
    }

  def singleToAsync[F[_], A](
      single: Single[A])(
      implicit F: Async[F]): F[A] =
    F.bracket(
      F.delay(new AsyncObserver[A]))(
      obs => mkAsync[F, A](obs, single.subscribe))(
      obs => F.delay(obs.dispose()))

}
