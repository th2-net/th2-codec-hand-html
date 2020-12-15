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

package com.exactpro.th2.codec.handhtml.processor;

import com.exactpro.sf.common.util.Pair;
import com.exactpro.th2.codec.handhtml.util.HtmlUtils;
import com.exactpro.th2.common.grpc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.stream.Collectors;

/*
    Following class does RawMessage processing
    and Message generation
 */

@Slf4j
public class FixProcessor {
    private final FixHtmlProcessorConfiguration configuration;

    private final HtmlUtils.TagNameChecker tableChecker;
    private final HtmlUtils.ClassNameChecker messageTypeChecker;

    public FixProcessor (FixHtmlProcessorConfiguration configuration) {
        this.configuration = configuration;

        this.tableChecker = new HtmlUtils.TagNameChecker("table");
        this.messageTypeChecker = new HtmlUtils.ClassNameChecker(configuration.getMessageTypeElClassName());

    }

    public Message process (RawMessage rawMessage) throws Exception {
        ObjectMapper objectMapper;

        objectMapper = new ObjectMapper();
        String body = new String(rawMessage.getBody().toByteArray());
        HashMap<String, String> jsonMap = objectMapper.readValue(body, HashMap.class);

        Document document = Jsoup.parse(jsonMap.get(configuration.getContentKey()));

        Element table = HtmlUtils.traverseSubtree(document, tableChecker);
        Element messageTypeElement = HtmlUtils.traverseSubtree(document, messageTypeChecker);
        String messageType = messageTypeElement == null ? "Undefined" : messageTypeElement.text();

        if (table == null) {
            throw new Exception("Could not find table in raw message");
        }

        Map<String, Object> fieldMap = HtmlUtils.parse(table, configuration);

        if (fieldMap == null) {
            throw new Exception("Could not parse the html data");
        }

        //TODO: Use Dictionary for this purpose
        fieldMap = (Map) adjustCollections(fieldMap);

        Message subMessage = FixMessageGenerator.generateSubMessage(fieldMap);

        if (subMessage == null) {
            throw new Exception("Processor could not process the hierarchy");
        }

        return FixMessageGenerator.generateMessage (messageType, rawMessage.getMetadata(), jsonMap, configuration.getContentKey(), subMessage);
    }

    /*
        Simple function to check whether a map
        can be converted into list
     */

    private boolean hasSequentialElements (Set<String> set) {
        List<String> ls = set.stream().sorted().collect(Collectors.toList());
        if (ls.size() == 0) {
            return true;
        }

        for (int i = 0; i < ls.size(); i ++) {
            if (!ls.get(i).matches("[0-9]+")) {
                return false;
            }

            if (i != Integer.parseInt(ls.get(i))) {
                return false;
            }
        }

        return true;
    }

    /*
        Recursive function, which converts
        any list-like (keys are consecutive numbers starting with 0)
        map into list and setts it into parent
     */

    private Object adjustCollections (Map <String, Object> map) {
        List <Pair <String, Object> > updatedEntries = new ArrayList<>();

        for (var entry : map.entrySet()) {
            var key = entry.getKey();
            var val = entry.getValue();

            if (val instanceof HashMap) {
                var newVal = adjustCollections((HashMap) val);

                if (newVal instanceof ArrayList) {
                    updatedEntries.add(new Pair<>(key, newVal));
                }
            }
        }

        for (var pair : updatedEntries) {
            map.put(pair.getFirst(), pair.getSecond());
        }

        if (hasSequentialElements (map.keySet())) {
            ArrayList <Object> ls = new ArrayList<>();

            for (var key : map.keySet().stream().sorted().collect(Collectors.toList())) {
                ls.add(map.get(key));
            }

            return ls;
        }

        return map;
    }
}


