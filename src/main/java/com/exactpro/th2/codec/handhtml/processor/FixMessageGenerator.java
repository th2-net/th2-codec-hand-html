package com.exactpro.th2.codec.handhtml.processor;

import com.exactpro.th2.common.grpc.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
    Constructs Message Objects from
    provided Fix data
 */

public class FixMessageGenerator {
    /*
        Following method produces main message,
        with all original fields and values except
        newly generated subMessage
    */

    public static Message generateMessage (String messageType, RawMessageMetadata rawMessageMetadata,
                                            Map<String, String> fields, String contentKey, Message subMessage) {
        Message.Builder builder = Message.newBuilder();

        MessageMetadata metadata = MessageMetadata.newBuilder()
                .setId(rawMessageMetadata.getId())
                .setMessageType(messageType)
                .setTimestamp(rawMessageMetadata.getTimestamp())
                .build();

        builder.setMetadata(metadata);

        for (var field : fields.entrySet()) {
            if (field.getKey().equals(contentKey)) {
                builder.putFields(contentKey
                        , Value.newBuilder().setMessageValue(subMessage).build());
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

    public static Message generateSubMessage (Map <String, Object> fields) {
        return buildParsedMessage(fields).build();

    }

    private static Message.Builder buildParsedMessage(Map<String, Object> fields) {
        Map<String, Value> messageFields = new LinkedHashMap<>(fields.size());
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            messageFields.put(entry.getKey(), parseObj(entry.getValue()));
        }

        return Message.newBuilder().putAllFields(messageFields);
    }

    private static Value parseObj(Object value) {
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

}
