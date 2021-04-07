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

import scala.{Array, Byte}
import scala.Predef.String

import argonaut._, Argonaut._
import cats.implicits._

final case class ServiceAccountConfig(
    tokenUri: Url,
    authProviderCertUrl: Url,
    privateKey: String,
    clientId: String,
    clientCertUrl: Url,
    authUri: Url,
    projectId: String,
    privateKeyId: String,
    clientEmail: String,
    accountType: String) {

  val serviceAccountAuthBytes: Array[Byte] = this.asJson.toString.getBytes("UTF-8")
}

object ServiceAccountConfig {

  implicit val serviceAccountConfigCodecJson: CodecJson[ServiceAccountConfig] =
    casecodec10[String, String, String, String, String, String, String, String, String, String, ServiceAccountConfig](
      (tokenUri,
      authProviderCertUrl,
      privateKey,
      clientId,
      clientCertUrl,
      authUri,
      projectId,
      privateKeyId,
      clientEmail,
      accountType) => ServiceAccountConfig(
        tokenUri = Url(tokenUri),
        authProviderCertUrl = Url(authProviderCertUrl),
        privateKey = privateKey,
        clientId = clientId,
        clientCertUrl = Url(clientCertUrl),
        authUri = Url(authUri),
        projectId = projectId,
        privateKeyId = privateKeyId,
        clientEmail = clientEmail,
        accountType = accountType),
      sac =>
        (sac.tokenUri.value,
        sac.authProviderCertUrl.value,
        sac.privateKey,
        sac.clientId,
        sac.clientCertUrl.value,
        sac.authUri.value,
        sac.projectId,
        sac.privateKeyId,
        sac.clientEmail,
        sac.accountType).some)(
          "token_uri",
          "auth_provider_x509_cert_url",
          "private_key",
          "client_id",
          "client_x509_cert_url",
          "auth_uri",
          "project_id",
          "private_key_id",
          "client_email",
          "type")
}
