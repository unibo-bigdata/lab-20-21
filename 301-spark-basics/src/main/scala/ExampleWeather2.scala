import org.apache.spark.sql.SparkSession

// spark2-submit --class ExampleWeather2 BD-301-spark-basics.jar
object ExampleWeather2 extends App {

  val username = "egallinucci"

  // Function to parse weather records; returns key -value pairs in the form(month, temperature)
  def parseWeatherLine(line: String): (String, Double) = {
    val year = line.substring(15, 19)
    val month = line.substring(19, 21)
    val day = line.substring(21, 23)
    var temp = line.substring(87, 92).toInt
    (month, temp / 10)
  }

  override def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("ExampleWeather Spark 2.1").getOrCreate()

    // Create an RDD from the files in the given folder
    val rddWeather = spark.sparkContext.textFile("hdfs:/bigdata/dataset/weather-sample")

    //Coalesce to reduce the number of partitions(it is one per block by default, then parse records
    val rddWeatherKv = rddWeather.coalesce(12).map(x => parseWeatherLine(x))
    //Aggregate by key(i.e., month) to compute the sum and the count of temperature values
    val rddTempDataPerMonth = rddWeatherKv.aggregateByKey((0.0, 0.0))((a, v) => (a._1 + v, a._2 + 1), (a1, a2) => (a1._1 + a2._1, a1._2 + a2._2))
    //Calculate the average temperature in each record
    val rddAvgTempPerMonth = rddTempDataPerMonth.map({ case (k, v) => (k, v._1 / v._2) })
    //Sort, coalesce and cache the result(because it is used twice)
    val rddResult = rddAvgTempPerMonth.sortByKey().coalesce(1)

    //Save the RDD on HDFS; the directory should NOT exist
    rddResult.saveAsTextFile("hdfs:/user/" + username + "/spark/avgTempPerMonth")
  }

}