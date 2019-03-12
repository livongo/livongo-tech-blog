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
