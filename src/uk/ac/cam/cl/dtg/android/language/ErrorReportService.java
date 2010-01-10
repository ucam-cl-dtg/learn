package uk.ac.cam.cl.dtg.android.language;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * {@link Service} for uploading error update to the web server.
 * 
 * @author Vytautas Vaitukaitis
 *
 */
public class ErrorReportService extends Service implements Runnable
{
	private static final String LOG_TAG = "ErrorReportService";
	
	public final static String INTENT_COLLECTION_GLOBAL_ID = "globalID";
	public final static String INTENT_CARD_ID = "cardID";
	public final static String INTENT_CARD_TITLE = "cardTitle";
	public final static String INTENT_MESSAGE = "message";
	
	private long mGlobalID, mCardID;
	private String mMessage, mCardTitle;

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		mGlobalID = intent.getLongExtra(INTENT_COLLECTION_GLOBAL_ID, -1);
		mCardID = intent.getLongExtra(INTENT_CARD_ID, -1);
		mMessage = intent.getStringExtra(INTENT_MESSAGE);
		mCardTitle = intent.getStringExtra(INTENT_CARD_TITLE);
		
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
		long cardID = mCardID;
		String cardTitle = mCardTitle;
		String message = mMessage;
		
		ServerHelper.reportError(this, globalID, cardID, cardTitle, message);
		
		this.stopSelf();

	}
	
	
}

