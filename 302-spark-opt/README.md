# 302 Spark optimization

Module 1, Big Data course (81932), University of Bologna.

File ```src/main/scala/Exercise``` contains the code for this assignment. 
Complete the code and/or answer the provided questions.
All solutions will be released after class under ```src/main/scala/ExerciseComplete```

To run via spark shell:
- ```spark-shell --num-executors 1``` (on the VM)
- ```spark2-shell``` (on the cluster)
- Copy/paste the code in ```src/main/scala/StationData``` and ```src/main/scala/WeatherData``` (use ```:paste``` to enter paste mode; use Ctrl+D to exit it)
- Copy/paste and complete exercise code (note: there is no need to create the SparkContext)

To run via spark submit:
- Comment/uncomment dependencies and code lines code lines (see function ```getSparkContext()```) depending on your Spark version
- ```spark-submit --class Exercise BD-302-spark-opt.jar <exerciseNumber>```
- ```spark2-submit --class Exercise BD-302-spark-opt.jar <exerciseNumber>```
- Beware: debugging is more burdensome (use ```yarn logs -applicationId <appId>``` where ```<appId>``` is similar to ```application_1583679666662_0134``` to retrieve the Executors' logs)

## 302-1 Job optimization

Optimize the two jobs (avg temperature and max temperature) by avoiding the repetition of the same computations and by defining a good number of partitions.

Hints:
- Verify your persisted data in the web UI
- Use either ```repartition()``` or ```coalesce()``` to define the number of partitions
  - ```repartition()``` shuffles all the data
  - ```coalesce()``` minimizes data shuffling by exploiting the existing partitioning
- Verify the execution plan of your RDDs with ```rdd.toDebugString``` (shell only) or on the web UI

## 302-2 RDD preparation

Check the five possibilities to transform the Station RDD and identify the best one.

## 302-3 Joining RDDs

Define the join between rddWeather and rddStation and compute:
- The maximum temperature for every city
- The maximum temperature for every city in the UK: 
  - ```StationData.country == "UK"```
- Sort the results by descending temperature
  - ```map({case(k,v)=>(v,k)})``` to invert key with value and vice versa

Hints & considerations:
- Keep only temperature values <999
- Join syntax: ```rdd1.join(rdd2)```
  - Both RDDs should be structured as key-value RDDs with the same key: usaf + wban
- Consider partitioning and caching to optimize the join
  - Careful: it is not enough for the two RDDs to have the same number of partitions; they must have the same partitioner!
- Verify the execution plan of the join in the web UI

## 302-4 Memory occupation

Use Spark's web UI to verify the space occupied by the provided RDDs.

## 302-5 Evaluating different join methods

Consider the following scenario:
- We have a disposable RDD of Weather data (i.e., it is used only once): ```rddW```
- And we have an RDD of Station data that is used many times: ```rddS```
- Both RDDs are cached (```collect() ```is called to enforce caching)

We want to join the two RDDS. Which option is best?
- Simply join the two RDDs
- Enforce on ```rddW1``` the same partitioner of ```rddS``` (and then join)
- Exploit broadcast variables

## 302-6 Optimizing Exercise 3

Start from the result of Exercise 3; is there a more efficient way to compute the same result?

## 302-7 Full job optimization (movielens dataset)

Considering the given job on the movielens dataset (```/bigdata/dataset/movielens```), which defines a more complex workload than those seen before.

IMPORTANT: to run it in the sheel, you need to copy/paste the code within the ```MovieLensParser``` file.

In input, it takes:
- a CSV file of movies (each row is movie with a movieId, a title, and list of genres)
- a CSV file of ratings (each row is a rating [0-5] made on a movieId in a certain year)
- a CSV file of tags (each row is a tag associate to a movieId in a certain year)

The goal of the job:
- return, for each year, the top N movies based on the average rating
  - for each movie, the result should list the movie's title and the total number of associated tags
  - the result must be ordered by year

The procedure:
- initialize RDDS from CSV files
- count the number of tags for each movie
- join movies with the other RDDs to associate the former with the respective ratings and number of tags
- aggregate the result to compute the average rating for each movie
- group the result by year

The goal of the exercise:
- think of the optimizations that can be carried out on this job
- try implementing them and see how much time/computation you are able to save
- do NOT modify anything outside of the core part, nor in the MovieLensParser class
- ensure that Spark recomputes everything by re-initializing every RDD

IMPORTANT: for a fair comparison, run the spark2-shell or the spark2-submit with these parameters
- ```--num-executors 2```
- ```--executor-cores 3```

