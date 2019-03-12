package livongo.large_files_blog_post

import java.nio.file.{Files, Path}
import livongo.large_files_blog_post.common.{LineMetricsAccumulator, Result}
import cats.effect.{IO, Resource}
import fs2.Stream

object FS2Core extends FileReader {
  override def consume(path: Path): Result =
    Stream
      .resource(Resource.fromAutoCloseable(IO(Files.newBufferedReader(path))))
      .flatMap {
        Stream.unfold(_) { reader =>
          Option(reader.readLine()).map(_ -> reader)
        }
      }
      .compile
      .fold(LineMetricsAccumulator.empty)(_ addLine _)
      .map(_.asResult)
      .unsafeRunSync()

  override def description: String = "fs2-core"
}
