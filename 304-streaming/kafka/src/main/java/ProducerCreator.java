import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class ProducerCreator extends TimerTask {

    private String productionMode;
    private Producer<Long, String> producer;
    private String topicName;
    private Integer messageCount;

    private BufferedReader br;

    public ProducerCreator(String productionMode, String topicName, Integer messageCount) throws FileNotFoundException {
        this.productionMode = productionMode;
        this.topicName = topicName;
        this.messageCount = messageCount;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, IKafkaConstants.KAFKA_BROKERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, IKafkaConstants.CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class.getName());
        producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String topicName, String message){
        ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(topicName,message);
        try {
            RecordMetadata metadata = producer.send(record).get();
//                            System.out.println("Record sent with key " + index + " to partition " + metadata.partition()
//                                    + " with offset " + metadata.offset());
        }
        catch (ExecutionException e) {
            System.out.println("Error in sending record");
            System.out.println(e);
        }
        catch (InterruptedException e) {
            System.out.println("Error in sending record");
            System.out.println(e);
        }
    }

    @Override
    public void run() {
        switch(productionMode){
            case "tweets":
                try {
                    br = new BufferedReader(new FileReader(IKafkaConstants.DATASET_TWEET_PATH));
                    int i = 0;
                    double skipRange = Math.max(0, 10000 - messageCount);
                    double nMessagesToSkip = Math.random() * skipRange;

                    System.out.println("Sending " + messageCount + " messages");

                    br.readLine();
                    while (nMessagesToSkip-- > 1) {
                        br.readLine();
                    }

                    String line = br.readLine();
                    while (line != null && i++ < messageCount) {
                        sendMessage(topicName,line);
                        line = br.readLine();
                    }
                    br.close();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            case "got": // Java faker - Game of Thrones quotes
                System.out.println("Sending " + messageCount + " messages to " + topicName);
                Faker faker = new Faker();
                for(int i=0; i<messageCount; i++){
                    sendMessage(topicName,faker.gameOfThrones().quote());
                }
                return;
            case "lorem": // Java faker - Lorem Ipsum phrases
                System.out.println("Sending " + messageCount + " messages to " + topicName);
                Faker faker1 = new Faker();
                for(int i=0; i<messageCount; i++){
                    sendMessage(topicName,faker1.lorem().sentence());
                }
                return;
        }
    }
}