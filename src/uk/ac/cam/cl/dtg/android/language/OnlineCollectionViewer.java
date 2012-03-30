package uk.ac.cam.cl.dtg.android.language;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

/**
 * 
 * {@link Activity} that shows all the information about the online collections.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class OnlineCollectionViewer extends Activity implements Runnable
{
	public static final String INTENT_GLOBALID = "globalID";
	public static final String INTENT_TITLE = "title";
	public static final String INTENT_DESCRIPTION = "description";
	public static final String INTENT_AUTHOR_NAME = "authorName";
	public static final String INTENT_RATING = "rating";
	public static final String INTENT_DOWNLOAD_COUNT = "downloads";
	public static final String INTENT_UPLOAD_TIME = "uploadTime";
	public static final String INTENT_DOWNLOAD_SIZE = "downloadSize";
	public static final String INTENT_RATING_COUNT = "ratingCount";

	private static final String LOG_TAG = "OnlineCollectionViewer";

	public static final String INTENT_COLLECTION_ID = "collectionID";

	private static final int MESSAGE_WHAT_TYPE_CHANGED = 0;
	private static final String MESSAGE_BUNDLE_NEW_TYPE = "newType";
	OnlineCollection mCollection;

	private RatingBar mRatingBar;
	private LinearLayout mRatingLayout, mButtonLayout;

	private Handler mHandler = new Handler()
	{
		@Override
    public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MESSAGE_WHAT_TYPE_CHANGED:
				MyLog.d(LOG_TAG, "Collection type changed");

				Bundle bundle = msg.getData();

				refreshDownloadStatus(bundle.getInt(MESSAGE_BUNDLE_NEW_TYPE));

				break;
			}
		}
	};

	private Thread mThread;
	private int mType;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.onlinecollectionview);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		Intent intent = this.getIntent();

		mCollection = new OnlineCollection(intent.getLongExtra(INTENT_GLOBALID, 0), intent.getStringExtra(INTENT_TITLE), intent.getStringExtra(INTENT_DESCRIPTION), intent.getStringExtra(INTENT_AUTHOR_NAME), intent.getIntExtra(
				INTENT_RATING, 0), intent.getIntExtra(INTENT_DOWNLOAD_COUNT, 0), intent.getFloatExtra(
				INTENT_DOWNLOAD_SIZE, 0f), intent.getLongExtra(INTENT_UPLOAD_TIME, 0), intent.getIntExtra(
				INTENT_RATING_COUNT, 0));

		if (mCollection != null)
		{
			fillData();
		}
	}

	/**
	 * Method to fill in all of the views with appropriate data.
	 * 
	 */
	private void fillData()
	{
		if (mCollection != null)
		{
			// inflate all the views and fill them with data
			TextView titleView = (TextView) findViewById(R.id.title);
			TextView descriptionView = (TextView) findViewById(R.id.collection_description);
			TextView uploadedDateView = (TextView) findViewById(R.id.date_uploaded);
			TextView authorNameView = (TextView) findViewById(R.id.author_name);
			TextView downloadCountView = (TextView) findViewById(R.id.download_count);
			mRatingLayout = (LinearLayout) findViewById(R.id.ratingHolder);
			mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
			mButtonLayout = (LinearLayout) findViewById(R.id.download_button_place);

			// set the title text
			titleView.setText(mCollection.getTitle());

			// set the collection description
			descriptionView.setText(mCollection.getDescription());

			// set the author name
			authorNameView.setText(mCollection.getAuthorName());

			downloadCountView.setText(mCollection.getDownloadCount() + " "
					+ getString(R.string.downloads));

			Date dateUploaded = new Date(mCollection.getDateUploaded() * 1000);

			String formattedTime = (String) DateFormat.getDateFormat(this).format(dateUploaded);

			uploadedDateView.setText(formattedTime);

			ApplicationDBAdapter db = new ApplicationDBAdapter(this);
			db.open();
			try {
			  Collection c = db.getCollectionByGlobalID(mCollection.getGlobalID());
			  mType = c.getType();
			} catch (IOException e) {
			  mType = -1;
			} finally {
        db.close();
      }

			refreshDownloadStatus(mType);

			// start the thread which checks the download status of the
			// application and updates the views accordingly
			mThread = new Thread(this);
			mThread.start();
		} else
			MyLog.e(LOG_TAG, "Collection is empty - no point in inflating views");

	}

	/**
	 * Method that refreshes all the views and buttons when collection type has
	 * been changed.
	 * 
	 * @param collectionType new collection type
	 */
	private void refreshDownloadStatus(int collectionType)
	{
		if (collectionType == -1 || collectionType == Collection.TYPE_PRIVATE_SHARED_COLLECTION)
		{
			// collection is not installed on the phone or if it is your
			// collection

			// show the online rating
			mRatingBar.setRating((float) mCollection.getRating());

			// rating layout shall not be clickable
			mRatingLayout.setClickable(false);

			// show rating count next to the stars
			TextView ratingView = (TextView) findViewById(R.id.rating_count);
			ratingView.setText(mCollection.getRatingCount() + " " + getString(R.string.ratings));

			// set the button listener to start downloading

			if (collectionType == -1)
			{
				mButtonLayout.removeAllViews();

				final Button button = new Button(this);
				button.setText(R.string.download);
				button.setOnClickListener(new View.OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						button.setClickable(false);
						button.setText(R.string.downloading);

						ApplicationDBAdapter db = new ApplicationDBAdapter(OnlineCollectionViewer.this);
						db.open();
						long localID;
						try {
						  localID = db.insertCollection(mCollection.getTitle(),
						      Collection.TYPE_CURRENTLY_DOWNLOADING, -1,
						      mCollection.getDescription(), mCollection.getGlobalID());
						} finally {
						  db.close();
						}

						Intent intent = new Intent(OnlineCollectionViewer.this, CollectionDownloadService.class);

						intent.putExtra(CollectionDownloadService.INTENT_COLLECTION_GLOBAL_ID,
								mCollection.getGlobalID());
						intent.putExtra(CollectionDownloadService.INTENT_COLLECTION_LOCAL_ID,
								localID);

						OnlineCollectionViewer.this.startService(intent);

					}
				});

				mButtonLayout.addView(button,
						new ViewGroup.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
			} else
			{
				// this is your own collection - no need to show the button
				mButtonLayout.removeAllViews();
			}
		} else if (collectionType == Collection.TYPE_DOWNLOADED_UNLOCKED)
		{
			// collection is already downloaded and installed on the phone

			// get local collection
			ApplicationDBAdapter db = new ApplicationDBAdapter(this);
			db.open();
			
			Collection c = null;
			try {
			  try {
			    c = db.getCollectionByGlobalID(mCollection.getGlobalID());
			  } finally {
			    db.close();
			  }

			  // set the rating bar to show your rating
			  mRatingBar.setRating((float) c.getRating());
			} catch (IOException e){
			  MyLog.e(LOG_TAG, e.getMessage());
			}

			TextView ratingView = (TextView) findViewById(R.id.rating_count);

			// if the collection has not been rated - show the encouragement to
			// do that!
			if (c == null || c.getRating() == -1)
			{
				ratingView.setText(R.string.rate_it);
			} else
			{
				ratingView.setText(R.string.your_rating);
			}

			mRatingLayout.setClickable(true);
			// set the listener to show the rating dialog
			mRatingLayout.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showRatingDialog();
				}
			});

			mRatingLayout.setLongClickable(true);
			mRatingLayout.setOnLongClickListener(new View.OnLongClickListener()
			{

				@Override
				public boolean onLongClick(View v)
				{
					showRatingDialog();
					return true;
				}
			});

			mButtonLayout.removeAllViews();

			// set the button to check for updates
			Button button = new Button(this);

			button.setText(R.string.downloaded);
			button.setClickable(false);

			mButtonLayout.addView(button,
					new ViewGroup.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));
		} else if (collectionType == Collection.TYPE_CURRENTLY_DOWNLOADING)
		{
			mButtonLayout.removeAllViews();

			Button button = new Button(this);
			button.setClickable(false);
			button.setText(R.string.downloading);

			mButtonLayout.addView(button,
					new ViewGroup.LayoutParams(150, ViewGroup.LayoutParams.WRAP_CONTENT));

			// show the same rating as if the collection has not been downloaded
			mRatingBar.setRating((float) mCollection.getRating());
			mRatingLayout.setClickable(false);

			// show rating count next to the stars
			TextView ratingView = (TextView) findViewById(R.id.rating_count);
			ratingView.setText(mCollection.getRatingCount() + " " + getString(R.string.ratings));
		}

	}

	/**
	 * Method that shows the rating dialog.
	 * 
	 */
	private void showRatingDialog()
	{
		MyLog.d(LOG_TAG, "showRatingDialog() called");

		// set up the dialog
		AlertDialog.Builder ratingDialogBuilder = new AlertDialog.Builder(this);
		ratingDialogBuilder.setTitle(getString(R.string.rate_collection));

		RatingBar bigRatingBar = new RatingBar(this);
		bigRatingBar.setNumStars(5);
		bigRatingBar.setRating(mRatingBar.getRating());
		bigRatingBar.setStepSize(1);

		bigRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener()
		{

			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser)
			{
				// update local DB and send the rating update to the web server
				ApplicationDBAdapter db = new ApplicationDBAdapter(OnlineCollectionViewer.this);
				db.open();
				try {
				  Collection c = db.getCollectionByGlobalID(mCollection.getGlobalID());

				  db.updateRating(c.getRowID(), (int) rating);
				  db.close();// Close it early if we can

				  if (c.getType() == Collection.TYPE_DOWNLOADED_UNLOCKED)
				  {
				    Intent intent = new Intent(OnlineCollectionViewer.this, CollectionRatingService.class);

				    intent.putExtra(CollectionRatingService.INTENT_GLOBAL_ID,
				        mCollection.getGlobalID());
				    intent.putExtra(CollectionRatingService.INTENT_RATING, (int) rating);

				    OnlineCollectionViewer.this.startService(intent);
				  }

				} catch (IOException e){
				  MyLog.e(LOG_TAG, e.getMessage());
				} finally {
				  db.close();
				}

				// redraw the rating bar
				mRatingBar.setRating(rating);
			}
		});

		ratingDialogBuilder.setView(bigRatingBar);

		ratingDialogBuilder.show();

	}

	@Override
	protected void onDestroy()
	{
		mThread.interrupt();

		mThread = null;

		super.onDestroy();
	}

	@Override
	public void run()
	{
		Collection localCollection;
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		int oldType = mType;
		Message msg;
		Bundle bundle;

		while (true)
		{
		  int newType;
			db.open();
      try {
        localCollection = db.getCollectionByGlobalID(mCollection.getGlobalID());
        newType = localCollection.getType();
      } catch (IOException e) {
        MyLog.e(LOG_TAG, e.getMessage());
        newType = -1;
      } finally {
        db.close();
      }

			// send the message to the handler if the type has changed
			if (newType != oldType)
			{
				oldType = newType;
				msg = new Message();
				msg.what = MESSAGE_WHAT_TYPE_CHANGED;

				bundle = new Bundle();
				bundle.putInt(MESSAGE_BUNDLE_NEW_TYPE, oldType);

				msg.setData(bundle);
				mHandler.sendMessage(msg);
			}

			try
			{
				Thread.sleep(1000);
			} catch (Exception e)
			{
				break;
			}
		}

	}
}
