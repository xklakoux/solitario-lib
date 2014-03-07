package com.xklakoux.solitariolib.views;

import java.lang.reflect.Type;

import android.content.Context;

import com.google.gson.InstanceCreator;
import com.xklakoux.solitariolib.enums.Rank;
import com.xklakoux.solitariolib.enums.Suit;

public class CardInstanceCreator implements InstanceCreator<Card> {

	private final Context context;

	public CardInstanceCreator(Context context) {
		this.context=  context;
	}

	@Override
	public Card createInstance(Type arg0) {
		return new Card(context, Suit.SPADES, Rank.ACE);
	}

}