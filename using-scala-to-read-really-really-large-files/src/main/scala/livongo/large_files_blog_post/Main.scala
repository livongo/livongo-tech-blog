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

import java.lang.management.{ManagementFactory, MemoryType}
import java.nio.file.Path

import livongo.large_files_blog_post.common._

import scala.collection.JavaConverters._

trait FileReader {
  def description: String

  def consume(path: Path): Result
}

object Main extends App {
  Arguments.parse(args).foreach {
    case Arguments.PrintHeaders =>
      println("library,execution time in ms,peak memory usage in bytes")

    case Arguments.PrintLibraries =>
      Implementation.values.map(_.entryName).foreach(println)

    case Arguments.ReadFile(implementation, target, verbose) =>
      val reader = implementation.reader
      if (verbose) {
        println(s"Using ${reader.description}")
      }

      val (result, timingData) =
        TimingData.timed {
          reader.consume(target)
        }

      val peakMemory =
        ManagementFactory.getMemoryPoolMXBeans.asScala
          .filter(_.getType == MemoryType.HEAP)
          .foldLeft(0L)(_ + _.getPeakUsage.getUsed)

      if (verbose) {
        result.print(timingData)
      } else {
        println(s"${reader.description},${timingData.toMills},$peakMemory")
      }

    case invalidArgs =>
      Console.err.println(s"Invalid arguments: $invalidArgs")
      Arguments.showUsageAsError()
      sys.exit(1)
  }
}
