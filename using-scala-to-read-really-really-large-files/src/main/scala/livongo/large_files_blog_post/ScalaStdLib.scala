package livongo.large_files_blog_post

import java.nio.file.Path
import livongo.large_files_blog_post.common.{LineMetricsAccumulator, Result}
import scala.io.{Codec, Source}

object ScalaStdLib extends FileReader {
  override def consume(path: Path): Result = {
    val source = Source.fromFile(path.toFile, 4096)(Codec.UTF8)
    try {
      source
        .getLines()
        .foldLeft(LineMetricsAccumulator.empty)(_ addLine _)
        .asResult
    } finally source.close()
  }

  override def description: String = "Scala StdLib"
}
