# 304 Spark Streaming

Module 1, Big Data course (81932), University of Bologna.

## 304-0 Setup

For the streaming scenario, we need a streaming source. 
It can be done in two ways:
- Quickly setup a Netcat server to send some data
- Connect to Kafka to consume messages from some topic

### Netcat server

To simulate the streaming scenario we need a source that sends data. 
Both the cluster and the VM are equipped with Netcat.
- Usage: ```nc -lk <port>```

Use an available port number, e.g., ```9999```. 
Beware of possible conflicts when doing this on the cluster. 

### Streaming application via spark2-submit

This is the best way to run streaming applications.

With the Netcat server, use:

```spark2-submit --class ExerciseNetcat BD-304-streaming-spark.jar <exerciseNumber> <host> <port>```
- ```<host>``` is the IP of your machine on the cluster
   (IPs are 137.204.72.[233+N] where N is isi-vclustN)
  
With Kafka, use:

```spark2-submit --jars spark-streaming-kafka-0-10_2.11-2.1.0.jar --class ExerciseKafka BD-304-streaming-spark.jar <exerciseNumber> <consumerGroup> <topicName>``` 

- ```<consumerGroup>``` is the name of the consumer group 
  - Use your username as the consumer group
- ```<topicName>``` (optional) is the topic from which messages will be consumed
  - Avoid conflicts by defining topic names in the form "username_myTopic"
- The ```spark-streaming-kafka-0-10_2.11-2.1.0.jar``` file is provided with this package, in the ```resources``` folder;
make sure that you put this file in the local directory of your cluster machine
  - Note: with a fat jar there would be no need, but the fat jar with Spark's libraries is VERY heavy

Some exercises require additional parameters; in such cases, proper usage is indicated in the exercises' description.

#### Troubleshooting

If you get a NoSuchMethodError when running the spark2-submit with Kafka, run the following command:

```export SPARK_KAFKA_VERSION=0.10```

### Streaming application via spark-shell

The Spark shell is not the best tool for the streaming scenario: 
once a StreamingContext has been stopped, it cannot be restarted. 
- The whole application must be rewritten every time.
- If the SparkContext is accidentally stopped, the shell must be restarted as well.

Fastest way to operate on the shell:
- Write your application in a text editor (e.g., Notepad++) 
or an IDE (e.g., Eclipse/IntelliJ)
- Copy/paste the whole application in the shell
  - No need to use ```:paste``` mode; just paste
- Call ```ssc.stop(false)``` to stop the StreamingContext 
and leave the SparkContext alive
  - Beware: it may happen that Spark does not close correctly
  the StreamingContext; in such case, the shell must be restarted
- Repeat

### Note for VM users

The minimum setup for the VM may not be enough for the streaming
application to correctly print output on the terminal's console.

## 304-1 Testing the streaming application

[Netcat users] Copy/Paste some content (e.g., the text from the ```divinacommedia.txt```
file) into the Netcat server and see what happens on the application's console.

[Kafka users] Produce some textual content (productionMode = 2) 
and see what happens on the application's console. Default topic: ```bigdata_quotes```

## 304-2 Word count

Same as above.

## 304-3 Enabling checkpoint

Checkpoints allow the application to restart from where it last stopped.
               
NOTICE: you need to create a directory on HDFS to store the checkpoint data.
For instance:
- ```hdfs dfs -mkdir streaming```
- ```hdfs dfs -mkdir streaming/checkpoint3```

Then run the application; you need to provide 
an additional ```<path>``` parameter to the ```spark2-submit``` command
to indicate the absolute path on HDFS, e.g., ```/user/egallinucci/streaming/checkpoint3```

[Kafka users] Default topic: ```bigdata_quotes```

## 304-4 Enabling State

State allows the job to continuously update a temporary result (i.e., the state).
   
NOTICE: you need to either
- create a DIFFERENT directory on HDFS to store the checkpoint data
- empty the previous directory

Otherwise, the application will re-run the job already checkpointed in the directory!

The additional ```<path>``` parameter is required.

[Kafka users] Default topic: ```bigdata_quotes```

## 304-5 Sliding windows

This job carries out word counting on a sliding window 
that is wide 30 seconds and is updated every 3 seconds.

[Kafka users] Default topic: ```bigdata_quotes```

## 304-6 Trending hashtags

The dataset for this exercise (and for the next ones) 
is the content of ```dataset/tweet.dsv```, 
which contains a set of public tweets that discussed the topic
of vaccines back in 2016.

- [Netcat users] Copy/Paste some content from the ```dataset/tweet.dsv``` file.
- [Kafka users] Produce tweets (productionMode = 1).
 
This job is a simple evolution of word counting to carry out hashtag 
counting via sliding windows. 
The window is wide 1 minute and it is updated every 5 seconds.

[Kafka users] Default topic: ```bigdata_tweets```

## 304-7 Incremental count of tweets by city

This is a stateful job to incrementally count the number of tweets by city. 

Remember to either create a new directory on HDFS or to empty the previous one.
The additional ```<path>``` parameter is required.

[Kafka users] Default topic: ```bigdata_tweets```

## 304-8 Incremental count of tweets by city

This job extends the previous one by calculating also the average sentiment 
(per country instead of per city).

Remember to either create a new directory on HDFS or to empty the previous one. 
The additional ```<path>``` parameter is required.

[Kafka users] Default topic: ```bigdata_tweets```

## 304-9 Streaming algorithms

The next exercises are prepared for the **shell environment** and the ```dataset/tweet.dsv``` dataset.

Spark provides an implementation for some approximated algorithms, but not for the Streaming library. 
We will try them on simple DataFrames rather than on DStreams.

- In a batch use case, they allow to compute results in much less time and less pressure on the memory.

### Approximating distinct count (HyperLogLog++)

HyperLogLog++ is a slightly optimized version of HyperLogLog

- 64 bit hashing, empirical bias correction, etc.

Parameters:

- Maximum RSD (robust standard deviation); defaults to 0.05


```
import org.apache.spark.sql

val a = sc.textFile("hdfs:/bigdata/dataset/tweet").map(_.split("\\|")).filter(_(0)!="LANGUAGE")
val b = a.filter(_(2)!="").flatMap(_(2).split(" ")).filter(_!="").map(_.replace(",","")).toDF("hashtag").cache()
b.collect()
```

Exact result:
```
b.agg(countDistinct("hashtag")).collect()
```

Approximate result (default relative standard deviation = 0.05):
```
b.agg(approxCountDistinct("hashtag")).collect()
b.agg(approxCountDistinct("hashtag",0.1)).collect()
b.agg(approxCountDistinct("hashtag",0.01)).collect()
```

### Approximating membership (Bloom filter)

Parameters:

- Number of expected elements (n)
- Probability of false positives (p)

Exact result:
```
b.filter("hashtag = '#vaccino'").limit(1).count() // returns 1
b.filter("hashtag = '#vaccino2'").limit(1).count() // returns 0
```

```bloomFilter``` triggers an action; n=1000, p=0.01

```
val bf = b.stat.bloomFilter("hashtag", 1000, 0.01)
bf.mightContain("#vaccino") // returns true
bf.mightContain("#vaccino2") // probably returns false

val bf = b.stat.bloomFilter("hashtag", 1000, 10)
bf.mightContain("#vaccino") // returns true
bf.mightContain("#vaccino2") // may return true
```

### Approximating frequency (Count-Min sketch)

Parameters

- Tolerance for error (ε)
- Confidence (1-δ)
- A seed value

Exact result:
```b.filter("hashtag = '#vaccino'").count()```

```countMinSketch``` triggers an action; ε=0.01, 1-δ=0.99, seed=10

```
val cms = b.stat.countMinSketch("hashtag",0.01,0.99,10)
cms.estimateCount("#vaccino")

val cms = b.stat.countMinSketch("hashtag",0.1,0.99,10)
cms.estimateCount("#vaccino")

val cms = b.stat.countMinSketch("hashtag",0.01,0.9,10)
cms.estimateCount("#vaccino")
```