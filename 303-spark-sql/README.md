# 303 SparkSQL

Module 1, Big Data course (81932), University of Bologna.

## 303-0 New datasets

In the ```/dataset``` folder are some new datasets to be used in the following exercises. These datasets are available on the cluster's HDFS in the 
```/bigdata/dataset``` folder.

## 303-1 Creating DataFrames from different file formats

Load a JSON file (Movies)

```shell
val dfMovies = spark.read.json("/bigdata/dataset/movies/movies.json")
val dfMovies = spark.read.format("json").load("/bigdata/dataset/movies/movies.json")
```

Load a CSV without a defined schema (Population)

```shell
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructType,StructField,StringType}
val population = sc.textFile("/bigdata/dataset/population/zipcode_population.csv")
val schemaString = "zipcode total_population avg_age male female"
val schema = StructType(schemaString.split(" ").map(fieldName ⇒ StructField(fieldName, StringType, true)))
val rowRDD = population.map(_.split(";")).map(e ⇒ Row(e(0), e(1), e(2), e(3), e(4)))
val dfPeople = spark.createDataFrame(rowRDD, schema)
```

Load a TXT with the schema in the first row (Real estate transactions)

```shell
val dfTransactions = spark.read.format("csv").option("header", "true").option("delimiter",";").load("/bigdata/dataset/real_estate/real_estate_transactions.txt")
```

Load a Parquet file (User data)

```shell
val dfUserData = spark.read.format("parquet").load("/bigdata/dataset/userdata/userdata.parquet")
```

Load a Hive table (Geography)

```shell
spark.sql("use default")
val dfGeo = spark.sql("select * from geo")
```

## 303-2 Saving DataFrames to different file formats

Write to JSON on HDFS

```shell
dfUserData.write.mode("append").json("userData")
```

Write to table on Hive (create a new database, select it, and write the table). 
First, create a database with name "user_[username]" (e.g., "user_egallinucci").

```shell
spark.sql("create database user_[username]")
spark.sql("use user_[username]")

# .saveAsTable saves as Parquet by default
dfUserData.write.saveAsTable("userdata")
dfUserData.write.mode("overwrite").saveAsTable("userdata")
dfUserData.write.format("parquet").mode("overwrite").saveAsTable("userdata")

# Use partitioning or bucketing
dfUserData.write.partitionBy("gender").saveAsTable("userdata_p")
dfUserData.bucketBy(5,"first_name").saveAsTable("userdata_b")
dfUserData.bucketBy(10,"first_name").saveAsTable("userdata_b2")
```

Write to Parquet file on HDFS

```shell
dfMovies.write.parquet("movies.parquet")
```

## 303-3 Basic SQL Operations

There are two main ways to formulate SQL queries on DataFrames:
- using ```spark.sql()``` to write an arbitrary SQL query;
- calling DataFrame's specific methods like ```df.select()```, ```df.groupBy()```, etc.

```shell
spark.sql("select gender, count(*) from userdata_b group by gender").show
spark.sql("select first_name, count(*) from userdata_b group by first_name").show
```

Use the [Spark documentation](https://spark.apache.org/docs/2.1.0/sql-programming-guide.html) as a reference to find the available methods.

```shell
val dfWeather = sc.textFile("hdfs:/bigdata/dataset/weather-sample").map(WeatherData.extract).toDF()
val dfStation = sc.textFile("hdfs:/bigdata/dataset/weather-info/stations.csv").map(StationData.extract).toDF()

dfWeather.createOrReplaceTempView("weather")
dfStation.createOrReplaceTempView("station")
val dfJoin = spark.sql("select w.*, s.* from weather w, station s where w.usaf=s.usaf and w.wban = s.wban")

dfJoin.printSchema()

val q1 = dfJoin.filter("elevation > 100").select("country","elevation")
val q2 = dfJoin.groupBy("country").agg(avg("temperature")).orderBy(asc("country"))
val q3 = dfJoin.groupBy("country").agg(avg("temperature").as("avgTemp")).orderBy(desc("avgTemp"))
q1.show()
q2.show()
q3.show()
```

## 303-4 Execution plan evaluation

Verify predicate push-down: in both cases, selection predicates are pushed as close to the source as possible.

```
# Spark 1
val q4 = sqlContext.sql("select w.*, s.* from weather w, station s where w.usaf=s.usaf and w.wban = s.wban and elevation > 100")
# Spark 2
val q4 = spark.sql("select w.*, s.* from weather w, station s where w.usaf=s.usaf and w.wban = s.wban and elevation > 100")

q1.explain
q4.explain
```

Evaluate broadcasting: check execution times and the DAGs in the Web UI.

```
val dfJoin2 = dfWeather.join(broadcast(dfStation),dfWeather("usaf")===dfStation("usaf") && dfWeather("wban") === dfStation("wban"))
# Latest Spark's versions (after 2.1)
val dfJoin2 = spark.sql("/*+ broadcast(s) */ select w.*, s.* from weather w, station s where w.usaf=s.usaf and w.wban = s.wban")

dfJoin2.explain
dfJoin.explain

dfJoin2.count()
dfJoin.count()
```

## 303-5 Exercises on new datasets

Try out some queries on the available data frames.
- On ```dfTransactions```, calculate the average price per city and show the most expensive cities first
  - Also, convert from EUR to USD (considering an exchange rate of 1€ = 1.2$); i.e., multiply the price by 1.2
  - Save the result to a Parquet file

- On ```dfUserData```, order countries by increasing gender pay gap
  - Hint: use ```.withColumnRenamed("oldname","newname")``` to rename columns
  - Hint: use ```.withColumn("name",expr("..."))``` to create a new column through some expression

## 303-6 Exercise on MovieLens

Implement the core part of exercise 302-7 with SparkSQL.

```shell
val dfMovies = rddMovies.toDF("movieId","title","genres")
val dfRatings = rddRatings.toDF("movieId","year","rating")
val dfTags = rddTags.toDF("movieId","year")
```