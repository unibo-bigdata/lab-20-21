// 303-5

dfTransactions.createOrReplaceTempView("transactions")
val q = spark.sql("select city, round(avg(price*1.2),2) as avgPrice from transactions group by city order by avgPrice desc")

q.show()
q.write.parquet("transactionPrices.parquet")

////

val dfCG = dfUserData
    .groupBy("country","gender")
    .agg(avg("salary").as("avgSalary"))

val dfF = dfCG
    .filter("gender = 'Female'")
    .select("country","avgSalary")
    .withColumnRenamed("country","f_country")
    .withColumnRenamed("avgSalary","f_avgSalary")

val dfM = dfCG
    .filter("gender = 'Male'")
    .select("country","avgSalary")
    .withColumnRenamed("country","m_country")
    .withColumnRenamed("avgSalary","m_avgSalary")

val dfRes = dfF
    .join(dfM, dfF("f_country")===dfM("m_country"))
    .withColumn("gap",expr("f_avgSalary - m_avgSalary"))
    .orderBy("gap")

// 303-6

val dfMovies = rddMovies.toDF("movieId","title","genres")
val dfRatings = rddRatings.toDF("movieId","year","rating")
val dfTags = rddTags.toDF("movieId","year")

dfMovies.createOrReplaceTempView("movies")
dfRatings.createOrReplaceTempView("ratings")
dfTags.createOrReplaceTempView("tags")

spark.sql("select movieid, count(*) nTags from tags t group by movieid").createOrReplaceTempView("tagsPerMovie")
spark.sql("select r.year, m.title, t.nTags, avg(r.rating) avgRating from movies m, ratings r, tagsPerMovie t where m.movieId = t.movieId and m.movieId = r.movieId group by r.year, m.title, t.nTags").show

# Catalyst does not push down aggregations.. thus, the following is a better solution

spark.sql("select movieid, year, avg(rating) avgRating from ratings r group by movieid, year").createOrReplaceTempView("ratingsPerMovie")
spark.sql("select r.year, m.title, t.nTags, r.avgRating from movies m, ratingsPerMovie r, tagsPerMovie t where m.movieId = t.movieId and m.movieId = r.movieId").show