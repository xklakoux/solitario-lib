package com.xklakoux.solitariolib.views;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CardSerializer implements JsonSerializer<Card> {

	@Override
	public JsonElement serialize(Card arg0, Type arg1, JsonSerializationContext arg2) {
		final JsonObject json = new JsonObject();
		json.addProperty("suit", arg0.getSuit().ordinal());
		json.addProperty("rank", arg0.getRank().ordinal());
		json.addProperty("faceup", arg0.isFaceup());
		return json;
	}

}