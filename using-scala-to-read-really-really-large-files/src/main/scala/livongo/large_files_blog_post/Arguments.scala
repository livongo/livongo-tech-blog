package livongo.large_files_blog_post

import java.nio.file.{Path, Paths}

import cats.syntax.option._
import com.livongo.large_files_blog_post.JavaStdLib

import scala.collection.immutable

sealed abstract class Implementation(val reader: FileReader)
    extends enumeratum.EnumEntry
    with enumeratum.EnumEntry.Hyphencase

object Implementation extends enumeratum.Enum[Implementation] {
  case object ScalaStdlib extends Implementation(ScalaStdLib)
  case object akkaStreams extends Implementation(Akka)
  case object fs2Core extends Implementation(FS2Core)
  case object fs2Io extends Implementation(FS2IO)
  case object betterFiles extends Implementation(BetterFiles)
  case object javaStdlib extends Implementation(JavaStdLib.implementation)

  override def values: immutable.IndexedSeq[Implementation] = findValues

  implicit val reads: scopt.Read[Implementation] = scopt.Read.reads(Implementation.withNameInsensitive)
}

sealed trait Arguments {
  def asPrintHeaders: Arguments = Arguments.PrintHeaders

  def asPrintLibraries: Arguments = Arguments.PrintLibraries

  def asReadFileIfLegal: Arguments = this match {
    case Arguments.NoArgs                         => Arguments.PartialReadFile(None, None)
    case prf @ Arguments.PartialReadFile(_, _, _) => prf
    case rf @ Arguments.ReadFile(_, _, _)         => rf
    case _                                        => this
  }

  def withReader(readerName: Implementation): Arguments = this

  def withTarget(target: Path): Arguments = this

  def withPrintResults(printResults: Boolean): Arguments = this
}
object Arguments {
  implicit val pathRead: scopt.Read[Path] = scopt.Read.reads(Paths.get(_))

  case object NoArgs extends Arguments
  case object PrintHeaders extends Arguments
  case object PrintLibraries extends Arguments
  case class PartialReadFile(readerOpt: Option[Implementation], targetOpt: Option[Path], printResults: Boolean = false)
      extends Arguments {
    override def withReader(readerName: Implementation): Arguments =
      targetOpt.fold(copy(readerOpt = readerName.some): Arguments) { target =>
        ReadFile(readerName, target)
      }

    override def withTarget(target: Path): Arguments =
      readerOpt.fold(copy(targetOpt = target.some): Arguments) { readerName =>
        ReadFile(readerName, target)
      }

    override def withPrintResults(verbose: Boolean): Arguments = copy(printResults = verbose)
  }
  case class ReadFile(reader: Implementation, target: Path, printResults: Boolean = false) extends Arguments {
    override def withReader(readerName: Implementation): Arguments = copy(reader = readerName)

    override def withTarget(target:        Path):    Arguments = copy(target       = target)
    override def withPrintResults(verbose: Boolean): Arguments = copy(printResults = verbose)
  }

  private val parser: scopt.OptionParser[Arguments] = new scopt.OptionParser[Arguments]("main") {
    cmd("print-headers")
      .text("Print a single CSV header row, then quit. Overrides all options except --help")
      .action((_, a) => a.asPrintHeaders)

    note("")
    cmd("print-libraries")
      .text("Print a list of valid arguments for --library. Overrides all options except print-headers and --help")
      .action((_, a) => a.asPrintLibraries)

    note("")
    cmd("process")
      .text("Process a file using one of the various implementations")
      .action((_, a) => a.asReadFileIfLegal)
      .children(
        opt[Unit]('r', "results")
          .text("Print out results instead of a CSV row of measurements.")
          .action((_, a) => a.withPrintResults(true))
          .optional(),
        opt[Implementation]('l', "library")
          .valueName("implementation")
          .text(
            "The implementation to use, valid arguments are " + Implementation.values.map(_.entryName).mkString(" ")
          )
          .action((r, a) => a.withReader(r))
          .required(),
        opt[Path]('f', "file")
          .valueName("path")
          .text("The path to the file to process")
          .action((p, a) => a.withTarget(p))
          .required()
      )

    note("\nGlobal options")
    help("help")
  }

  def showUsageAsError(): Unit = parser.showUsageAsError()

  def parse(args: Seq[String]): Option[Arguments] = parser.parse(args, NoArgs)
}
