package uk.ac.cam.cl.dtg.android.language;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * {@link Service} that uploads user's rating for some shared collection 
 * 
 * @author Vytautas Vaitukaitis
 *
 */
public class CollectionRatingService extends Service implements Runnable
{
	private static final String LOG_TAG = "CollectionRatingService";

	public static final String INTENT_GLOBAL_ID = "collectionID";
	public static final String INTENT_RATING = "rating";
	
	private long mGlobalID;
	private int mRating;

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		mGlobalID = intent.getLongExtra(INTENT_GLOBAL_ID, -1);
		mRating = intent.getIntExtra(INTENT_RATING, -1);
		
		if (mGlobalID != -1 && mRating != -1)
		{
			Thread t = new Thread(this);
			t.start();
		} else
		{
			L.e(LOG_TAG, "Seems that collection ID is not correct - not doing anything...");
			this.stopSelf();
		}

	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	
	@Override
	public void run()
	{
		long globalID = mGlobalID;
		int rating = mRating;

		ServerHelper.updateRating(this, globalID, rating);
		
		this.stopSelf();

	}
	
	
}

