# 304 Streaming - Kafka

Module 1, Big Data course (81932), University of Bologna.

This is a Java tool that can be used to produce messages and push them to a Kafka broker.

## Topic creation

First you need to create a topic on Kafka.
Replace ```<topic_name>``` with a name of your choice; 
consider adding your username to the topic name to avoid conflicts with other students.

Remember that the number of partitions can only be increased, and that the cluster has 2 brokers.

```kafka-topics --create --zookeeper isi-vclust0.csr.unibo.it:2181 --replication-factor 1 --partitions 4 --topic <topic_name>```

Check the list of available topics.

```kafka-topics --list --zookeeper isi-vclust0.csr.unibo.it:2181```

Delete a topic.

```kafka-topics --zookeeper isi-vclust0.csr.unibo.it:2181 --delete --topic <topic_name>```

## Usage of the tool

The tool has three production modes:

0. "tweets": generate tweets from a given .dsv file
0. "got": generate random Game of Thrones quotes
0. "lorem": generate random Lorem Ipsum sentences

In order to use the tool, you need to:

- Put the  ```dataset/tweet.dsv``` file on your cluster machine
- Update the constant values in ```src/main/java/IKafkaConstants```; in particular, check:
  - ```MESSAGE_RATE```: the interval (in ms) for message production;
  - ```MESSAGE_COUNT```: the number of messages to be produced in every interval
  - ```DATASET_TWEET_PATH```: the path for the .dsv file
- Build and put the fat Jar on your cluster machine

To use the tool:

```java -cp BD-304-streaming-kafka-all.jar KafkaApp produce <productionMode> <topicName> <messageCount>```

where:

- ```<productionMode>``` is either "tweets", "got", or "lorem" (default: "tweets")
- ```<topicName>``` is a topic of your choice (default: sample)
- ```<messageCount>``` is the optional overwrite of ```MESSAGE_COUNT```