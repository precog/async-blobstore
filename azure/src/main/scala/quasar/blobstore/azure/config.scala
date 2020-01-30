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

import scala.{Int, Option, Product, Serializable}
import scala.Predef.String

import java.time.OffsetDateTime

import cats._

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Positive

final case class ContainerName(value: String)
final case class StorageUrl(value: String)

final case class AccountName(value: String)
final case class AccountKey(value: String)

final case class ClientId(value: String)
final case class ClientSecret(value: String)
final case class TenantId(value: String)

sealed trait AzureCredentials extends Product with Serializable

object AzureCredentials {
  final case class SharedKey(
    accountName: AccountName,
    accountKey: AccountKey) extends AzureCredentials

  final case class ActiveDirectory(
    clientId: ClientId,
    tenantId: TenantId,
    clientSecret: ClientSecret) extends AzureCredentials
}

final case class MaxQueueSize(value: Int Refined Positive)

object MaxQueueSize {
  def default: MaxQueueSize = MaxQueueSize(10)
}

final case class Expires[A](value: A, expiresAt: OffsetDateTime)

object Expires {
  def never[A](value: A): Expires[A] =
    Expires(value, OffsetDateTime.MAX)

  implicit def expiresFunctor: Functor[Expires] = new Functor[Expires] {
    def map[A, B](fa: Expires[A])(f: A => B): Expires[B] =
      Expires(f(fa.value), fa.expiresAt)
  }
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
