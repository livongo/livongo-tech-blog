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
