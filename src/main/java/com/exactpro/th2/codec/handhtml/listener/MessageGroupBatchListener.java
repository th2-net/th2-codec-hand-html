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

import com.exactpro.th2.codec.handhtml.decoder.FixDecoder;
import com.exactpro.th2.common.grpc.*;
import com.exactpro.th2.common.schema.message.MessageListener;
import com.exactpro.th2.common.schema.message.MessageRouter;
import lombok.extern.slf4j.Slf4j;

/*
    Listener receives RawMessages, uses FixDecoder and
    sends generated MessageBatch via MessageRouter
 */

@Slf4j
public class MessageGroupBatchListener implements MessageListener<MessageGroupBatch> {

    private MessageRouter<MessageGroupBatch> batchGroupRouter;
    private FixDecoder fixDecoder;

    public MessageGroupBatchListener(MessageRouter<MessageGroupBatch> batchGroupRouter, FixDecoder fixDecoder) {
        this.batchGroupRouter = batchGroupRouter;
        this.fixDecoder = fixDecoder;
    }

    @Override
    public void handler(String consumerTag, MessageGroupBatch message) {

        MessageGroupBatch.Builder outputBatchBuilder = MessageGroupBatch.newBuilder();

        if (message.getGroupsCount() != 1) {
            log.error("Received batch has more than one group!, router will not send anything!");
            return;
        }

        try {
            MessageGroup messageGroup = fixDecoder.decode(message.getGroups(0));


            if (messageGroup == null) {
                log.info("Exception happened during decoding, router won't send anything");
                return;
            }

            if (messageGroup.getMessagesCount() == 0) {
                log.info("Messages weren't found in this batch, router won't send anything");
                return;
            }

            batchGroupRouter.sendAll(outputBatchBuilder.addGroups(messageGroup).build());
        } catch (Exception e) {
            log.error("Exception sending message(s)", e);
        }
    }

    @Override
    public void onClose() {

    }
}
