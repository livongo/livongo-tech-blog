/* Copyright (C) 2019 Livongo Corporation - All Rights Reserved
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
package livongo.large_files_blog_post

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString
import livongo.large_files_blog_post.common.{LineMetricsAccumulator, Result}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Akka extends FileReader {
  override def consume(path: Path): Result = {
    implicit val executionContext:  ExecutionContext  = ExecutionContext.global
    implicit val actorSystem:       ActorSystem       = ActorSystem()
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

    try {
      Await.result(
        FileIO
          .fromPath(path)
          .via(Framing.delimiter(ByteString("\n"), 512).map(_.utf8String))
          .runFold(LineMetricsAccumulator.empty)(_ addLine _)
          .map(_.asResult),
        Duration.Inf
      )
    } finally {
      actorMaterializer.shutdown()
      Await.result(actorSystem.terminate(), Duration.Inf)
      ()
    }
  }

  override def description: String = "Akka Streams"
}
