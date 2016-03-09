package org.spacebison.multimic.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.spacebison.multimic.net.message.Message;
import org.spacebison.multimic.net.message.gson.MessageDeserializer;

/**
 * Created by cmb on 08.03.16.
 */
public class GsonHelper {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Message.class, new MessageDeserializer()).create();

    public static Gson getGson() {
        return GSON;
    }
}
