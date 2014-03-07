package com.xklakoux.solitariolib.views;

import java.lang.reflect.Type;

import android.content.Context;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.xklakoux.solitariolib.enums.Rank;
import com.xklakoux.solitariolib.enums.Suit;

public class CardDeserializer implements JsonDeserializer<Card> {

	private final Context context;

	public CardDeserializer(Context context) {
		this.context=  context;
	}

	@Override
	public Card deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
		JsonObject object = arg0.getAsJsonObject();
		Suit suit = Suit.values()[object.get("suit").getAsInt()];
		Rank rank = Rank.values()[object.get("rank").getAsInt()];
		Boolean faceUp = object.get("faceup").getAsBoolean();
		Card card = new Card(context, suit, rank, faceUp);
		return card;
	}
}