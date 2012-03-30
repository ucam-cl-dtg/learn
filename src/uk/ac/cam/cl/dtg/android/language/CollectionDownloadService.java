package uk.ac.cam.cl.dtg.android.language;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * {@link Service} that downloads collections from collection sharing server.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CollectionDownloadService extends Service implements Runnable
{
	private static final String LOG_TAG = "CollectionDownloadService";
	
	private static final int ONGOING_NOTIFICATION = 0;

	public final static String INTENT_COLLECTION_GLOBAL_ID = "globalID";
	public final static String INTENT_COLLECTION_LOCAL_ID = "localID";

	private long mLocalID, mGlobalID;
	
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	
	private int mNumber = 0;
	private int mNotificationCode = 1;

	@Override
	public void onStart(Intent intent, int startId)
	{
		MyLog.d(LOG_TAG, "onStart() called");

		mLocalID = intent.getLongExtra(INTENT_COLLECTION_LOCAL_ID, -1);
		mGlobalID = intent.getLongExtra(INTENT_COLLECTION_GLOBAL_ID, -1);
		mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
				
		if (mLocalID != -1 && mGlobalID != -1)
		{
			Thread t = new Thread(this);
			t.start();
		}
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}

	@Override
	public void run()
	{
		this.updateOngoingNotificationOnStart();
		
		String tempFileName = String.valueOf(System.currentTimeMillis());

		long globalId = mGlobalID;
		long localId = mLocalID;

		if (ServerHelper.downloadFile(this, globalId, tempFileName, 0))
		{
			boolean resultUnzip = ServerHelper.unzipFile(localId, tempFileName, true);

			MyLog.d(LOG_TAG, "Result of unzipping - " + resultUnzip);

			if (resultUnzip)
			{
				MyLog.d(LOG_TAG, "Downloading and unzipping went successfully");
				
				// hide collection resources away from other applications
				ApplicationInitializer.hideCollectionResources(localId);
				
				// show the notification
				showNotification(localId);
			} else
			{
			  dealWithCollectionDownloadFailure("File could not be unzipped",localId);
			}
		} else
		{
			dealWithCollectionDownloadFailure("File could not be downloaded", localId);
		}

		updateOngoingNotificationOnFinish();
	}

	private void dealWithCollectionDownloadFailure(String message, long localId){
	  MyLog.e(LOG_TAG, message);

    showFailedNotification(localId);

    ApplicationDBAdapter db = new ApplicationDBAdapter(this);
    db.open();
    try {
      db.deleteCollection(localId);
    } finally {
      db.close();
    }
	}
	
	/**
	 * 
	 * Method that shows notification that collection was downloaded successfully.
	 * 
	 * @param localID local collection ID
	 */
	private void showNotification(long localID)
	{
		MyLog.d(LOG_TAG, "showNotification() called");
		int icon = android.R.drawable.stat_sys_download_done;
		CharSequence tickerText = getString(R.string.download_successful);
		long when = System.currentTimeMillis();
		CharSequence contentTitle = getString(R.string.app_name);

		MyLog.d(LOG_TAG, "Changing collection type in the DB!");
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		Collection collection;
		try {
		  collection = db.getCollectionById(localID);
		  db.updateCollectionType(localID, Collection.TYPE_DOWNLOADED_UNLOCKED);
		} finally {
		  db.close();
		}

		CharSequence contentText = getString(R.string.collection) + " \"" + collection.getTitle()
				+ "\" " + getString(R.string.was_downloaded_successfully);

		Intent notificationIntent = new Intent(this, LearningActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
		
		mNotificationManager.notify(mNotificationCode++, notification);		
	}

	/**
	 * 
	 * Shows the notification that upload did not go successfully.
	 * 
	 * @param collectionID local collection ID that was not downloaded successfully
	 */
	private void showFailedNotification(long collectionID)
	{
		int icon = android.R.drawable.stat_notify_error; // icon from

		// resources
		CharSequence tickerText = getString(R.string.download_failed); // ticker-text
		long when = System.currentTimeMillis(); // notification time
		Context context = getApplicationContext(); // application Context
		CharSequence contentTitle = getString(R.string.app_name); // expanded
		// message
		// title

		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		Collection collection;
		try {
		  collection = db.getCollectionById(collectionID);
		} finally {
		  db.close();
		}

		CharSequence contentText = getString(R.string.collection) + " \"" + collection.getTitle()
				+ "\" " + getString(R.string.was_not_updated_successfully); // expanded
		// message
		// text

		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(mNotificationCode++, notification);

	}
	
	private void updateOngoingNotificationOnStart()
	{
		mNumber++;
		
		if (mNumber == 1)
		{
			MyLog.d(LOG_TAG, "showNotification() called");
			int icon = android.R.drawable.stat_sys_download;
			CharSequence tickerText = getString(R.string.collection_being_downloaded);
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.app_name);

			CharSequence contentText = getString(R.string.collection_being_downloaded);

			Intent notificationIntent = new Intent(this, LearningActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			mNotification = new Notification(icon, tickerText, when);
			mNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
			mNotification.flags = mNotification.flags | Notification.FLAG_ONGOING_EVENT;
		}
		else
		{
			MyLog.d(LOG_TAG, "showNotification() called");
			int icon = android.R.drawable.stat_sys_download;
			CharSequence tickerText = getString(R.string.multiple_collections_being_downloaded);
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.app_name);

			CharSequence contentText = getString(R.string.multiple_collections_being_downloaded);

			Intent notificationIntent = new Intent(this, LearningActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			
			if (mNotification == null)
				mNotification = new Notification(icon, tickerText, when);
			
			
			mNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
			mNotification.flags = mNotification.flags | Notification.FLAG_ONGOING_EVENT;
		}
		mNotificationManager.notify(ONGOING_NOTIFICATION, mNotification);
	}
	
	private void updateOngoingNotificationOnFinish()
	{
		mNumber--;
		if (mNumber == 0)
		{
			mNotificationManager.cancel(ONGOING_NOTIFICATION);
			mNotification = null;
		}
		else
		{
			Intent notificationIntent = new Intent(this, LearningActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			
			String text;
			if (mNumber == 1)
			{
				text = getString(R.string.collection_being_downloaded);
			}
			else
			{
				text = getString(R.string.multiple_collections_being_downloaded);
			}
			
			mNotification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
			mNotificationManager.notify(ONGOING_NOTIFICATION, mNotification);
		}
	}

	
	
}

