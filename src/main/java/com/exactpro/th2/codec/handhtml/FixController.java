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

package com.exactpro.th2.codec.handhtml;

import com.exactpro.th2.codec.handhtml.decoder.FixDecoder;
import com.exactpro.th2.codec.handhtml.listener.RawMessageListener;
import com.exactpro.th2.common.schema.factory.CommonFactory;
import com.exactpro.th2.codec.handhtml.processor.FixHtmlProcessorConfiguration;
import com.exactpro.th2.codec.handhtml.processor.FixProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.exactpro.th2.common.metrics.CommonMetrics.setLiveness;
import static com.exactpro.th2.common.metrics.CommonMetrics.setReadiness;

@Slf4j
public class FixController {

    private static volatile List<AutoCloseable> resources;

    public static void main(String[] args) {

        setLiveness(true);
        resources = new ArrayList<>();

        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        try {
            CommonFactory factory = CommonFactory.createFromArguments();
            resources.add(factory);

            var parsedBatchRouter = factory.getMessageRouterParsedBatch();
            resources.add(parsedBatchRouter);

            var rawBatchRouter = factory.getMessageRouterRawBatch();
            resources.add(rawBatchRouter);

            configureShutdownHook(lock, condition);
            setReadiness(true);

            FixHtmlProcessorConfiguration fixHtmlProcessorConfiguration = factory.getCustomConfiguration(FixHtmlProcessorConfiguration.class);
            FixProcessor fixProcessor = new FixProcessor(fixHtmlProcessorConfiguration);

            FixDecoder fixDecoder = new FixDecoder(fixProcessor);
            RawMessageListener rawMessageListener = new RawMessageListener(parsedBatchRouter, fixDecoder);
            rawBatchRouter.subscribeAll(rawMessageListener);

            awaitShutdown(lock, condition);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void awaitShutdown(ReentrantLock lock, Condition condition) throws InterruptedException {
        try {
            lock.lock();
            log.info("Wait shutdown");
            condition.await();
            log.info("App shutdown");
        } finally {
            lock.unlock();
        }
    }

    private static void configureShutdownHook (ReentrantLock lock, Condition condition) {
        Runtime.getRuntime().addShutdownHook(new Thread("Shutdown hook") {
            @Override
            public void run() {
                log.info("Shutdown start");
                setReadiness(false);

                try {
                    lock.lock();
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }

                for (var resource : resources) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        log.error("Exception closing resource {}", resource.getClass().getSimpleName(), e);
                    }
                }

                log.info("Shutdown end");
                setLiveness(false);
            }
        });
    }
}
