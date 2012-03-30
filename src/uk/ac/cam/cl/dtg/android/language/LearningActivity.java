package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * {@link Activity} responsible for showing cards for learning.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class LearningActivity extends Activity implements AnswerListener, OnGestureListener
{
	private static final String LOG_TAG = "LearningActivity";

	private static final String PREFERENCES_INTRODUCTION_SHOWN = "introShown";

	private CardRenderer mCardRenderer;
	private LearningAlgorithm mLearningAlgorithm;
	private StatisticsHelper mStatisticsHelper;

	private static final int ACTIVITY_CHOOSE_COLLECTION = 0;

	private static final String BUNDLE_CARD_ID = "cardID";
	private static final String BUNDLE_COLLECTION_ID = "collectionID";
	private static final String BUNDLE_LEARNING_ALGORITHM_STATE = "learningAlgorithmState";
	private static final String BUNDLE_TIME_CARD_SHOWN = "timeShown";
	private static final String BUNDLE_TESTING = "testing";
	private static final String BUNDLE_SHOWING_CARDS = "showingCards";

	// code for the current collection and card
	private long mCollectionID;
	private long mCardID;

	private GestureDetector mGestureDetector;

	// list of cards to be populated
	private ArrayList<Card> mCards;

	private boolean mShowingCards = false;

	private long timeCardShown;

	private boolean mTesting;

	private Card mCard;

	private long mTimeFlingedLast = 0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())

		{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			// essential for video rendering - done here so that the screen does
			// not flicker during rendering
			getWindow().setFormat(PixelFormat.TRANSLUCENT);

			// initialize gesture detector
			mGestureDetector = new GestureDetector(this);

			if (savedInstanceState != null)
			{
				mShowingCards = savedInstanceState.getBoolean(BUNDLE_SHOWING_CARDS, false);

				// check if the app was showing cards before this happened
				// if it wasn't, then it was probably inactive and is likely to
				// receive onActivityResult() call right now
				if (mShowingCards)
				{

					mCardID = savedInstanceState.getLong(BUNDLE_CARD_ID);
					long collectionID = savedInstanceState.getLong(BUNDLE_COLLECTION_ID);
					timeCardShown = savedInstanceState.getLong(BUNDLE_TIME_CARD_SHOWN);
					mTesting = savedInstanceState.getBoolean(BUNDLE_TESTING);
					initializeCollection(collectionID);

					// initialize card renderer
					mCardRenderer = new CardRenderer(this, mCollectionID);

					Bundle learningAlgorithmState = savedInstanceState.getBundle(BUNDLE_LEARNING_ALGORITHM_STATE);

					// initialize learning algorithm
					initializeLearningAlgorithm(learningAlgorithmState);

					showParticularCard(mCardID);
				}

			} else
			{
				// create intent for card collection viewer
				startCollectionBrowser();
			}
		}

	}

	@Override
	public void answerCorrect()
	{

		buildToast(true);

		// update statistics
		mStatisticsHelper.insertStatistics(mCardID, System.currentTimeMillis() - timeCardShown,
				true, mTesting);

		// show the next card
		showNext();
	}

	/**
	 * 
	 * Builds a toast showing whether you were correct or not.
	 * 
	 * @param correct
	 *            true if the answer was correct, false if not.
	 */
	private void buildToast(boolean correct)
	{
		Toast myToast = new Toast(this);

		LinearLayout holder = new LinearLayout(this);
		holder.setGravity(Gravity.CENTER);
		holder.setOrientation(LinearLayout.VERTICAL);
		holder.setBackgroundResource(android.R.drawable.toast_frame);

		ImageView icon = new ImageView(this);
		if (correct)
			icon.setImageResource(R.drawable.correct);
		else
			icon.setImageResource(R.drawable.wrong);
		icon.setPadding(10, 10, 10, 10);

		TextView tv = new TextView(this);
		tv.setTextSize(18);
		tv.setPadding(10, 10, 10, 10);
		tv.setTextColor(Color.WHITE);
		if (correct)
			tv.setText(R.string.answer_correct);
		else
			tv.setText(R.string.answer_incorrect);

		holder.addView(icon);
		holder.addView(tv);

		myToast.setView(holder);
		myToast.setDuration(Toast.LENGTH_SHORT);

		myToast.show();

	}

	@Override
	public void answerIncorrect()
	{
		buildToast(false);

		// alter the statistics for this card
		mStatisticsHelper.insertStatistics(mCardID, System.currentTimeMillis() - timeCardShown,
				false, mTesting);

		// show the next card
		showNext();
	}

	/**
	 * 
	 * Initializes appropriate learning algorithm as set in application
	 * settings.
	 * 
	 * @param state
	 *            learning algorithm state
	 */
	private void initializeLearningAlgorithm(Bundle state)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String algorithm = sharedPreferences.getString(
				getString(R.string.preferences_code_learning_algorithm),
				String.valueOf(LearningAlgorithm.NEGATIVE_LEITNER_ALGORITHM));

		int algorithmID = Integer.valueOf(algorithm);
		switch (algorithmID)
		{
		case LearningAlgorithm.SIMPLE_ALGORITHM:
			MyLog.d(LOG_TAG, "Initializing simple algorithm");

			if (state == null)
				mLearningAlgorithm = new SimpleAlgorithm(mCards);
			else
				mLearningAlgorithm = new SimpleAlgorithm(mCards, state);
			break;
		case LearningAlgorithm.ITERATING_ALGORITHM:
			MyLog.d(LOG_TAG, "Initializing iterating algorithm");

			if (state == null)
				mLearningAlgorithm = new IteratingAlgorithm(mCards);
			else
				mLearningAlgorithm = new IteratingAlgorithm(mCards, state);
			break;
		case LearningAlgorithm.LEITNER_ALGORITHM:

			MyLog.d(LOG_TAG, "Initializing Leitner algorithm");

			if (state == null)
				mLearningAlgorithm = new LeitnerAlgorithm(mCards, mStatisticsHelper.getStatistics());
			else
				mLearningAlgorithm = new LeitnerAlgorithm(mCards, mStatisticsHelper.getStatistics(), state);

			break;
		case LearningAlgorithm.NEGATIVE_LEITNER_ALGORITHM:

			MyLog.d(LOG_TAG, "Initializing negative Leitner algorithm");

			if (state == null)
				mLearningAlgorithm = new NegativeLeitnerAlgorithm(mCards, mStatisticsHelper.getStatistics());
			else
				mLearningAlgorithm = new NegativeLeitnerAlgorithm(mCards, mStatisticsHelper.getStatistics(), state);

			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case ACTIVITY_CHOOSE_COLLECTION:
				// check whether the introductory dialog has been shown - if it
				// hasn't, show it
				showIntroductoryDialog();

				long collectionID = data.getLongExtra(CollectionBrowser.INTENT_COLLECTION_ID, -1);

				if (collectionID != -1)
				{
					mCollectionID = collectionID;

					initializeCollection(collectionID);
					mCardRenderer = new CardRenderer(this, collectionID);

					initializeLearningAlgorithm(null);

					startShowing();
				} else
					this.finish();
				break;
			default:
				this.finish();
				break;
			}

		} else if (resultCode == RESULT_CANCELED)
		{
			this.finish();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 
	 * Initializes collection with given ID - loads cards and statistics for
	 * that particular collection.
	 * 
	 * @param collectionID
	 *            ID of the collection to load
	 */
	private void initializeCollection(long collectionID)
	{
		if (mCardRenderer != null)
			mCardRenderer.reinitialize();

		mCollectionID = collectionID;

		// get all the cards
		CardDBAdapter db = new CardDBAdapter();
		db.open(collectionID);
		try {
		  mCards = db.getAllCards();
		} finally {
		  db.close();
		}

		// update last learned in the application DB
		ApplicationDBAdapter appDB = new ApplicationDBAdapter(this);
		appDB.open();
		try {
		  appDB.updateLastLearned(mCollectionID);
		} finally {
		  appDB.close();
		}

		// initialize statistics helper and load statistics
		mStatisticsHelper = new StatisticsHelper(this, mCollectionID);
		mStatisticsHelper.loadStatistics(ApplicationDBAdapter.KEY_STATISTICS_TIME + " DESC");

		// invert the order of the statistics
		Collections.reverse(mStatisticsHelper.getStatistics());
	}

	/**
	 * 
	 * Method that starts showing cards to user.
	 */
	private void startShowing()
	{
		mShowingCards = true;

		showNext();
	}

	/**
	 * Method to show next card. Goes off and asks learning algorithm to provide
	 * with the next one, calls {@link #showCard(NextCard, boolean)} to render
	 * and show it.
	 * 
	 */
	private void showNext()
	{
		// stop the currently running one
		if (mCardRenderer != null)
		{
			mCardRenderer.stop();
		}

		// get XML for the next card
		NextCard nextOne = mLearningAlgorithm.getNext();

		showCard(nextOne, true);

		timeCardShown = System.currentTimeMillis() + CardRenderer.DURATION_TOTAL_LAG;
	}

	/**
	 * 
	 * Method to render and show card.
	 * 
	 * @param card
	 *            card to be shown
	 * @param showAnimation
	 *            true if slide in animation should be shown, false if not
	 */
	private void showCard(NextCard card, boolean showAnimation)
	{
		if (card != null)
		{
			mCard = card.card;

			String xmlDesc = card.card.getXmlDescription();

			mCardID = card.card.getId();
			mTesting = card.testing;

			if (xmlDesc != "")
			{
				mCardRenderer.renderCard(xmlDesc, mTesting, showAnimation);
			} else
				startCollectionBrowser();
		} else
		{
			MyLog.e(LOG_TAG,
					"The card given by the learning algorithm is null... - getting back to collection browser");
			this.showNoMoreCardsToast();
			mCardRenderer.slideOut();
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					startCollectionBrowser();
				}
			}, CardRenderer.DURATION_OUT_ANIMATION);
		}

	}

	@Override
	protected void onDestroy()
	{
		if (mCardRenderer != null)
			mCardRenderer.stop();
		super.onDestroy();
	}

	/**
	 * 
	 * Method that finds and shows specific card. Also uses
	 * {@link #showCard(NextCard, boolean)} for card rendering and showing.
	 * 
	 * @param cardID
	 *            ID of the card to be shown
	 */
	private void showParticularCard(long cardID)
	{
		Iterator<Card> cards = mCards.iterator();

		Card found = null, i = null;

		while (cards.hasNext())
		{
			i = cards.next();

			if (i.getId() == cardID)
				found = i;
		}

		if (found != null)
		{
			NextCard nextCard = new NextCard();

			nextCard.card = found;
			nextCard.testing = mTesting;

			showCard(nextCard, false);
		} else
		{
			MyLog.e(LOG_TAG,
					"Card with particular ID not found - just showing the next one that learning algorithm decides..");
			showNext();
		}
	}

	/**
	 * Shows the toast informing the user that learning algorithm decided that
	 * it is enough.
	 * 
	 */
	private void showNoMoreCardsToast()
	{
		Toast myToast = new Toast(this);

		LinearLayout holder = new LinearLayout(this);
		holder.setGravity(Gravity.CENTER);
		holder.setOrientation(LinearLayout.VERTICAL);
		holder.setBackgroundResource(android.R.drawable.toast_frame);

		ImageView icon = new ImageView(this);
		icon.setImageResource(R.drawable.finish);
		icon.setPadding(10, 10, 10, 10);

		TextView tv = new TextView(this);
		tv.setTextSize(16);
		tv.setTextColor(Color.WHITE);
		tv.setText(R.string.no_more_cards_toast);
		tv.setPadding(10, 10, 10, 10);
		holder.addView(icon);
		holder.addView(tv);
		holder.setPadding(10, 10, 10, 10);

		myToast.setView(holder);
		myToast.setDuration(Toast.LENGTH_LONG);

		myToast.show();

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.clear();

		if (mShowingCards)
		{
			MenuItem nextItem = menu.add(R.string.show_next);
			nextItem.setIcon(android.R.drawable.ic_media_next);

			nextItem.setOnMenuItemClickListener(new OnMenuItemClickListener()
			{

				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					moveToNext();
					return false;
				}
			});

			ApplicationDBAdapter db = new ApplicationDBAdapter(this);
			db.open();
			Collection c;
			try {
			  c = db.getCollectionById(mCollectionID);
			} finally {
			  db.close();
			}

			if (c.getType() == Collection.TYPE_DOWNLOADED_UNLOCKED)
			{
				MenuItem reportItem = menu.add(R.string.report_error);
				reportItem.setIcon(android.R.drawable.ic_menu_report_image);

				final long globalID = c.getGlobalID();

				reportItem.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{

					@Override
					public boolean onMenuItemClick(MenuItem item)
					{

						Intent intent = new Intent(LearningActivity.this, ErrorReporter.class);

						intent.putExtra(ErrorReporter.INTENT_CARD_ID, mCardID);
						intent.putExtra(ErrorReporter.INTENT_COLLECTION_GLOBAL_ID, globalID);
						intent.putExtra(ErrorReporter.INTENT_CARD_TITLE, mCard.getTitle());

						// no need to get the result
						startActivity(intent);

						return false;
					}
				});
			}
		}

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Method to go to next card. Insert statistics item as well (assuming that
	 * user was incorrect).
	 * 
	 */
	protected void moveToNext()
	{
		// alter the statistics for this card - assume the answer would have
		// been incorrect
				
		mStatisticsHelper.insertStatistics(mCardID, System.currentTimeMillis() - timeCardShown,
				false, mTesting);

		showNext();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putLong(BUNDLE_CARD_ID, mCardID);
		outState.putLong(BUNDLE_COLLECTION_ID, mCollectionID);
		outState.putLong(BUNDLE_TIME_CARD_SHOWN, timeCardShown);
		outState.putBoolean(BUNDLE_TESTING, mTesting);
		outState.putBoolean(BUNDLE_SHOWING_CARDS, mShowingCards);

		if (mLearningAlgorithm != null)
		{
			Bundle algorithmBundle = new Bundle();
			mLearningAlgorithm.saveState(algorithmBundle);
			outState.putBundle(BUNDLE_LEARNING_ALGORITHM_STATE, algorithmBundle);
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onDown(MotionEvent e)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		if ((velocityX < -5.0) && mShowingCards)
		{
			// check whether enough time passed after the last fling - threshold
			// is 1.2 sec
			if (mTimeFlingedLast + 1200 < System.currentTimeMillis())
			{
				mTimeFlingedLast = System.currentTimeMillis();
				moveToNext();
			}
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e)
	{
	// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e)
	{
	// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent me)
	{
		return mGestureDetector.onTouchEvent(me);
	}

	private void startCollectionBrowser()
	{
		mShowingCards = false;
		Intent intent = new Intent(this, CollectionBrowser.class);
		intent.putExtra(CollectionBrowser.INTENT_ACTION,
				CollectionBrowser.INTENT_ACTION_PICK_FOR_LEARNING);
		startActivityForResult(intent, ACTIVITY_CHOOSE_COLLECTION);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && mShowingCards)
		{
			// stop showing cards and show the collection browser
			mShowingCards = false;
			mCardRenderer.stop();
			mCardRenderer.slideOut();
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					startCollectionBrowser();
				}
			}, CardRenderer.DURATION_OUT_ANIMATION);
			return true;
		} else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * Method to show introductory dialog to user. It is only shown once - the
	 * first time user begins learning.
	 */
	private void showIntroductoryDialog()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean shown = prefs.getBoolean(PREFERENCES_INTRODUCTION_SHOWN, false);

		if (!shown)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.learning));
			builder.setMessage(getString(R.string.learning_algorithms_introduction));
			builder.setPositiveButton(R.string.ok, null);

			builder.show();

			Editor editor = prefs.edit();
			editor.putBoolean(PREFERENCES_INTRODUCTION_SHOWN, true);
			editor.commit();
		}

	}
}
