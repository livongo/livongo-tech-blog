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

/**
  * These are semantic wrappers that should have little to no runtime impact.
  *
  * They extend the special trait [[scala.AnyVal]], marking them as a compile-time fiction. In practice there are
  * some cases where object allocations happen anyway, but it still helps.
  */
final case class FullName(value:  String) extends AnyVal
final case class FirstName(value: String) extends AnyVal
object FirstName {

  /**
    * Extract the first name from a full name.
    *
    * @param fullName the full name, which is expected to be in the format `family, given` or `family, given middle`
    * @return [[scala.None]] if the format isn't what we expect, or [[scala.Some]] if we can pull a
    *         [[livongo.large_files_blog_post.common.FirstName]] out of the input.
    */
  def apply(fullName: FullName): Option[FirstName] = {
    for {
      firstAndMiddleNames <- fullName.value.split(',').tail.headOption.map(_.trim)
      if firstAndMiddleNames.nonEmpty
      firstName <- firstAndMiddleNames.split(' ').headOption
    } yield FirstName(firstName)
  }

  implicit val ordering: Ordering[FirstName] = Ordering.by(_.value)
}

final case class DateBucket(year: Int, month: Int) {
  override def toString: String = "%04d-%02d".format(year, month)
}
object DateBucket {

  /**
    * Companion object constructors are a common pattern in Scala, and a good place
    * for really common initialization code.
    *
    * @param raw a string, with the expected format 'YYYYMM' (longer is OK, shorter will break things)
    */
  def apply(raw: String): DateBucket = {
    val year  = raw.take(4).toInt
    val month = raw.slice(4, 6).toInt
    DateBucket(year, month)
  }

  implicit val ordering: Ordering[DateBucket] = Ordering.by(db => db.year -> db.month)
}

/**
  * Holds the values needed to accumulate the results so far while processing the lines in the file.
  *
  * @param lineCount          number of lines before this one
  * @param specialNames       recorded names, and the indexes on which they were found - see
  *                           [[livongo.large_files_blog_post.common.LineMetricsAccumulator#SpecialLineNames]]
  * @param donationsByMonth   the number of donations in a given month
  * @param firstNameFrequency the number of times each first name appears
  */
final class LineMetricsAccumulator(
    lineCount:          Int,
    specialNames:       List[(Int, FullName)],
    donationsByMonth:   Map[DateBucket, Int],
    firstNameFrequency: Map[FirstName, Int]
) {

  /**
    * Progresses and incorporates the results of an additional line.
    *
    * This logic is common across the various ways of reading the file, so it's abstracted out here.
    *
    * @param line the raw line
    */
  def addLine(line: String): LineMetricsAccumulator = line match {
    case Record(fullName, firstNameOpt, dateBucket) =>
      val lineNumber = lineCount + 1
      new LineMetricsAccumulator(
        lineCount = lineNumber,
        specialNames =
          if (LineMetricsAccumulator.LineNumbersOfSpecialNames.contains(lineNumber))
            (lineNumber, fullName) :: specialNames
          else specialNames,
        donationsByMonth = donationsByMonth.updated(
          dateBucket,
          donationsByMonth.getOrElse(dateBucket, 0) + 1
        ),
        firstNameFrequency = firstNameOpt.fold(firstNameFrequency) { firstName =>
          firstNameFrequency.updated(
            firstName,
            firstNameFrequency.getOrElse(firstName, 0) + 1
          )
        }
      )
  }

  /**
    * Convert to [[livongo.large_files_blog_post.common.Result]] by running the only aggregation (most common first name)
    */
  def asResult: Result = {
    val (mostCommonFirstName, mostCommonFirstNameCount) =
      // This orders first by frequency, then alphabetically to break any ties.
      firstNameFrequency.maxBy(_.swap)
    Result(
      lineCount,
      specialNames,
      donationsByMonth,
      mostCommonFirstName,
      mostCommonFirstNameCount
    )
  }
}

object LineMetricsAccumulator {
  def empty: LineMetricsAccumulator = new LineMetricsAccumulator(-1, List.empty, Map.empty, Map.empty)
  val LineNumbersOfSpecialNames = Set(0, 432, 43243)
}

/**
  * Contains the results of processing the whole file.
  *
  * @param lineCount                number of lines in the file
  * @param specialNames             recorded names, and the indexes on which they were found - see
  *                                 [[livongo.large_files_blog_post.common.LineMetricsAccumulator#SpecialLineNames]]
  * @param donationsByMonth         the number of donations in a given month
  * @param mostCommonFirstName      the first name which was the most common
  * @param mostCommonFirstNameCount the number of times the most common first name occurred
  */
final case class Result(
    lineCount:                Int,
    specialNames:             List[(Int, FullName)],
    donationsByMonth:         Map[DateBucket, Int],
    mostCommonFirstName:      FirstName,
    mostCommonFirstNameCount: Int
) {

  /**
    * Helper to print out the results in a standard format
    *
    * @param timingData how long it took to calculate these results
    */
  def print(timingData: TimingData): Unit = {
    println(s"Processed $lineCount lines in $timingData")
    println("Results:")
    println {
      specialNames
        .sortBy(_._1)
        .map {
          case (line, FullName(name)) => s"Name on line $line: $name"
        }
        .mkString("\n")
    }
    println {
      s"""|Most common first name: ${mostCommonFirstName.value} ($mostCommonFirstNameCount times)
          |Donations by year and month:""".stripMargin
    }
    println {
      donationsByMonth.toSeq.sorted
        .map {
          case (dateBucket, count) => s"    $dateBucket : $count"
        }
        .mkString("\n")
    }
  }
}

object Result {

  import scala.collection.JavaConverters._

  /**
    * The conversions from Java collections are much easier to do on the Scala side of things, for a couple of reasons:
    * 1. Classes which extend [[scala.AnyVal]] really confuse the Java compiler
    * 2. While function resolution works when passing an `int` or [[java.lang.Integer]] to a parameter of type
    * [[scala.Int]] when it's a top-level type (like `lineCount`), the Java compiler has issues recognizing the
    * intended equivalence when it's a generic type parameter (like `specialNames`)
    * 3. While it's possible to call the various conversion methods in [[scala.collection.JavaConverters]] from Java,
    * it's really awkward and doesn't work well if you need to get to [[scala.collection.immutable.Map]], because of
    * the implicit parameters used by [[scala.collection.TraversableOnce#toMap(scala.Predef.$less$colon$less)]]
    */
  def fromJava(
      lineCount:                Int,
      specialNames:             java.util.Map[java.lang.Integer, String],
      donationsByMonth:         java.util.Map[DateBucket, java.lang.Integer],
      mostCommonFirstName:      String,
      mostCommonFirstNameCount: Int
  ): Result = {
    new Result(
      lineCount,
      specialNames.asScala
        .map {
          case (k, v) => k.intValue -> FullName(v)
        }
        .toList
        .sortBy(_._1),
      donationsByMonth.asScala.map {
        case (k, v) => k -> v.intValue
      }.toMap,
      FirstName(mostCommonFirstName),
      mostCommonFirstNameCount
    )
  }
}
