package com.fptu.sep490.personalservice.helper;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {
    @Override
    public LocalTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.isTextual()) {
            // "HH:mm" hoặc "HH:mm:ss"
            return LocalTime.parse(node.textValue());
        }

        if (node.isObject()) {
            int hour   = node.has("hour")   ? node.get("hour").asInt()   : 0;
            int minute = node.has("minute") ? node.get("minute").asInt() : 0;
            int second = node.has("second") ? node.get("second").asInt() : 0;
            int nano   = node.has("nano")   ? node.get("nano").asInt()   : 0;
            return LocalTime.of(hour, minute, second, nano);
        }

        throw new AppException(Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                Constants.ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
