package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.ApplicationInitializer;
import uk.ac.cam.cl.dtg.android.language.AudioEditor;
import uk.ac.cam.cl.dtg.android.language.CardDBAdapter;
import uk.ac.cam.cl.dtg.android.language.CardRenderer;
import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.R;
import uk.ac.cam.cl.dtg.android.language.ResourceHelper;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

/**
 * 
 * Class that represents audio component on a flashcard.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class Audio extends Component
{
	private final static String LOG_TAG = "Audio";

	private Activity mContext;
	long mDelay;
	private MediaPlayer mMediaPlayer;
	private MediaListener mListener;
	private ImageView mIconView;
	private Handler mHandler;

	private boolean mPlayOnTesting;
	private boolean mTesting;

	private long mCollectionID;
	private long mResourceID;

	private Runnable mRunnable;

	public Audio(Activity c, long collectionID, long resourceID, long delay, boolean playOnTesting,
			boolean testing)
	{
		mContext = c;
		mDelay = delay;
		mCollectionID = collectionID;
		mResourceID = resourceID;
		mPlayOnTesting = playOnTesting;
		mTesting = testing;
	}

	@Override
	public void render()
	{

		if (mTesting == true && mPlayOnTesting == false)
		{
			// show the audio is not played in this mode text view
			LinearLayout mainHolder = new LinearLayout(mContext);
			mainHolder.setGravity(Gravity.CENTER);

			mIconView = new ImageView(mContext);
			mIconView.setScaleType(ScaleType.CENTER);
			mIconView.setImageResource(R.drawable.play_grey);

			mIconView.setOnClickListener(new View.OnClickListener()
			{
				private boolean shown = false;

				@Override
				public void onClick(View v)
				{
					if (!shown)
					{
						Toast.makeText(mContext, R.string.audio_not_played_in_testing_mode,
								Toast.LENGTH_SHORT).show();
						shown = true;
					}
				}
			});

			mainHolder.addView(
					mIconView,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			mView = mainHolder;
		} else
		{
			// initialize the play/pause icon
			LinearLayout mainHolder = new LinearLayout(mContext);
			mainHolder.setGravity(Gravity.CENTER);

			mIconView = new ImageView(mContext);
			mIconView.setScaleType(ScaleType.CENTER);

			mainHolder.addView(
					mIconView,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			// initialize handler
			mHandler = new Handler();

			// initialize runnable
			mRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					initializeAndPlay();
				}
			};

			mHandler.postDelayed(mRunnable, mDelay + CardRenderer.DURATION_TOTAL_LAG);
			mIconView.setImageResource(R.drawable.play);

			mListener = new MediaListener();
			mIconView.setOnTouchListener(mListener);

			mView = mainHolder;
		}
	}

	/**
	 * 
	 * Method to initialize and play audio.
	 * 
	 */
	private void initializeAndPlay()
	{
		try
		{

			mMediaPlayer = new MediaPlayer();

			CardDBAdapter db = new CardDBAdapter();
			db.open(mCollectionID);
			String suffix = db.getResource(mResourceID).getSuffix();
			db.close();

			mMediaPlayer.setDataSource(ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID
					+ "/" + mResourceID + "." + suffix);
			
			mMediaPlayer.prepare();

			mMediaPlayer.setOnCompletionListener(mListener);

			mMediaPlayer.start();
			mIconView.setImageResource(R.drawable.pause);

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while setting data source - " + e.getMessage()
					+ ". Printing stack trace:");
			e.printStackTrace();
		}
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_AUDIO);
			serializer.attribute("", XMLStrings.XML_MEDIA_RESOURCE_ID, String.valueOf(mResourceID));
			serializer.attribute("", XMLStrings.XML_MEDIA_DELAY, String.valueOf(mDelay));
			serializer.attribute("", XMLStrings.XML_AUDIO_PLAY_IN_TEST_MODE,
					String.valueOf(mPlayOnTesting));

			if (mMediaPlayer != null)
				mMediaPlayer.stop();

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing audio - " + e.getMessage());
		}
	}

	@Override
	protected void stop()
	{
		// if the audio is played in this mode, stop the audio and change the
		// icon
		// otherwise, do nothing
		MyLog.d(LOG_TAG, "stop() called");

		if (mTesting == false || mPlayOnTesting == true)
		{
			MyLog.d(LOG_TAG, "Stopping the audio");
			if (mMediaPlayer != null)
				mMediaPlayer.stop();
			if (mHandler != null && mRunnable != null)
				mHandler.removeCallbacks(mRunnable);
			if (mIconView != null)
				mIconView.setImageResource(R.drawable.play);
		}
	}

	/**
	 * 
	 * Custom class for listing to media player and view events.
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	class MediaListener implements View.OnTouchListener, MediaPlayer.OnCompletionListener
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			if (mMediaPlayer != null)
			{
				boolean playing = mMediaPlayer.isPlaying();
				if (playing)
				{
					mMediaPlayer.pause();
					mIconView.setImageResource(R.drawable.play);
				} else
				{
					mMediaPlayer.start();
					mIconView.setImageResource(R.drawable.pause);
				}
			} else
			{
				// remove all the callbacks from the handler so as the audio is
				// not played twice on top of each other
				if (mHandler != null && mRunnable != null)
				{
					MyLog.d(LOG_TAG, "Removing callbacks");
					mHandler.removeCallbacks(mRunnable);
				}

				// media player is yet null - initialize and play
				initializeAndPlay();
			}
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp)
		{
			mIconView.setImageResource(R.drawable.play);
		}
	}

	@Override
	public void editComponent(int requestCode)
	{
		// launch text editor
		Intent intent = new Intent(mContext, AudioEditor.class);
		intent.putExtra(AudioEditor.INTENT_DELAY, mDelay);
		intent.putExtra(AudioEditor.INTENT_PLAY_IN_TEST_MODE, mPlayOnTesting);

		mContext.startActivityForResult(intent, requestCode);
	}

	@Override
	public void handleEditResult(Intent intent)
	{
		mDelay = intent.getLongExtra(AudioEditor.INTENT_DELAY, mDelay);
		mPlayOnTesting = intent.getBooleanExtra(AudioEditor.INTENT_PLAY_IN_TEST_MODE, true);
	}

	@Override
	public void deleteResources()
	{
		ResourceHelper resHelper = new ResourceHelper(mContext);

		resHelper.reduceReferenceCount(mCollectionID, mResourceID);

	}

}
