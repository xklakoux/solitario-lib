/**
 * 
 */
package com.xklakoux.solitariolib.views;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xklakoux.solitariolib.R;
import com.xklakoux.solitariolib.SettingsConstant;
import com.xklakoux.solitariolib.Utils;
import com.xklakoux.solitariolib.enums.Suit;
import com.xklakoux.solitariolib.enums.Rank;

/**
 * @author artur
 * 
 */
public class Card extends ImageView {

	public final String TAG = Card.class.getSimpleName();
	private final Context context;
	private final SharedPreferences prefs;
	private Suit suit;
	private Rank rank;
	private boolean faceup = false;
	private final int reverseResourceId;

	public Card(Context context, Suit suit, Rank rank) {
		super(context);
		this.suit = suit;
		this.rank = rank;
		setAdjustViewBounds(true);
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		reverseResourceId = (Utils.getResId(
				"reverse_" + prefs.getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE),
				R.drawable.class));
		setImageResource(reverseResourceId);
		setOnTouchListener(new CardTouchListener());

		// setId(Game.getUniqueId());
	}

	public Card(Context context, Suit suit, Rank rank, boolean faceUp) {
		super(context);
		this.suit = suit;
		this.rank = rank;
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		faceup = faceUp;
		setAdjustViewBounds(true);
		setOnTouchListener(new CardTouchListener());
		reverseResourceId = (Utils.getResId(
				"reverse_" + prefs.getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE),
				R.drawable.class));
		// setId(App.getUniqueId());
	}

	public Card(Context context, Card card) {
		super(context);
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		suit = card.getSuit();
		rank = card.getRank();
		faceup = true;
		reverseResourceId = (Utils.getResId(
				"reverse_" + prefs.getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE),
				R.drawable.class));

	}

	public Suit getSuit() {
		return suit;
	}

	public void setSuit(Suit suit) {
		this.suit = suit;
	}

	public Rank getRank() {
		return rank;
	}

	public void setRank(Rank rank) {
		this.rank = rank;
	}

	public boolean isFaceup() {
		return faceup;
	}

	public void setFaceup(boolean faceup) {
		this.faceup = faceup;
		if (faceup) {
			String index = prefs.getString(SettingsConstant.CARD_SET, SettingsConstant.DEFAULT_CARD_SET);
			setImageResource(Utils.getResId(suit.getName() + "_" + rank.getId() + "_" + index, R.drawable.class));
		} else {
			String index = prefs.getString(SettingsConstant.REVERSE, SettingsConstant.DEFAULT_REVERSE);
			setImageResource(Utils.getResId("reverse_" + index, R.drawable.class));
		}
	}

	class CardTouchListener implements OnTouchListener {

		public final String TAG = CardTouchListener.class.getSimpleName();

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			final int action = MotionEventCompat.getActionMasked(event);

			switch (action) {
			case MotionEvent.ACTION_DOWN:

				BasePile owner = (BasePile) v.getParent();
				int index = owner.indexOfChild(v);
				Card card = (Card) v;

				if (isValidMove((Card) v)) {
					ClipData data = ClipData.newPlainText("", "");
					for (int i = 0; i < index; i++) {
						owner.getChildAt(i).setVisibility(View.INVISIBLE);
					}
					DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(owner, card, event);
					if (v.startDrag(data, shadowBuilder, v, 0)) {
						for (int i = index; i < owner.getChildCount(); i++) {
							owner.getChildAt(i).setVisibility(View.INVISIBLE);
						}
					} else {
						for (int i = index; i < owner.getChildCount(); i++) {
							owner.getChildAt(i).setVisibility(View.VISIBLE);
						}
					}
					for (int i = 0; i < index; i++) {
						owner.getChildAt(i).setVisibility(View.VISIBLE);
					}
					return true;
				}
			}
			return true;
		}

		boolean isValidMove(Card selectedCard) {
			ViewGroup owner = (ViewGroup) selectedCard.getParent();
			int index = owner.indexOfChild(selectedCard);

			if (!selectedCard.isFaceup()) {
				return false;
			}

			Card referenceCard = selectedCard;
			for (int i = index + 1; i < owner.getChildCount(); i++) {
				Card card = (Card) owner.getChildAt(i);

				if (!(referenceCard.getSuit() == card.getSuit())
						|| !(referenceCard.getRank().getId() - 1 == (card.getRank().getId()))) {
					return false;
				}
				referenceCard = card;
			}
			return true;

		}

	}

	private class MyDragShadowBuilder extends View.DragShadowBuilder {

		BasePile pile;
		Card card;
		MotionEvent event;

		private MyDragShadowBuilder(BasePile pile, Card card, MotionEvent event) {
			super(pile);
			this.pile = pile;
			this.card = card;
			this.event = event;
		}

		@Override
		public void onProvideShadowMetrics(Point size, Point touch) {

			int width;
			int height;

			width = getView().getWidth();
			height = getView().getHeight();
			size.set(width, height);
			touch.set((int) event.getX(), calculateMarginTop(pile, card) + (int) event.getY());

		}

		private int calculateMarginTop(BasePile pile, Card cardStart) {
			int marginTop = 0;
			for (int i = 0; i < pile.indexOfCard(cardStart); i++) {
				Card card = pile.getCardAt(i);
				float tempMargin = card.isFaceup() ? getResources().getDimension(R.dimen.card_stack_margin_up)
						: getResources().getDimension(R.dimen.card_stack_margin_down);
				marginTop += tempMargin;
			}
			return marginTop;
		}
	}
}