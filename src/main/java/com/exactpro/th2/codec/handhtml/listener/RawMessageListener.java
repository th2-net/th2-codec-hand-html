/*
 Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.exactpro.th2.codec.handhtml.listener;

import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageBatch;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.schema.message.MessageListener;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.codec.handhtml.parser.FixProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RawMessageListener implements MessageListener<RawMessageBatch> {

    private MessageRouter<MessageBatch> batchMessageRouter;
    private FixProcessor fixProcessor;

    public RawMessageListener (MessageRouter<MessageBatch> batchMessageRouter, FixProcessor fixProcessor) {
        this.batchMessageRouter = batchMessageRouter;
        this.fixProcessor = fixProcessor;
    }

    @Override
    public void handler(String consumerTag, RawMessageBatch message) {

        try {
            List<Message> parsedMessages = new ArrayList<>();

            for (var msg : message.getMessagesList()) {
                try {
                    Message m = fixProcessor.process(msg);
                    parsedMessages.add(m);
                } catch (Exception e) {
                    log.error("Exception parsing message", e);
                }
            }

            log.info("Sending processed message");
            batchMessageRouter.send(MessageBatch.newBuilder().addAllMessages(parsedMessages).build());
        } catch (Exception e) {
            log.error("Exception processing message(s)", e);
        }
    }

    @Override
    public void onClose() {

    }
}
