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
import com.exactpro.th2.common.grpc.MessageBatch;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.schema.message.MessageListener;
import com.exactpro.th2.common.schema.message.MessageRouter;
import lombok.extern.slf4j.Slf4j;

/*
    Listener receives RawMessages, uses FixProcessor and
    sends generated messages via MessageRouter
 */

/*
    Listener receives RawMessages, uses FixProcessor and
    sends generated messages via MessageRouter
 */

@Slf4j
public class RawMessageListener implements MessageListener<RawMessageBatch> {

    private MessageRouter<MessageBatch> batchMessageRouter;
    private FixDecoder fixDecoder;

    public RawMessageListener (MessageRouter<MessageBatch> batchMessageRouter, FixDecoder fixDecoder) {
        this.batchMessageRouter = batchMessageRouter;
        this.fixDecoder = fixDecoder;
    }

    @Override
    public void handler(String consumerTag, RawMessageBatch message) {

        try {
            batchMessageRouter.send(fixDecoder.decode(message));
        } catch (Exception e) {
            log.error("Exception sending message(s)", e);
        }
    }

    @Override
    public void onClose() {

    }
}
