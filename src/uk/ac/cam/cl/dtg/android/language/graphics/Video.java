package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.ApplicationInitializer;
import uk.ac.cam.cl.dtg.android.language.CardDBAdapter;
import uk.ac.cam.cl.dtg.android.language.CardRenderer;
import uk.ac.cam.cl.dtg.android.language.DelayEditor;
import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.R;
import uk.ac.cam.cl.dtg.android.language.Resource;
import uk.ac.cam.cl.dtg.android.language.ResourceHelper;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.VideoView;

/**
 * 
 * Video component.
 * 
 * @author Vytautas Vaitukaitis
 *
 */
public class Video extends Component
{
	private static final String LOG_TAG = "Video";

	private Activity mContext;
	private long mCollectionID;
	private long mResourceID;
	private long mDelay;
	private VideoView mVideoView;

	private Handler mHandler;
	private Runnable mRunnable;

	public Video(Activity context, long collectionID, long resourceID, long delay)
	{
		mContext = context;
		mCollectionID = collectionID;
		mResourceID = resourceID;
		mDelay = delay;
	}

	// prepares the video to be shown
	public void render()
	{
		MyLog.d(LOG_TAG, "render() called");
		
		// set up the VideoView
		final LinearLayout mainHolder = new LinearLayout(mContext);
		mainHolder.setBaselineAligned(false);
		mainHolder.setGravity(Gravity.CENTER);
		
		ImageView iView = new ImageView(mContext);
		iView.setImageResource(R.drawable.movie);
		mainHolder.addView(iView);
		
		mVideoView = new VideoView(mContext);
		mVideoView.setBackgroundColor(Color.TRANSPARENT);

		// post a delayed thread

		mHandler = new Handler();
		mRunnable = new Runnable()
		{
			
			@Override
			public void run()
			{
				CardDBAdapter db = new CardDBAdapter();
				db.open(mCollectionID);
				Resource res = db.getResource(mResourceID);								
				db.close();
				
				if (res != null)
				{
					String suffix = res.getSuffix();
					
					mVideoView.setVideoPath(ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID + "/" + mResourceID + "." + suffix);
					mVideoView.requestFocus();					
	
					// instantiate the listener and set it as on touch and
					// completion listener
					MediaListener mListener = new MediaListener();
					mVideoView.setOnTouchListener(mListener);
	
					MyLog.d(LOG_TAG, String.valueOf(mDelay
							+ CardRenderer.DURATION_TOTAL_LAG));
	
					mVideoView.start();
					mainHolder.removeAllViews();
					mainHolder.addView(mVideoView, new ViewGroup.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT));
				}

			}
		};

		mHandler.postDelayed(mRunnable, mDelay
				+ CardRenderer.DURATION_TOTAL_LAG);

		mView = mainHolder;

	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_VIDEO);
			serializer.attribute("", XMLStrings.XML_MEDIA_RESOURCE_ID, String.valueOf(mResourceID));
			serializer.attribute("", XMLStrings.XML_MEDIA_DELAY, String.valueOf(mDelay));
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing video - "
					+ e.getMessage());
		}
	}

	@Override
	protected void stop()
	{
		// remove the runnable from the queue
		mVideoView.setVisibility(View.INVISIBLE);
		((LinearLayout) mView).removeView(mVideoView);
		mHandler.removeCallbacks(mRunnable);			
	}

	private class MediaListener implements View.OnTouchListener
	{

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			boolean playing = mVideoView.isPlaying();

			if (playing)
			{
				mVideoView.pause();
			} else
			{
				mVideoView.start();
			}
			return false;
		}
	}

	@Override
	public void editComponent(int requestCode)
	{
		// launch text editor
		Intent intent = new Intent(mContext, DelayEditor.class);
		intent.putExtra(DelayEditor.INTENT_DELAY, mDelay);
			
		mContext.startActivityForResult(intent, requestCode);		
	}

	@Override
	public void handleEditResult(Intent intent)
	{
		mDelay = intent.getLongExtra(DelayEditor.INTENT_DELAY, mDelay);
	}

	@Override
	public void deleteResources()
	{
		ResourceHelper resHelper = new ResourceHelper(mContext);
		
		resHelper.reduceReferenceCount(mCollectionID, mResourceID);

	}
}
