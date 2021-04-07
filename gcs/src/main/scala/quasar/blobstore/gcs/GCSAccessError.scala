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

import java.lang.RuntimeException
import scala.Predef.String

import argonaut._, Argonaut._
import cats.effect.Sync
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.argonaut._

final case class GCSAccessError(message: String) extends RuntimeException(message)

object GCSAccessError {
  implicit def gcsAccessErrorEntityDecoder[F[_]: Sync]: EntityDecoder[F, GCSAccessError] = jsonOf[F, GCSAccessError]
  implicit def gcsAccessErrorEntityEncoder[F[_]: Sync]: EntityEncoder[F, GCSAccessError] = jsonEncoderOf[F, GCSAccessError]

  implicit val codecJsonGCSListings: CodecJson[GCSAccessError] = CodecJson(
    {(gae: GCSAccessError) =>
      ("message" := gae.getMessage) ->: jEmptyObject
    }, {j => {
        for {
          message <- (j --\ "error" --\ "message").as[String]
        } yield GCSAccessError(message)
      }
    })
}
