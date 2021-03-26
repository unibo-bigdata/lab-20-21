import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession


// spark2-submit --class ExerciseComplete BD-302-spark-opt.jar <exerciseNumber>
// spark-submit --class ExerciseComplete BD-302-spark-opt.jar <exerciseNumber>
object ExerciseComplete extends App {

  override def main(args: Array[String]): Unit = {
    val sc = getSparkContext()

    if(args.length >= 1){
      args(0) match {
        case "1" => exercise1(sc)
        case "2" => exercise2(sc)
        case "3" => exercise3(sc)
        case "4" => exercise4(sc)
        case "5" => exercise5(sc)
        case "6" => exercise6(sc)
        case "7" => exercise7(sc)
      }
    }
  }

  /**
   * Creates the SparkContent; comment/uncomment code depending on Spark's version!
   * @return
   */
  def getSparkContext(): SparkContext = {
    // Spark 1
    // val conf = new SparkConf().setAppName("Exercise 302 - Spark1")
    // new SparkContext(conf)

    // Spark 2
    val spark = SparkSession.builder.appName("Exercise 302 - Spark2").getOrCreate()
    spark.sparkContext
  }

  /**
   * Optimize the two jobs (avg temperature and max temperature)
   * by avoiding the repetition of the same computations
   * and by defining a good number of partitions.
   *
   * Hints:
   * - Verify your persisted data in the web UI
   * - Use either repartition() or coalesce() to define the number of partitions
   *   - repartition() shuffles all the data
   *   - coalesce() minimizes data shuffling by exploiting the existing partitioning
   * - Verify the execution plan of your RDDs with rdd.toDebugString (shell) or on the web UI
   *
   * --- SOLUTION ---
   *
   * Number of partitions:
   * - Consider the limitation of up to 9 cores during class time
   * - Consider that 1 core is taken by the driver
   * - The number of cores will be 2 to 8
   * - An approximately good number of partitions is between 4-8 or 16-32, depending on the number of cores
   *
   * Caching:
   * - It is good to cache the rdd AFTER the common transformations (including the coalescing) have been carried out
   * - Then, remember to reference the cached rdd
   *
   * Check cached rdd in the Spark UI
   *
   * Coalesce vs repartition:
   * - coalesce aggregates partitions without shuffling; repartition forces a shuffle
   *   - rddWeather.filter(_.temperature<999).coalesce(8).map(x => (x.month, x.temperature)).toDebugString
   *   - rddWeather.filter(_.temperature<999).repartition(8).map(x => (x.month, x.temperature)).toDebugString
   * - none of them defines associates a partitioning criteria to the RDD
   *   - rddWeather.coalesce(8).partitioner
   *   - rddWeather.repartition(8).partitioner
   *
   * @param sc
   */
  def exercise1(sc: SparkContext): Unit = {
    val rddWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract)

    val cachedRdd = rddWeather.filter(_.temperature<999).repartition(8).map(x => (x.month, x.temperature)).cache()

    // Average temperature for every month
    cachedRdd.aggregateByKey((0.0,0.0))((a,v)=>(a._1+v,a._2+1), (a1,a2)=>(a1._1+a2._1,a1._2+a2._2)).map({case(k,v)=>(k,v._1/v._2)}).collect()
    // Maximum temperature for every month
    cachedRdd.reduceByKey((x,y)=>{if(x<y) y else x}).collect()
  }

  /**
   * Find the best option
   *
   * --- SOLUTION ---
   *
   * - keyBy() and map() break the partitioning, thus they must be issued before partitionBy()
   * - Anything that happens after cache() is not saved and must be recomputed each time;
   *   thus, it is good practice to cache() as later as possible
   * - rddS4 and rddS5 are the best option
   *
   * @param sc
   */
  def exercise2(sc: SparkContext): Unit = {
    import org.apache.spark.HashPartitioner
    val p = new HashPartitioner(8)

    val rddStation = sc.textFile("hdfs:/bigdata/dataset/weather-info/stations.csv").map(StationData.extract)

    val rddS4 = rddStation
      .keyBy(x => x.usaf + x.wban)
      .map({case (k,v) => (k,(v.country,v.elevation))})
      .partitionBy(p)
      .cache()
    val rddS5 = rddStation
      .map(x => (x.usaf + x.wban, (x.country,x.elevation)))
      .partitionBy(p)
      .cache()

  }

  /**
   * Define the join between rddWeather and rddStation and compute:
   * - The maximum temperature for every city
   * - The maximum temperature for every city in the UK
   *   - StationData.country == "UK"
   * - Sort the results by descending temperature
   *   - map({case(k,v)=>(v,k)}) to invert key with value and vice versa
   *
   * Hints & considerations:
   * - Keep only temperature values <999
   * - Join syntax: rdd1.join(rdd2)
   *   - Both RDDs should be structured as key-value RDDs with the same key: usaf + wban
   * - Consider partitioning and caching to optimize the join
   *   - Careful: it is not enough for the two RDDs to have the same number of partitions;
   *     they must have the same partitioner!
   * - Verify the execution plan of the join in the web UI
   *
   * --- SOLUTION BELOW ---
   *
   * @param sc
   */
  def exercise3(sc: SparkContext): Unit = {
    import org.apache.spark.HashPartitioner

    val p = new HashPartitioner(8)

    val rddWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract)
    val rddStation = sc.textFile("hdfs:/bigdata/dataset/weather-info/stations.csv").map(StationData.extract)

    val rddS = rddStation.keyBy(x => x.usaf + x.wban).partitionBy(p)
    val rddW = rddWeather.filter(_.temperature<999).keyBy(x => x.usaf + x.wban).partitionBy(p)

    val rddJoin = rddW.join(rddS).cache()
    rddJoin.toDebugString

    rddJoin.map({case(k,v)=>(v._2.name,v._1.temperature)}).reduceByKey((x,y)=>{if(x<y) y else x}).collect()
    rddJoin.filter(_._2._2.country=="UK").map({case(k,v)=>(v._2.name,v._1.temperature)}).reduceByKey((x,y)=>{if(x<y) y else x}).collect()
    rddJoin.filter(_._2._2.country=="UK").map({case(k,v)=>(v._2.name,v._1.temperature)}).reduceByKey((x,y)=>{if(x<y) y else x}).map({case(k,v)=>(v,k)}).sortByKey(false).collect()
  }

  /**
   * Use Spark's web UI to verify the space occupied by the following RDDs
   *
   * --- SOLUTION ---
   *
   * - Collecting is required to trigger the (lazy) evaluation
   * - memSerRdd's size will be less than half of memRdd's
   * - diskRdd's size will be the same as memSerRdd (thus, output to disk is always serialized)
   *
   * @param sc
   */
  def exercise4(sc: SparkContext): Unit = {
    import org.apache.spark.storage.StorageLevel._
    val rddWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract)

    sc.getPersistentRDDs.foreach(_._2.unpersist())

    val memRdd = rddWeather.sample(false,0.1).repartition(8).cache()
    val memSerRdd = memRdd.map(x=>x).persist(MEMORY_ONLY_SER)
    val diskRdd = memRdd.map(x=>x).persist(DISK_ONLY)

    memRdd.collect()
    memSerRdd.collect()
    diskRdd.collect()
  }

  /**
   * Consider the following scenario:
   * - We have a disposable RDD of Weather data (i.e., it is used only once): rddW
   * - And we have an RDD of Station data that is used many times: rddS
   * - Both RDDs are cached (collect() is called to enforce caching)
   *
   * We want to join the two RDDS. Which option is best?
   * - Simply join the two RDDs
   * - Enforce on rddW1 the same partitioner of rddS (and then join)
   * - Exploit broadcast variables
   *
   * --- SOLUTION ---
   *
   * - Check slides S4 on virtuale.unibo.it for detailed comments on this exercise
   * - The first and second options both take approximately the same time
   *   - The first one shuffles less data
   *   - The second one allows skipping unnecessary partitions from rddS
   * - The third option is faster
   *   - rddW data is not shuffled
   *   - the downside is the increased space occupation in the executors' memory
   *
   * @param sc
   */
  def exercise5(sc: SparkContext): Unit = {
    import org.apache.spark.HashPartitioner

    val rddWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract)
    val rddStation = sc.textFile("hdfs:/bigdata/dataset/weather-info/stations.csv").map(StationData.extract)

    val rddW = rddWeather
      .filter(_.temperature<999)
      .keyBy(x => x.usaf + x.wban)
      .cache()
    val rddS = rddStation
      .keyBy(x => x.usaf + x.wban)
      .partitionBy(new HashPartitioner(8))
      .cache()

    // Collect to enforce caching
    rddW.collect
    rddS.collect

    // Is it better to simply join the two RDDs..
    rddW
      .join(rddS)
      .map({case(k,v)=>(v._2.name,v._1.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .collect()

    // ..to enforce on rddW1 the same partitioner of rddS..
    rddW
      .partitionBy(new HashPartitioner(8))
      .join(rddS)
      .map({case(k,v)=>(v._2.name,v._1.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .collect()

    // ..or to exploit broadcast variables?
    val bRddS = sc.broadcast(rddS.collectAsMap())
    val rddJ = rddW
      .map({case (k,v) => (bRddS.value.get(k),v)})
      .filter(_._1!=None)
      .map({case(k,v)=>(k.get.asInstanceOf[StationData],v)})
    rddJ
      .map({case (k,v) => (k.name,v.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .collect()
  }

  /**
   * Start from the result of Exercise 3; is there a more efficient way to compute the same result?
   *
   * --- SOLUTION BELOW ---
   *
   * @param sc
   */
  def exercise6(sc: SparkContext): Unit = {
    val rddWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract)
    val rddStation = sc.textFile("hdfs:/bigdata/dataset/weather-info/stations.csv").map(StationData.extract)

    val rddW = rddWeather.filter(_.temperature<999).keyBy(x => x.usaf + x.wban).cache()
    val rddS = rddStation.keyBy(x => x.usaf + x.wban).cache()

    // First version
    val rdd6a = rddW
      .join(rddS).filter(_._2._2.country=="UK")
      .map({case(k,v)=>(v._2.name,v._1.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x}).map({case(k,v)=>(v,k)})
      .sortByKey(false)

    // Pushing down filters is always a good move
    val rdd6b = rddW
      .join(rddS.filter(_._2.country=="UK"))
      .map({case(k,v)=>(v._2.name,v._1.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .map({case(k,v)=>(v,k)})
      .sortByKey(false)

    // Pushing down aggregations is even better
    val rdd6c = rddW
      .map({case(k,v)=>(k,v.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .join(rddS).filter(_._2._2.country=="UK")
      .map({case(k,v)=>(v._1,v._2.name)})
      .sortByKey(false)

    // Best option: pushing down both filters and aggregations
    val rdd6d = rddW
      .map({case(k,v)=>(k,v.temperature)})
      .reduceByKey((x,y)=>{if(x<y) y else x})
      .join(rddS.filter(_._2.country=="UK"))
      .map({case(k,v)=>(v._1,v._2.name)})
      .sortByKey(false)
  }


  /**
   * Consider the following job on the movielens dataset.
   *
   * In input, it takes:
   * - a CSV file of movies (each row is movie with a movieId, a title, and list of genres)
   * - a CSV file of ratings (each row is a rating [0-5] made on a movieId in a certain year)
   * - a CSV file of tags (each row is a tag associate to a movieId in a certain year)
   *
   * The goal of the job:
   * - return, for each year, the top N movies based on the average rating
   *   - for each movie, the result should list the movie's title and the total number of associated tags
   *   - the result must be ordered by year
   *
   * The procedure:
   * - initialize RDDS from CSV files
   * - count the number of tags for each movie
   * - join movies with the other RDDs to associate the former with the respective ratings and number of tags
   * - aggregate the result to compute the average rating for each movie
   * - group the result by year
   *
   * The goal of the exercise:
   * - think of the optimizations that can be carried out on this job
   * - try implementing them and see how much time/computation you are able to save
   * - do NOT modify anything outside of the core part, nor in the MovieLensParser class
   * - ensure that Spark recomputes everything by re-initializing every RDD
   *
   * IMPORTANT: for a fair comparison, run the spark2-shell or the spark2-submit with these parameters
   * - --num-executors 2
   * - --executor-cores 3
   *
   * --- SOLUTION ---
   *
   * The following optimizations that can be done:
   * - increase the number of partitions of rddRatings to increase the level of parallelism
   *   and take full advantage of the computing resources
   * - anticipate the aggregation of ratings before joining with movies and tags
   * - broadcast movies and tags due to their small size (?)
   *
   * @param sc
   */
  def exercise7(sc: SparkContext): Unit = {
    val inputMoviesPath = "/bigdata/dataset/movielens/movies.csv"
    val inputRatingsPath = "/bigdata/dataset/movielens/ratings.csv"
    val inputTagsPath = "/bigdata/dataset/movielens/tags.csv"
    val outputPath = "movielens-output2"
    val topN = 10

    /* CORE PART (start) */

    // Initialize RDDs from CSV files
    val rddMovies = sc.textFile(inputMoviesPath).flatMap(MovieLensParser.parseMovieLine)
    val rddRatings = sc.textFile(inputRatingsPath,16).flatMap(MovieLensParser.parseRatingLine)
    val rddTags = sc.textFile(inputTagsPath).flatMap(MovieLensParser.parseTagLine)

    import org.apache.spark.HashPartitioner

    val broadcastMovies = sc.broadcast(rddMovies.map(m => (m._1, m._2)).collectAsMap)

    val broadcastTagsPerMovie = sc.broadcast(rddTags.map(t => (t._1, 1)).reduceByKey(_ + _).collectAsMap)

    // rddRatingsKV ( (movieId, year), rating )
    val rddRatingsKV = rddRatings
      //.repartition(16)
      .map(r => ((r._1, r._2), r._3))

    // rddRatingPerMovie ( (movieId, year), avgRating ) - Calculate average rating by movie
    val rddRatingPerMovie = rddRatingsKV
      .aggregateByKey((0.0,0.0))((a,v)=>(a._1+v,a._2+1), (a1,a2)=>(a1._1+a2._1,a1._2+a2._2))

    // rddRatingPerMovieByYear ( year, (title,nTags,avgRating) ) - Join with the two broadcast variables to get title and number of tags
    val rddRatingPerMovieByYear = rddRatingPerMovie
      .map({case (k,v) => (k, (broadcastMovies.value.get(k._1), broadcastTagsPerMovie.value.get(k._1), v._1/v._2) )})

    /* CORE PART (end) */

    // rddRatingPerMovie ( year, (title,nTags,avgRating) ) - Group by year and format the final result
    val result = rddRatingPerMovieByYear
      .groupByKey
      .mapValues(_.toList.sortBy(-_._3).take(topN))
      .coalesce(1)
      .sortByKey()
      .saveAsTextFile(outputPath)
  }

}