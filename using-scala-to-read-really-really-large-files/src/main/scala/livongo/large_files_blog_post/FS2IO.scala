package livongo.large_files_blog_post

import java.nio.file.Path
import java.util.concurrent.Executors
import livongo.large_files_blog_post.common.{LineMetricsAccumulator, Result}
import cats.effect.{ContextShift, IO, Resource}
import fs2.{Stream, io, text}
import scala.concurrent.ExecutionContext

object FS2IO extends FileReader {
  override def consume(path: Path): Result = {
    implicit val executionContext: ExecutionContext = ExecutionContext.global
    implicit val contextShift:     ContextShift[IO] = IO.contextShift(executionContext)
    val readingExecutionContext =
      Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))))(ec => IO(ec.shutdown()))

    Stream
      .resource(readingExecutionContext)
      .flatMap { EC =>
        io.file
          .readAll[IO](path, EC, 4096)
          .through(text.utf8Decode)
          .through(text.lines)
      }
      .filter(_.nonEmpty)
      .compile
      .fold(LineMetricsAccumulator.empty)(_ addLine _)
      .map(_.asResult)
      .unsafeRunSync()
  }

  override def description: String = "fs2-io"
}
