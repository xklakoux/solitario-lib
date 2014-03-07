package com.xklakoux.solitariolib;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xklakoux.solitariolib.enums.Difficulty;
import com.xklakoux.solitariolib.enums.GameState;
import com.xklakoux.solitariolib.enums.Rank;
import com.xklakoux.solitariolib.enums.Suit;
import com.xklakoux.solitariolib.views.BasePile;
import com.xklakoux.solitariolib.views.Card;
import com.xklakoux.solitariolib.views.CardDeserializer;
import com.xklakoux.solitariolib.views.Stock;

/**
 * @author artur
 * 
 */
public abstract class BaseGameActivity extends Activity implements OnSharedPreferenceChangeListener{

	protected SharedPreferences prefs;

	protected Difficulty chosenDifficulty;
	protected String chosenOrientation;
	protected float chosenAnimationSpeed;
	protected boolean chosenSound;
	protected boolean chosenUnrestrictedDeal;
	protected boolean chosenUnrestrictedUndo;
	protected boolean chosenHints;

	protected TextView winner;
	protected RelativeLayout statsLayout;

	protected int cardWidth;
	protected int cardHeight;

	protected boolean onCreate = false;


	protected final int DELAY = 2000;
	protected final Handler handler = new Handler();
	protected final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			getActionBar().hide();
		};

	};

	private RelativeLayout root;

	static List<BasePile> tableauPiles;

	private Stock stock;
	private ScrollView scrollPiles;
	private LinearLayout piles;
	private RelativeLayout hollowPile;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_layout);
		prefs = PreferenceManager.getDefaultSharedPreferences(Game.getAppContext());

		initSettings();
		findViewsSetListenersAndManagers();
		fixLayoutOrientation(getResources().getConfiguration().orientation);
		if (!Game.getMoves().isEmpty()) {
			restoreGame();
		}
		getCardDimensions(tableauPiles.get(0).getFirstCardSpot());
		Game.getBus().register(this);
		handler.postDelayed(runnable, DELAY);

	}


	protected abstract List<BasePile> getFoundationPiles();
	protected abstract List<BasePile> getTableauPiles();
	protected abstract List<BasePile> getDeck();
	protected abstract List<BasePile> getCellPiles();



	private void findViewsSetListenersAndManagers() {
		winner = (TextView) findViewById(R.id.winner);

		piles = (LinearLayout) findViewById(R.id.piles);
		//		hollowPile = (BasePile) findViewById(R.id.hollowPile);
		statsLayout = (RelativeLayout) findViewById(R.id.stats);


		tableauPiles = getTableauPiles();
		//		tableauPiles.add((BasePile) findViewById(R.id.pile0));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile1));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile2));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile3));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile4));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile5));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile6));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile7));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile8));
		//		tableauPiles.add((BasePile) findViewById(R.id.pile9));

		int i=0;
		for(BasePile pile: tableauPiles) {
			pile.setTag(i++);
		}

		stock = (Stock) findViewById(R.id.deck);
		stock.setOnClickListener(new OnDeckClickListener());
		stock.setSize(cardWidth, cardHeight);

		root = (RelativeLayout) findViewById(R.id.root);

		TextView score = (TextView) findViewById(R.id.tvScore);
		TextView movesCount = (TextView) findViewById(R.id.tvMoves);
		Chronometer time = (Chronometer) findViewById(R.id.tvTimeElapsed);
		Game.setStatsManager(new StatsManager(score, movesCount, time));

		scrollPiles = (ScrollView) findViewById(R.id.scrollPiles);
		scrollPiles.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setActionBarVisibility();
				}
				return false;
			}
		});
		prefs = Game.getSettings();
		prefs.registerOnSharedPreferenceChangeListener(this);

		refreshResources();
	}


	class OnDeckClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {

			if (stock.isEmpty()) {
				return;
			}
			if (!chosenUnrestrictedDeal && !cardsCorrect()) {
				return;
			}
			if (Game.getState() != GameState.STARTED) {
				return;
			}
			int i = 0;
			Game.setState(GameState.DEALING);
			for (BasePile pile : tableauPiles) {
				Card card = stock.getCard();
				dealCard(pile, card, true, i++, false);
			}

			Game.getMoves().add(new Move(Move.ACTION_DEAL));
		}

		/**
		 * @return
		 */
		private boolean cardsCorrect() {
			for (BasePile pile : tableauPiles) {
				if (pile.getCardsCount() < 1) {
					Toast.makeText(BaseGameActivity.this, R.string.all_tableaus_should_be_filled, Toast.LENGTH_SHORT)
					.show();
					return false;
				}
			}
			return true;
		}

	}


	private void dealCard(BasePile pile, Card card, boolean faceup, int hundredMillisecondOffset, boolean isNewGameDeal) {

		int[] deckLocation = new int[2];
		stock.getLastVisible().getLocationOnScreen(deckLocation);
		int statusBarOffsetY = Utils.getStatusBarOffset(BaseGameActivity.this, root);

		int[] location = new int[2];
		pile.getLastTrueChild().getLocationOnScreen(location);
		final Card fakeCard = new Card(BaseGameActivity.this, Suit.SPADES, Rank.ACE);
		root.addView(fakeCard);
		setCardSize(fakeCard);
		fakeCard.setOnTouchListener(null);

		float horizontalMargin = getResources().getDimension(R.dimen.activity_horizontal_margin);
		float verticalMargin = getResources().getDimension(R.dimen.activity_vertical_margin);

		int cardsOffset = hundredMillisecondOffset / tableauPiles.size();
		float stackMargin = 0;
		if (isNewGameDeal) {
			stackMargin = getResources().getDimension(R.dimen.card_stack_margin_down) * cardsOffset;

		} else {
			stackMargin = getResources().getDimension(R.dimen.card_stack_margin_up);
		}
		float fromX = deckLocation[0] - horizontalMargin;
		float toX = location[0] - horizontalMargin;
		float fromY = deckLocation[1] - statusBarOffsetY - verticalMargin;
		float toY = location[1] - statusBarOffsetY - verticalMargin + stackMargin;

		Animation anim = new TranslateAnimation(Animation.ABSOLUTE, fromX, Animation.ABSOLUTE, toX, Animation.ABSOLUTE,
				fromY, Animation.ABSOLUTE, toY);

		anim.setInterpolator(new DecelerateInterpolator());
		anim.setDuration((long) (700.0 * chosenAnimationSpeed) + 100);
		int startOffset = hundredMillisecondOffset * ((int) (100.0 * chosenAnimationSpeed));
		anim.setStartOffset(startOffset);
		anim.setAnimationListener(new DealAnimationListener(pile, card, root, fakeCard));
		anim.setZAdjustment(Animation.ZORDER_TOP);
		fakeCard.setAnimation(anim);

		card.setFaceup(faceup);
	}

	class DealAnimationListener implements AnimationListener {

		private final BasePile container;
		private final Card card;
		private final Card fakeAnimCard;
		private final RelativeLayout root;

		public DealAnimationListener(BasePile container, Card card, RelativeLayout root, Card fakeAnimCard) {
			this.container = container;
			this.card = card;
			this.fakeAnimCard = fakeAnimCard;
			this.root = root;
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			container.addCard(card);
			if (container.checkState(true)) {
				Move lastMove = Game.getMoves().get(Game.getMoves().size() - 1);
				lastMove.setCompleted(true);
				lastMove.setSuit(card.getSuit());
				lastMove.setTo(tableauPiles.indexOf(container));
			}
			root.removeView(fakeAnimCard);

			Game.addCardDealt();
			if (Game.isFullDeal()) {
				Game.setState(GameState.STARTED);
			}
			if (Game.isStartDeal()) {
				Game.getStatsManager().clearStatsAndGo();
				getActionBar().hide();
			}
			if (chosenSound) {
				Game.playSound(Game.SOUND_PUT_CARD);
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationStart(Animation animation) {
			fakeAnimCard.setVisibility(View.VISIBLE);
			fakeAnimCard.bringToFront();
			root.requestLayout();
			root.invalidate();
		}
	}

	@SuppressLint("DefaultLocale")
	private void initSettings() {
		prefs = Game.getSettings();

		chosenDifficulty = Difficulty.valueOf(prefs.getString(SettingsConstant.DIFFICULTY, "EASY").toUpperCase());

		chosenOrientation = prefs.getString(SettingsConstant.ORIENTATION, SettingsConstant.DEFAULT_ORIENTATION);
		if (chosenOrientation.equals("horizontal")) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		} else if (chosenOrientation.equals("vertical")) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}

		chosenSound = prefs.getBoolean(SettingsConstant.SOUNDS, true);
		chosenUnrestrictedDeal = prefs.getBoolean(SettingsConstant.UNRES_DEAL, false);
		chosenUnrestrictedUndo = prefs.getBoolean(SettingsConstant.UNRES_UNDO, false);

		chosenHints = prefs.getBoolean(SettingsConstant.HINTS, true);

		String speed = prefs.getString(SettingsConstant.ANIMATION, "slow");
		if (speed.equals("slow")) {
			chosenAnimationSpeed = 1.0f;
		} else if (speed.equals("fast")) {
			chosenAnimationSpeed = 0.5f;
		} else if (speed.equals("superfast")) {
			chosenAnimationSpeed = 0.0f;
		}
	}

	@SuppressWarnings("deprecation")
	public void refreshResources() {

		for (BasePile pile : tableauPiles) {
			pile.refresh();
		}
		stock.refresh();
		String backgroundResName = Game.getSettings().getString(SettingsConstant.BACKGROUND,
				SettingsConstant.DEFAULT_BACKGROUND);
		root.setBackgroundDrawable((getResources().getDrawable((Utils.getResId("background_" + backgroundResName,
				R.drawable.class)))));

	}

	@Override
	protected void onResume() {
		super.onStart();
		if (Game.getState() == GameState.STARTED) {
			Long timeWhenStopped = Game.getSettings().getLong("time", 0);
			Game.getStatsManager().onGameResume(timeWhenStopped);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Gson gson = new Gson();
		String json = gson.toJson(Game.getMoves());
		Editor editor = Game.getSettings().edit();
		if (Game.getState() == GameState.FINISHED) {
			editor.putString("moves", null);
		} else {
			editor.putString("moves", json);
			editor.putLong("time", Game.getStatsManager().getTimeWhenStopped());
		}

		editor.commit();

	}

	private void gameWon() {
		winner.setVisibility(View.VISIBLE);
		Game.setState(GameState.FINISHED);
		Game.getStatsManager().timeStop();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Game.getStatsManager().onGamePause();
	}

	private void dealNewGame() {
		Game.setState(GameState.DEALING);
		for (int k = 0; k < Game.START_CARDS_DEAL_COUNT; k++) {
			BasePile pile = tableauPiles.get(k % 10);

			Card card = stock.getCard();

			boolean faceUp = Game.START_CARDS_DEAL_COUNT - k <= tableauPiles.size();
			dealCard(pile, card, faceUp, k, true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_undo) {
			undo(true);
			return true;
		} else if (itemId == R.id.action_new_game) {
			if (Game.getState() == GameState.STARTED) {
				showNewGameDialog();
			} else if (Game.getState() == GameState.DEALING) {
				// do nothing
			} else {
				setupNewGame();
			}
			return true;
		} else if (itemId == R.id.action_restart) {
			if (Game.getState() == GameState.STARTED) {
				restartGameDialog();
			} else if (Game.getState() == GameState.DEALING) {
				// do nothing
			}
			return true;
		} else if (itemId == R.id.action_settings) {
			Intent settingsActivity = new Intent(getBaseContext(), SettingsDialog.class);
			startActivity(settingsActivity);
			return true;
		} else if (itemId == R.id.action_rules) {
			showRulesDialog();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void showRulesDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(BaseGameActivity.this);
		builder.setMessage(R.string.rules_of_spider);
		builder.setTitle(R.string.spider_solitaire_rules);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	private void restartGame() {
		for (int i = 0; i < Game.getMoves().size();) {
			undo(false);
		}
		Game.getMoves().clear();
		Game.getStatsManager().clearStatsAndGo();
		getActionBar().hide();
	}

	private void restoreGame() {
		onCreate = false;

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Card.class, new CardDeserializer(Game.getAppContext()));
		Gson gson = gsonBuilder.create();

		String draw = Game.getSettings().getString("draw", null);

		Type type = new TypeToken<ArrayList<Card>>() {
		}.getType();
		List<Card> cards = gson.fromJson(draw, type);
		stock.setCards(cards);

		for (int k = 0; k < Game.START_CARDS_DEAL_COUNT; k++) {
			BasePile pile = tableauPiles.get(k % 10);

			Card card = stock.getCard();
			Game.addCardDealt();
			boolean faceUp = Game.START_CARDS_DEAL_COUNT - k <= tableauPiles.size();
			card.setFaceup(faceUp);
			pile.addCard(card);

		}

		for (int i = 0; i < Game.getMoves().size(); i++) {
			redo(i);
		}
	}

	private void setActionBarVisibility() {
		if (getActionBar().isShowing()) {
			getActionBar().hide();
		} else {
			getActionBar().show();
		}
	}

	private void setupNewGame() {
		Game.setState(GameState.DEALING);
		Game.getMoves().clear();

		for (BasePile pileLayout : tableauPiles) {
			pileLayout.removeViews(1, pileLayout.getCardsCount());
		}
		stock.initialize(chosenDifficulty, BaseGameActivity.this);
		dealNewGame();
		Game.getStatsManager().setTimeZero();
	}

	private void undo(boolean human) {
		if (Game.getMoves().isEmpty()) {
			return;
		}

		winner.setVisibility(View.GONE);
		int indexOfFirstDragged;

		Move move = Game.getMoves().pop();

		if (move.getAction() == Move.ACTION_DEAL && human && !chosenUnrestrictedUndo) {
			Game.getMoves().add(move);
			return;
		}

		BasePile landingContainer = tableauPiles.get(move.getFrom());
		BasePile draggedParent = tableauPiles.get(move.getTo());

		if (move.isCompletedUncovered()) {
			draggedParent.getLastCard().setFaceup(false);
		}

		if (move.isCompleted()) {
			Game.setUndid();
			Game.getStatsManager().updatePoints(StatsManager.SET_UNDID);

			for (Rank num : Rank.values()) {
				Card card = new Card(BaseGameActivity.this, move.getSuit(), num);
				card.setFaceup(true);
				draggedParent.addCard(card);
			}
		}

		if (move.isUncover()) {
			indexOfFirstDragged = landingContainer.getCardsCount() - move.getAmount();

			landingContainer.getLastCard().setFaceup(false);
		}

		switch (move.getAction()) {

		case Move.ACTION_MOVE:
			indexOfFirstDragged = draggedParent.getCardsCount() - move.getAmount();
			for (int i = indexOfFirstDragged; i < draggedParent.getCardsCount();) {
				draggedParent.moveCard(landingContainer, draggedParent.getCardAt(i));
				draggedParent.checkState(false);
				landingContainer.checkState(false);

			}

			break;

		case Move.ACTION_DEAL:

			for (int i = tableauPiles.size() - 1; i >= 0; i--) {
				Card card = tableauPiles.get(i).getLastCard();
				card.setFaceup(false);
				stock.addCard(card);
				tableauPiles.get(i).removeLastCard();
			}
			break;
		}

		Game.getStatsManager().updateMoves(StatsManager.MOVE);

	}

	private void redo(int index) {
		Move move = Game.getMoves().get(index);

		BasePile draggedParent = tableauPiles.get(move.getFrom());
		BasePile landingContainer = tableauPiles.get(move.getTo());

		int indexOfFirstDragged;

		Game.getStatsManager().updateMoves(StatsManager.MOVE);

		switch (move.getAction()) {

		case Move.ACTION_MOVE:
			indexOfFirstDragged = draggedParent.getCardsCount() - move.getAmount();
			for (int i = indexOfFirstDragged; i < draggedParent.getCardsCount();) {
				draggedParent.moveCard(landingContainer, draggedParent.getCardAt(i));
			}
			draggedParent.uncoverLastCard();
			break;

		case Move.ACTION_DEAL:

			for (int i = 0; i < tableauPiles.size(); i++) {
				Card card = stock.getCard();
				tableauPiles.get(i).addCard(card);
				card.setFaceup(true);
				Game.addCardDealt();
			}
			break;
		}
	}

	private void setCardSize(Card card) {
		card.getLayoutParams().width = cardWidth;
		card.getLayoutParams().height = cardHeight;
	}


	private void showNewGameDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(BaseGameActivity.this);
		builder.setMessage(R.string.current_progress_will_be_lost_message);
		builder.setTitle(R.string.start_new_game_alert_dialog_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				setupNewGame(); 	
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void restartGameDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(BaseGameActivity.this);
		builder.setMessage(R.string.current_progress_will_be_lost_message);
		builder.setTitle(R.string.restart_game_alert_dialog_title);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				restartGame();
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}


	private void fixLayoutOrientation(int orientation) {

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			stock.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.BELOW, 0);
			scrollPiles.setLayoutParams(params);

			hollowPile.setVisibility(View.INVISIBLE);
			piles.setWeightSum(11f);

			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.LEFT_OF, stock.getId());
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			statsLayout.setLayoutParams(params);

		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
			stock.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.BELOW, stock.getId());
			scrollPiles.setLayoutParams(params);

			hollowPile.setVisibility(View.GONE);
			piles.setWeightSum(10f);

			params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			params.addRule(RelativeLayout.LEFT_OF, 0);
			statsLayout.setLayoutParams(params);

		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		initSettings();

		if (key.equals(SettingsConstant.DIFFICULTY)) {
			if (sharedPreferences.getBoolean("firstTime", false)) {
				if (Game.getState() != GameState.DEALING) {
					setupNewGame();
				}
			} else {
				Editor e = sharedPreferences.edit();
				e.putBoolean("firstTime", true);
				e.commit();
			}
		} else if (key.equals(SettingsConstant.BACKGROUND)) {
			String backgroundResName = Game.getSettings().getString(SettingsConstant.BACKGROUND,
					SettingsConstant.DEFAULT_BACKGROUND);
			Drawable background = getResources().getDrawable(
					(Utils.getResId("background_" + backgroundResName, R.drawable.class)));
			root.setBackgroundDrawable(background);

		} else if (key.equals(SettingsConstant.HINTS) || key.equals(SettingsConstant.CARD_SET)
				|| key.equals(SettingsConstant.REVERSE)) {

			for (BasePile pile : tableauPiles) {
				pile.refresh();
			}
		}
	}

	private void getCardDimensions(final View scaled) {
		ViewTreeObserver vto = scaled.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				cardWidth = scaled.getMeasuredWidth();
				cardHeight = scaled.getMeasuredHeight();
				stock.setSize(cardWidth, cardHeight);
				if (onCreate && Game.getState() == GameState.NOT_STARTED) {
					setupNewGame();
				}
			}
		});
	}

}
