package com.xklakoux.solitariolib.interfaces;

import com.xklakoux.solitariolib.views.Card;

/**
 * @author artur
 *
 */
public interface StackingColor {

	boolean isRight(Card cardBelow, Card cardOver);

}
