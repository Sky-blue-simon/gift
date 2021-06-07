package lecture;

import lecture.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired GiftRepository giftRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPointCanceled_CancelGift(@Payload PointCanceled pointCanceled){

        if(!pointCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelGift : " + pointCanceled.toJson() + "\n\n");

        // Sample Logic //
        Gift gift = new Gift();
        giftRepository.save(gift);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
