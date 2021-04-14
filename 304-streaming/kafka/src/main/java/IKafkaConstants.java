public interface IKafkaConstants {
    // Address of kafka broker
    String KAFKA_BROKERS = "isi-vclust1.csr.unibo.it:9092";

    // Number of messages to be sent every MESSAGE_RATE milliseconds
    Integer MESSAGE_COUNT=1000;
    Integer MESSAGE_RATE=1000;

    String DATASET_TWEET_PATH = "/home/egallinucci/streaming/dataset/tweet.dsv";

    // Name of the client
    String CLIENT_ID="big-data-client";

    // Default topics to read from or write to
    String TOPIC_NAME_1="bigdata_tweets";
    String TOPIC_NAME_2="bigdata_quotes";

    String GROUP_ID_CONFIG="big-data-cg";

    Integer MAX_NO_MESSAGE_FOUND_COUNT=100;

    String OFFSET_RESET_LATEST="latest";

    String OFFSET_RESET_EARLIER="earliest";

    Integer MAX_POLL_RECORDS=1;

}