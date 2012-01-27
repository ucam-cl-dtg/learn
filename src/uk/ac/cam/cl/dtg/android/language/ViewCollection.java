package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * Activity for reviewing local collection information (for online collections,
 * see {@link OnlineCollectionViewer})
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ViewCollection extends Activity
{
	private static final String LOG_TAG = "ViewCollection";

	public static final String INTENT_COLLECTION_ID = "collectionID";

	Collection mCollection;
	ApplicationDBAdapter mDBAdapter;
	TextView mCollectionTypeField, mPrivacyDescriptionField;
	private RatingBar mRatingBar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())
		{
			setContentView(R.layout.collectionview);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			Intent intent = this.getIntent();
			long collectionID = intent.getLongExtra(INTENT_COLLECTION_ID, 0);

			mDBAdapter = new ApplicationDBAdapter(this);
			mDBAdapter.open();
			try {
			  mCollection = mDBAdapter.getCollectionById(collectionID);
			} finally {
			  mDBAdapter.close();
			}

			if (mCollection != null)
			{
				fillData();
			}
		}
	}

	/**
	 * 
	 * Fills all the views with the required data.
	 * 
	 */
	private void fillData()
	{
		if (mCollection != null)
		{
			// inflate all the views and fill them with data
			TextView titleView = (TextView) findViewById(R.id.title);
			TextView descriptionView = (TextView) findViewById(R.id.collection_description);
			TextView lastSeenDateView = (TextView) findViewById(R.id.last_time_seen);
			TextView cardCountView = (TextView) findViewById(R.id.card_count);
			LinearLayout ratingLayout = (LinearLayout) findViewById(R.id.ratingHolder);
			Button clearStatisticsButton = (Button) findViewById(R.id.clear_statistics);

			mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

			// set the title text
			titleView.setText(mCollection.getTitle());

			// set the collection description
			descriptionView.setText(mCollection.getDescription());

			// set the rating
			mRatingBar.setRating((float) mCollection.getRating());

			// set up the listeners for the rating holder
			ratingLayout.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					showRatingDialog();
				}
			});

			ratingLayout.setOnLongClickListener(new View.OnLongClickListener()
			{

				@Override
				public boolean onLongClick(View v)
				{
					showRatingDialog();
					return true;
				}
			});

			if (mCollection.getLastLearned() > 0)
			{
				Date lastSeen = new Date(mCollection.getLastLearned());

				String formattedTime = (String) DateFormat.getDateFormat(this).format(lastSeen)
						+ " " + (String) DateFormat.getTimeFormat(this).format(lastSeen);

				lastSeenDateView.setText(formattedTime);
			} else
			{
				lastSeenDateView.setText(R.string.never);
			}

			// get the statistics filled
			StatisticsHelper statsHelper = new StatisticsHelper(this, mCollection.getRowID());

			statsHelper.loadStatistics(ApplicationDBAdapter.KEY_STATISTICS_TIME + " DESC");

			ArrayList<StatisticsItem> stats = statsHelper.getStatistics();

			Collections.reverse(stats);

			long totalCount = 0, correctCount = 0;

			for (StatisticsItem s : stats)
			{
				if (s.wasInTestingMode())
				{
					totalCount++;
					if (s.isCorrect())
						correctCount++;
				}
			}

			MyLog.d(LOG_TAG, "total count is " + totalCount + ", correct count is " + correctCount);

			float score;

			if (totalCount != 0)
				score = (float) correctCount / (float) totalCount;
			else
				score = 0;

			final ProgressBar scoreBar = (ProgressBar) findViewById(R.id.score_bar);
			scoreBar.setMax(100);
			scoreBar.setProgress(Math.round(score * 100));

			// get the card count in the collection
			CardDBAdapter db = new CardDBAdapter();
			db.open(mCollection.getRowID());
			int cardCount = db.getCardCount();
			db.close();

			cardCountView.setText(String.valueOf(cardCount));

			clearStatisticsButton.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					ApplicationDBAdapter db = new ApplicationDBAdapter(ViewCollection.this);
					db.open();
					try {
					  db.clearStatisticsForCollection(mCollection.getRowID());
					} finally {
					  db.close();
					}

					scoreBar.setProgress(0);

					Toast.makeText(ViewCollection.this, R.string.statistics_cleared,
							Toast.LENGTH_SHORT).show();
				}
			});

			mCollectionTypeField = (TextView) findViewById(R.id.collection_type);
			mPrivacyDescriptionField = (TextView) findViewById(R.id.collection_privacy_desc);

			switch (mCollection.getType())
			{
			case Collection.TYPE_PRIVATE_SHARED_COLLECTION:
			case Collection.TYPE_PRIVATE_NON_SHARED:
			case Collection.TYPE_CURRENTLY_UPLOADING:

				mCollectionTypeField.setText(R.string.collection_private);
				mPrivacyDescriptionField.setText(R.string.collection_private_desc);

				break;
			case Collection.TYPE_DOWNLOADED_LOCKED:
			case Collection.TYPE_DOWNLOADED_UNLOCKED:
			case Collection.TYPE_CURRENTLY_DOWNLOADING:

				mCollectionTypeField.setText(R.string.collection_downloaded);
				mPrivacyDescriptionField.setText(R.string.collection_downloaded_desc);

				break;
			}

		} else
			MyLog.e(LOG_TAG, "Collection is empty - no point in inflating views");

	}

	/**
	 * 
	 * Method to open up rating dialog.
	 * 
	 */
	private void showRatingDialog()
	{
		MyLog.d(LOG_TAG, "showRatingDialog() called");

		// set up the dialog
		AlertDialog.Builder ratingDialogBuilder = new AlertDialog.Builder(ViewCollection.this);
		ratingDialogBuilder.setTitle(getString(R.string.rate_collection));

		RatingBar bigRatingBar = new RatingBar(this);
		bigRatingBar.setNumStars(5);
		bigRatingBar.setRating((float) mCollection.getRating());
		bigRatingBar.setStepSize(1);

		bigRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener()
		{

			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
			{
				// update the database
				ApplicationDBAdapter db = new ApplicationDBAdapter(ViewCollection.this);
				db.open();
				try {
				  db.updateRating(mCollection.getRowID(), (int) rating);
				} finally {
				  db.close();
				}

				if (mCollection.getType() == Collection.TYPE_DOWNLOADED_UNLOCKED)
				{
					Intent intent = new Intent(ViewCollection.this, CollectionRatingService.class);

					intent.putExtra(CollectionRatingService.INTENT_GLOBAL_ID,
							mCollection.getGlobalID());
					intent.putExtra(CollectionRatingService.INTENT_RATING, (int) rating);

					ViewCollection.this.startService(intent);
				}

				// set the rating in the object
				mCollection.setRating((int) rating);

				// redraw the rating bar
				mRatingBar.setRating(rating);

			}
		});

		ratingDialogBuilder.setView(bigRatingBar);

		ratingDialogBuilder.show();

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.clear();
		
		if (mCollection.getType() == Collection.TYPE_DOWNLOADED_UNLOCKED || mCollection.getType() == Collection.TYPE_CURRENTLY_DOWNLOADING)
		{
			// check whether a user has an account
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			boolean hasAccount = preferences.getBoolean(
					this.getString(R.string.preferences_code_account_registered), false);
			
			if (hasAccount)
			{
				// show him the regain rights button
				MenuItem rRightsButton = menu.add(R.string.regain_creator_rights);
				rRightsButton.setOnMenuItemClickListener(new OnMenuItemClickListener()
				{
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						final int RIGHTS_OK = 1, NO_RIGHTS = 0, ERROR = 2;
						
						// show the progress dialog
						final ProgressDialog pDialog = new ProgressDialog(ViewCollection.this);
						pDialog.setTitle(R.string.regaining_author_rights);
						pDialog.setMessage(getString(R.string.communicating_with_server));
						pDialog.setCancelable(false);
						pDialog.show();
						
						final Handler handler = new Handler(){

							@Override
							public void handleMessage(Message msg)
							{
								pDialog.dismiss();
								
								switch (msg.what)
								{
								case RIGHTS_OK:
									Toast.makeText(ViewCollection.this, getString(R.string.rights_regained), Toast.LENGTH_LONG).show();
									
									ApplicationDBAdapter db = new ApplicationDBAdapter(ViewCollection.this);
									db.open();
                    try {
                      db.updateCollectionType(mCollection.getRowID(),
                          Collection.TYPE_PRIVATE_SHARED_COLLECTION);
                    } finally {
                      db.close();
                    }
									
									if (mCollectionTypeField != null)
										mCollectionTypeField.setText(R.string.collection_private);
									if (mPrivacyDescriptionField != null)
										mPrivacyDescriptionField.setText(R.string.collection_private_desc);
									
									break;
								case NO_RIGHTS:
									Toast.makeText(ViewCollection.this, getString(R.string.rights_not_regained), Toast.LENGTH_LONG).show();
									break;
								case ERROR:
									Toast.makeText(ViewCollection.this, getString(R.string.connection_problem), Toast.LENGTH_LONG).show();
									break;
								}
								
								super.handleMessage(msg);
							}};
						
						Runnable r = new Runnable(){

							@Override
							public void run()
							{
								try
								{
									if (ServerHelper.regainRights(ViewCollection.this, mCollection.getGlobalID(), 0))
									{
										// access rights should be changed
										handler.sendEmptyMessage(RIGHTS_OK);										
									}
									else
									{
										handler.sendEmptyMessage(NO_RIGHTS);
									}
								}
								catch (Exception e)
								{
									handler.sendEmptyMessage(ERROR);									
								}
								
								
							}};
						
						Thread t = new Thread(r);
						t.start();									
						return true;
					}
				});
			}
			
			
		}
		return super.onPrepareOptionsMenu(menu);
	}

}
