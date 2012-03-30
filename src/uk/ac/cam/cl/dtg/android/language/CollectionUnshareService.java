package uk.ac.cam.cl.dtg.android.language;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * {@link Service} for unsharing collections at the server. Simply sends off a
 * request to the server.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CollectionUnshareService extends Service implements Runnable
{
	private static final String LOG_TAG = "CollectionUnshareService";
	public static final String INTENT_GLOBAL_ID = "collectionID";

	private long mGlobalID;

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		mGlobalID = intent.getLongExtra(INTENT_GLOBAL_ID, -1);

		if (mGlobalID != -1)
		{
			Thread t = new Thread(this);
			t.start();
		} else
		{
			MyLog.e(LOG_TAG, "Seems that collection ID is not correct - not doing anything...");
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

		ServerHelper.unshareCollection(this, globalID);

		this.stopSelf();
	}
}
