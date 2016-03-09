package org.spacebison.multimic.net.message.gson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.spacebison.multimic.net.message.Hello;
import org.spacebison.multimic.net.message.Message;
import org.spacebison.multimic.net.message.NtpRequest;
import org.spacebison.multimic.net.message.NtpResponse;
import org.spacebison.multimic.net.message.StartRecord;
import org.spacebison.multimic.net.message.StopRecord;

import java.lang.reflect.Type;

/**
 * Created by cmb on 08.03.16.
 */
public class MessageDeserializer implements JsonDeserializer<Message> {
    private static final Gson GSON = new Gson();

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Message message = GSON.fromJson(json, Message.class);
        switch (message.getType()) {
            case START_RECORD:
                return context.deserialize(json, StartRecord.class);

            case STOP_RECORD:
                return context.deserialize(json, StopRecord.class);

            case HELLO:
                return context.deserialize(json, Hello.class);

            case NTP_REQUEST:
                return context.deserialize(json, NtpRequest.class);

            case NTP_RESPONSE:
                return context.deserialize(json, NtpResponse.class);

            default:
                return message;
        }
    }
}
