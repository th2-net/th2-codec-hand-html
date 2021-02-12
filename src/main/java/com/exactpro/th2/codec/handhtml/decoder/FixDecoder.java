package com.exactpro.th2.codec.handhtml.decoder;

import com.exactpro.th2.codec.handhtml.processor.FixProcessor;
import com.exactpro.th2.common.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixDecoder {

    private final FixProcessor fixProcessor;

    public FixDecoder (FixProcessor fixProcessor) {
        this.fixProcessor = fixProcessor;
    }

    public MessageGroup decode (MessageGroup group) {
        MessageGroup.Builder messageGroupBuilder = MessageGroup.newBuilder();

        for (var msg : group.getMessagesList()) {
            try {
                if (msg.hasMessage()) {
                    messageGroupBuilder.addMessages(AnyMessage.newBuilder().setMessage(msg.getMessage()).build());
                }
                Message m = fixProcessor.process(msg.getRawMessage());
                messageGroupBuilder.addMessages(AnyMessage.newBuilder().setMessage(m).build());
            } catch (Exception e) {
                log.error("Exception decoding message", e);

                return null;
            }
        }

        log.info("Finished decoding RawMessages");
        return messageGroupBuilder.build();
    }
}
