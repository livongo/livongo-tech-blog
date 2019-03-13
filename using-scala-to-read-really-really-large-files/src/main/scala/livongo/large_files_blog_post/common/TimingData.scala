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
package livongo.large_files_blog_post.common

import java.time.{Duration, Instant}

/**
  * Simplifies storage and formatting of timing data.
  *
  * While creating these mini-classes is often helpful when you want better names for
  * accessing things than [[scala.Tuple2#_1]], in this case it's mostly used to make
  * it easier to print.
  */
final case class TimingData(start: Instant, end: Instant) {
  val duration: Duration = Duration.between(start, end)

  def toMills:       Long   = duration.toMillis
  def iso8601Format: String = duration.toString

  override def toString: String = s"${toMills}ms ($iso8601Format)"
}

object TimingData {

  /**
    * Executes a block and records timing data.
    *
    * @param block By-name parameter won't be evaluated until it's used in the method body
    * @tparam T The return type of the block
    * @return A tuple containing the value of the block and timing data
    */
  def timed[T](block: => T): (T, TimingData) = {
    val start  = Instant.now()
    val result = block
    val end    = Instant.now()
    (result, TimingData(start, end))
  }
}
