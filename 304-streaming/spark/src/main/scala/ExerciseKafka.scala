import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

// spark2-submit --class ExerciseKafka BD-304-streaming-spark.jar <exerciseNumber> <consumerGroup> <topic_name>
object ExerciseKafka extends App {

  override def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("Exercise 304 - Spark2").getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    if(args.length >= 2){
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> "isi-vclust1.csr.unibo.it:9092",
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> args(1),
        "auto.offset.reset" -> "latest",
        "enable.auto.commit" -> (true: java.lang.Boolean)
      )
      val topicsQuotes = if (args.length >= 3) Array(args(2)) else Array("bigdata_quotes")
      val topicsTweets = if (args.length >= 3) Array(args(2)) else Array("bigdata_tweets")

      val path = if (args.length >= 4) args(3) else ""
      args(0) match {
        case "1" => exercise1(spark.sparkContext,kafkaParams,topicsQuotes)
        case "2" => exercise2(spark.sparkContext,kafkaParams,topicsQuotes)
        case "3" => exercise3(spark.sparkContext,kafkaParams,topicsQuotes, path)
        case "4" => exercise4(spark.sparkContext,kafkaParams,topicsQuotes, path)
        case "5" => exercise5(spark.sparkContext,kafkaParams,topicsQuotes)
        case "6" => exercise6(spark.sparkContext,kafkaParams,topicsTweets)
        case "7" => exercise7(spark.sparkContext,kafkaParams,topicsTweets, path)
        case "8" => exercise8(spark.sparkContext,kafkaParams,topicsTweets, path)
      }
    }
  }

  /**
   * Simple total count of words
   * Words are detected by splitting lines on spacing (" ")
   * @param sc
   */
  def exercise1(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String]): Unit = {
    val ssc = new StreamingContext(sc, Seconds(3))
    val lines = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val words = lines.flatMap(_.value.split(" "))
    val count = words.count()
    count.print()
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * Word count
   * Words are detected by splitting lines on spacing (" ")
   * @param sc
   */
  def exercise2(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String]): Unit = {
    val ssc = new StreamingContext(sc, Seconds(3))
    val lines = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    val words = lines.flatMap(_.value.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _).map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) })
    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * Enabling checkpoint: this allows the application to restart from where it last stopped
   * NOTICE: you need to create a directory on HDFS to store the checkpoint data
   * - hdfs dfs -mkdir streaming
   * - hdfs dfs -mkdir streaming/checkpoint3
   * @param sc
   */
  def exercise3(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String], path: String): Unit = {
    def functionToCreateContext(): StreamingContext = {
      val newSsc = new StreamingContext(sc, Seconds(3))
      val lines = KafkaUtils.createDirectStream[String, String](
        newSsc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )
      val words = lines.flatMap(_.value.split(" "))
      val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _).map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) })
      wordCounts.print()
      newSsc.checkpoint(path)
      newSsc
    }

    val ssc = StreamingContext.getOrCreate(path, functionToCreateContext _)
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * Enabling state: this allows the job to continuously update a temporary result, i.e., the state
   * NOTICE: you need to either
   * - create a DIFFERENT directory on HDFS to store the checkpoint data
   * - empty the previous directory
   * Otherwise, the application will re-run the job already checkpointed in the directory
   * @param sc
   */
  def exercise4(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String], path: String): Unit = {
    def updateFunction( newValues: Seq[Int], oldValue: Option[Int] ): Option[Int] = {
      Some(oldValue.getOrElse(0) + newValues.sum)
    }

    def functionToCreateContext(): StreamingContext = {
      val newSsc = new StreamingContext(sc, Seconds(3))
      val lines = KafkaUtils.createDirectStream[String, String](
        newSsc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )
      val words = lines.flatMap(_.value.split(" "))
      val cumulativeWordCounts = words.map(x => (x, 1)).updateStateByKey(updateFunction)
      cumulativeWordCounts.map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) }).print()
      newSsc.checkpoint(path)
      newSsc
    }

    val ssc = StreamingContext.getOrCreate(path, functionToCreateContext _)
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * This job carries out word counting on a sliding window that is wide 30 seconds and is updated every 3 seconds.
   * @param sc
   */
  def exercise5(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String]): Unit = {
    val ssc = new StreamingContext(sc, Seconds(3))
    val lines = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    ).map(v => v).window(Seconds(30), Seconds(3))
    val words = lines.flatMap(_.value.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) }).print()

    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * The dataset for this exercise is the content of dataset/tweet.dsv
   * This job is a simple evolution of word counting to see the currently trending hashtags.
   * The window is wide 1 minute and it is updated every 5 seconds.
   * @param sc
   */
  def exercise6(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String]): Unit = {
    val ssc = new StreamingContext(sc, Seconds(1))
    val lines = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    ).map(v => v).window(Seconds(60), Seconds(5))
    val tweets = lines.filter(_.value.nonEmpty).map(_.value.split("\\|"))
    val hashtags = tweets.map(x => x(2)).flatMap(_.split(" ")).filter(_.nonEmpty)
    val hashtagCounts = hashtags.map(x => (x, 1)).reduceByKey(_ + _)
    hashtagCounts.map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) }).print()

    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * This is a stateful job to incrementally count the number of tweets by city.
   * Remember to either create a new directory on HDFS or to empty the previous one.
   * @param sc
   */
  def exercise7(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String], path: String): Unit = {
    def updateFunction( newValues: Seq[Int], oldValue: Option[Int] ): Option[Int] = {
      Some(oldValue.getOrElse(0) + newValues.sum)
    }

    def functionToCreateContext(): StreamingContext = {
      val newSsc = new StreamingContext(sc, Seconds(3))
      val lines = KafkaUtils.createDirectStream[String, String](
        newSsc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )
      val tweets = lines.filter(_.value.nonEmpty).map(_.value.split("\\|"))
      val cities = tweets.filter(x => x(4)!="" && x(4)!="0").map(x => x(4))
      val cumulativeCityCounts = cities.map(x => (x, 1)).updateStateByKey(updateFunction)
      cumulativeCityCounts.map({case(k,v)=>(v,k)}).transform({ rdd => rdd.sortByKey(false) }).print()
      newSsc.checkpoint(path)
      newSsc
    }

    val ssc = StreamingContext.getOrCreate(path, functionToCreateContext _)
    ssc.start()
    ssc.awaitTermination()
  }

  /**
   * This job extends the previous one by calculating also the average sentiment (per country instead of per city).
   * Remember to either create a new directory on HDFS or to empty the previous one.
   * @param sc
   */
  def exercise8(sc: SparkContext, kafkaParams: Map[String, Object], topics: Array[String], path: String): Unit = {
    def updateFunction( newValues: Seq[(Int,Int)], oldValue: Option[(Int,Int,Int,Double)] ): Option[(Int,Int,Int,Double)] = {
      val oldValue2 = oldValue.getOrElse(0,0,0,0.0)
      val totTweets = oldValue2._1 + newValues.map(_._1).sum
      val totSentiment = oldValue2._2 + newValues.map(_._2).sum
      val countSentiment = oldValue2._3 + newValues.size
      val avgSentiment = totSentiment.toDouble/countSentiment.toDouble
      Some((totTweets, totSentiment, countSentiment, avgSentiment))
    }

    def functionToCreateContext(): StreamingContext = {
      val newSsc = new StreamingContext(sc, Seconds(3))
      val lines = KafkaUtils.createDirectStream[String, String](
        newSsc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )
      val tweets = lines.filter(_.value.nonEmpty).map(_.value.split("\\|"))
      val cities = tweets.filter(x => x(7)!="" && x(7)!="0" && x(3)!="").map(x => ( x(7),(1,x(3).toInt) ) )
      val cumulativeCityCounts = cities.updateStateByKey(updateFunction)
      cumulativeCityCounts
        .mapValues(v => (v._1, v._2, v._3, Math.round(v._4*100).toDouble/100))
        .map({case(k,v)=>(v,k)})
        .transform({ rdd => rdd.sortByKey(false) })
        .print()
      newSsc.checkpoint(path)
      newSsc
    }

    val ssc = StreamingContext.getOrCreate(path, functionToCreateContext _)
    ssc.start()
    ssc.awaitTermination()
  }

}