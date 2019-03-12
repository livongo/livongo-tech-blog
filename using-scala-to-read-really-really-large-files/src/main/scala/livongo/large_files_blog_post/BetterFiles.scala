package livongo.large_files_blog_post

import java.nio.file.Path
import livongo.large_files_blog_post.common.{LineMetricsAccumulator, Result}
import better.files._

object BetterFiles extends FileReader {
  override def consume(path: Path): Result =
    path.toFile.toScala.lineIterator.foldLeft(LineMetricsAccumulator.empty)(_ addLine _).asResult

  override def description: String = "better-files"
}
