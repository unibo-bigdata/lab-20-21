import java.util.Calendar

object MovieLensParser {

  val noGenresListed = "(no genres listed)"
  val commaRegex = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"
  val pipeRegex = "\\|(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"
  val quotes = "\""

  /** Function to parse movie records
   *
   *  @param line line that has to be parsed
   *  @return tuple containing movieId, title and genres, none in case of input errors
   */
  def parseMovieLine(line: String): Option[(Long, String, String)] = {
    try {
      val input = line.split(commaRegex)
      var title = input(1).trim
      title = if(title.startsWith(quotes)) title.substring(1) else title
      title = if(title.endsWith(quotes)) title.substring(0, title.length - 1) else title
      Some(input(0).trim.toLong, title, input(2).trim)
    } catch {
      case _: Exception => None
    }
  }

  /** Function to parse rating records
   *
   *  @param line line that has to be parsed
   *  @return tuple containing movieId, year and rating, none in case of input errors
   */
  def parseRatingLine(line: String): Option[(Long, Int, Double)] = {
    try {
      val input = line.split(commaRegex)
      val timestamp = input(3).trim.toLong * 1000L
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(timestamp)
      Some(input(1).trim.toLong, cal.get(Calendar.YEAR), input(2).trim.toDouble)
    } catch {
      case _: Exception => None
    }
  }

  /** Function to parse tag records
   *
   *  @param line line that has to be parsed
   *  @return tuple containing movieId and year, none in case of input errors
   */
  def parseTagLine(line: String) : Option[(Long, Int)] = {
    try {
      val input = line.split(commaRegex)
      val timestamp = input(3).trim.toLong * 1000L
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(timestamp)
      Some(input(1).trim.toLong, cal.get(Calendar.YEAR))
    } catch {
      case _: Exception => None
    }
  }

}
