package livongo.large_files_blog_post.common

object Record {

  /**
    * We use this extractor (an object with `unapply` defined) to handle parsing out just the fields we care about.
    *
    * @param line the raw input line
    */
  def unapply(line: String): Option[(FullName, Option[FirstName], DateBucket)] = {
    val grabber = line.split('|').lift
    for {
      fullName <- grabber(7).map(FullName)
      rawDate  <- grabber(4)
    } yield (fullName, FirstName(fullName), DateBucket(rawDate))
  }
}
