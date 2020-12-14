package com.exactpro.th2.codec.handhtml.decoder;

import com.exactpro.th2.codec.handhtml.processor.FixProcessor;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageBatch;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FixDecoder {

    private final FixProcessor fixProcessor;

    public FixDecoder (FixProcessor fixProcessor) {
        this.fixProcessor = fixProcessor;
    }

    public MessageBatch decode (RawMessageBatch rawMessageBatch) {
        List<Message> decodedMessages = new ArrayList<>();

        for (var msg : rawMessageBatch.getMessagesList()) {
            try {
                Message m = fixProcessor.process(msg);
                decodedMessages.add(m);
            } catch (Exception e) {
                log.error("Exception decoding message", e);
            }
        }

        log.info("Finished decoding RawMessages");
        return MessageBatch.newBuilder().addAllMessages(decodedMessages).build();
    }
}
