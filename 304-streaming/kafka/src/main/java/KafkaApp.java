import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import kafka.Kafka;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class KafkaApp {

    public static void main(String[] args) throws FileNotFoundException {
        if(args.length > 0){
            String productionMode = args.length>1 ? args[1] : "tweets";
            String topicName = args.length>2 ? args[2] : (productionMode.equals("tweets") ? IKafkaConstants.TOPIC_NAME_1 : IKafkaConstants.TOPIC_NAME_2);
            Integer messageCount = args.length>3 ? Integer.parseInt(args[3]) : IKafkaConstants.MESSAGE_COUNT;
            switch(args[0]){
                case "produce":
                    runProducer(topicName, productionMode, messageCount);
                    break;
                case "consume":
                    runConsumer(topicName);
                    break;
                case "empty":
                    emptyTopic();
                    break;
            }
        }
    }

    static void runProducer(String topicName, String productionMode, Integer messageCount) throws FileNotFoundException {
        Timer timer = new Timer();
        timer.schedule(new ProducerCreator(productionMode, topicName, messageCount), 0, IKafkaConstants.MESSAGE_RATE);
    }

    static void runConsumer(String topicName) {
        Consumer<Long, String> consumer = ConsumerCreator.createConsumer(topicName);

        int noMessageFound = 0;

        while (true) {
            ConsumerRecords<Long, String> consumerRecords = consumer.poll(1000);
            // 1000 is the time in milliseconds consumer will wait if no record is found at broker.
            if (consumerRecords.count() == 0) {
                noMessageFound++;
                if (noMessageFound > IKafkaConstants.MAX_NO_MESSAGE_FOUND_COUNT)
                    // If no message found count is reached to threshold exit loop.
                    break;
                else
                    continue;
            }

            //print each record.
//            consumerRecords.forEach(record -> {
//                System.out.println("Record Key " + record.key());
//                System.out.println("Record value " + record.value());
//                System.out.println("Record partition " + record.partition());
//                System.out.println("Record offset " + record.offset());
//            });

            // commits the offset of record to broker.
            consumer.commitAsync();
        }
        consumer.close();
    }

    static void emptyTopic() {

    }

}
