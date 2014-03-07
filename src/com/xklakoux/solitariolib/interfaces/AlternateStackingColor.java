package com.xklakoux.solitariolib.interfaces;

import com.xklakoux.solitariolib.enums.Suit;
import com.xklakoux.solitariolib.views.Card;

/**
 * @author artur
 *
 */
public class AlternateStackingColor implements StackingColor {

	@Override
	public boolean isRight(Card cardBelow, Card cardOver) {

		if (((cardBelow.getSuit() == Suit.CLUBS || cardBelow.getSuit() == Suit.SPADES) && (cardOver.getSuit() == Suit.HEARTS || cardOver
				.getSuit() == Suit.DIAMONDS))
				|| ((cardBelow.getSuit() == Suit.HEARTS || cardBelow.getSuit() == Suit.DIAMONDS) && (cardOver
						.getSuit() == Suit.SPADES || cardOver.getSuit() == Suit.CLUBS))) {
			return true;
		}else {
			return false;
		}

	}
}
