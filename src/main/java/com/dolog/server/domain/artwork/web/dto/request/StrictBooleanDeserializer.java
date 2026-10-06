package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

// Jackson 기본값은 "true", 1 같은 값도 Boolean 으로 바꿔 받아서, JSON true/false 만 허용하려고 쓴다.
public class StrictBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_TRUE) return true;
        if (token == JsonToken.VALUE_FALSE) return false;
        return (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
    }
}
