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

package com.exactpro.th2.codec.handhtml.parser;

import com.exactpro.sf.common.util.Pair;
import com.exactpro.th2.common.grpc.*;
import com.exactpro.th2.codec.handhtml.util.HtmlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.stream.Collectors;

/*
    Following class does html parsing and
    message generation, logic might split into
    two different classes in near future
 */

@Slf4j
public class FixProcessor {

    private FixHtmlParserConfiguration configuration;

    public FixProcessor (FixHtmlParserConfiguration configuration) {
        this.configuration = configuration;
    }

    public Message process (RawMessage rawMessage) throws Exception {
        ObjectMapper objectMapper;

        objectMapper = new ObjectMapper();
        String body = new String(rawMessage.getBody().toByteArray());
        HashMap<String, String> jsonMap = objectMapper.readValue(body, HashMap.class);

        Document document = Jsoup.parse(jsonMap.get(configuration.getContentKey()));

        Element table = HtmlUtils.findChildTable(document);
        Element messageTypeElement = HtmlUtils.findChildWithClass(document, configuration.getMessageTypeElClassName());
        String messageType = messageTypeElement == null ? "Undefined" : messageTypeElement.text();

        Map<String, Object> fieldMap = parse(table);

        if (fieldMap == null) {
            log.error("Parser could not process html data");
            throw new Exception("Could not parse the html data");
        }

        fieldMap = (Map) adjustCollections(fieldMap);

        Message subMessage = generateSubMessage(fieldMap);

        if (subMessage == null) {
            log.error("Processor could not process the hierarchy");
            throw new Exception("Processor could not process the hierarchy");
        }

        return generateMessage(messageType, rawMessage.getMetadata(), jsonMap, subMessage);
    }

    /*
        Following method produces main message,
        which process method returns
     */

    private Message generateMessage (String messageType, RawMessageMetadata rawMessageMetadata, Map <String, String> fields, Message subMessage) {
        Message.Builder builder = Message.newBuilder();

        MessageMetadata metadata = MessageMetadata.newBuilder()
                .setId(rawMessageMetadata.getId())
                .setMessageType(messageType)
                .setTimestamp(rawMessageMetadata.getTimestamp())
                .build();

        builder.setMetadata(metadata);

        for (var field : fields.entrySet()) {
            if (field.getKey().equals(configuration.getContentKey())) {
                builder.putFields(configuration.getContentKey(), Value.newBuilder().setMessageValue(subMessage).build());
            } else {
                builder.putFields(field.getKey(), Value.newBuilder().setSimpleValue(field.getValue()).build());
            }
        }

        return builder.build();
    }

    /*
        Generates subMessage, which contains
        actual fields and hierarchy from html table
     */

    private Message generateSubMessage (Map <String, Object> fields) {
        return buildParsedMessage(fields).build();

    }

    /*
        Parses html table and constructs Map
        hierarchy from it
     */

    private Map<String, Object> parse(Element table) {
        Element tableBody = table.child(1);

        Stack<Map<String, Object>> stack = new Stack<>();
        stack.add(new HashMap<>());

        int prevDepth = configuration.getHierarchyStart(), curDepth = configuration.getHierarchyStart();
        for (Element row : tableBody.children()) {

            Element fieldName = row.child(0);

            // Indicator that internal fields will be coming
            if (row.childrenSize() == 1) {
                curDepth = Integer.parseInt(fieldName.child(0).attr(configuration.getHierarchyAttribute())
                        .split(configuration.getHierarchyIndicatorPrefix())[1]
                        .split(configuration.getHierarchyIndicatorSuffix())[0]);

                /*
                    Pop maps from stack, because their fields are over
                 */
                while (curDepth < prevDepth) {
                    stack.pop();
                    prevDepth -= configuration.getHierarchyStep();
                }
                prevDepth = curDepth;


                /*
                    Creation of hierarchy's new layer
                    and adding it as one of the fields of last Map
                 */
                Map<String, Object> childHMap = new HashMap<>();
                stack.peek().put(fieldName.text(), childHMap);

                stack.add(childHMap);
                continue;
            }

            curDepth = Integer.parseInt(fieldName.attr(configuration.getHierarchyAttribute())
                    .split(configuration.getHierarchyIndicatorPrefix())[1]
                    .split(configuration.getHierarchyIndicatorSuffix())[0]);

            /*
                Same logic as above
             */
            while (curDepth < prevDepth) {
                stack.pop();
                prevDepth -= configuration.getHierarchyStep();
            }
            prevDepth = curDepth;

            /*
                At this point it's guaranteed that
                we are putting non-complex value
             */
            Element fieldValue = row.child(1);
            stack.peek().put(fieldName.text(), fieldValue.text());
        }


        /*
            The first map will be the root Map
         */
        while (stack.size() > 1) {
            stack.pop();
        }

        return stack.peek();
    }

    private Message.Builder buildParsedMessage(Map<String, Object> fields) {
        Map<String, Value> messageFields = new LinkedHashMap<>(fields.size());
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            messageFields.put(entry.getKey(), parseObj(entry.getValue()));
        }

        return Message.newBuilder().putAllFields(messageFields);
    }

    private Value parseObj(Object value) {
        /*
            If current value is Map-like
             we should create new Message from it
         */
        if (value instanceof Map) {
            Message.Builder msgBuilder = Message.newBuilder();
            for (Map.Entry<?, ?> o1 : ((Map<?, ?>) value).entrySet()) {
                msgBuilder.putFields(String.valueOf(o1.getKey()), parseObj(o1.getValue()));
            }
            return Value.newBuilder().setMessageValue(msgBuilder.build()).build();
        }

        if (value instanceof List) {
            ListValue.Builder listValueBuilder = ListValue.newBuilder();
            for (Object o : ((List<?>) value)) {

                if (o instanceof Map) {
                    Message.Builder msgBuilder = Message.newBuilder();
                    for (Map.Entry<?, ?> o1 : ((Map<?, ?>) o).entrySet()) {
                        msgBuilder.putFields(String.valueOf(o1.getKey()), parseObj(o1.getValue()));
                    }
                    listValueBuilder.addValues(Value.newBuilder().setMessageValue(msgBuilder.build()));
                } else {
                    listValueBuilder.addValues(Value.newBuilder().setSimpleValue(String.valueOf(o)));
                }
            }
            return Value.newBuilder().setListValue(listValueBuilder).build();
        } else {
            return Value.newBuilder().setSimpleValue(String.valueOf(value)).build();
        }
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


