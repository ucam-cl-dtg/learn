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
 * Downloads collection updates as given in the intent. Collections must be locked (type set to CURRENTLY_DOWNLOADING).
 * 
 * @author Vytautas
 *
 */
public class DownloadUpdatesService extends Service implements Runnable
{
	//private static final String LOG_TAG = "CheckUpdateService";

	public final static String INTENT_COLLECTION_GLOBAL_IDS = "globalIDs";
	public final static String INTENT_COLLECTION_LOCAL_IDS = "localIDs";
	
	private long[] mLocalIDs, mGlobalIDs;
	
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	
	private int mNumber = 0;
	private static final int ONGOING_NOTIFICATION = 0;
	private int mNotificationCode = 1;
	
	@Override
	public void onStart(Intent intent, int startId)
	{
		
		mLocalIDs = intent.getLongArrayExtra(INTENT_COLLECTION_LOCAL_IDS);
		mGlobalIDs = intent.getLongArrayExtra(INTENT_COLLECTION_GLOBAL_IDS);
		
		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		
		if (mLocalIDs != null && mGlobalIDs != null)
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
		
		long[] globalIDs = mGlobalIDs;
		long[] localIDs = mLocalIDs;
		
		// for all of the collections - download a file and replace the contents with this file
		
		for (int i = 0; i < globalIDs.length; i++)
			updateOngoingNotificationOnStart();
		
		for (int i = 0; i < globalIDs.length; i++)
		{
			String tempFileName = String.valueOf(System.currentTimeMillis());
			long globalID = globalIDs[i];
			long localID = localIDs[i];
			
			
			if (ServerHelper.downloadFile(this, globalID, tempFileName, 0))
			{
				boolean resultUnzip = ServerHelper.unzipFile(localID, tempFileName, true);
		
				if (resultUnzip)
				{
					// show the notification
					unlockAndShowNotification(localID);
				} else
				{
					// unlock the collection			
					ApplicationDBAdapter db = new ApplicationDBAdapter(this);
					
					db.open();
					
					db.updateCollectionType(localID, Collection.TYPE_DOWNLOADED_UNLOCKED);
					
					db.close();
					showFailedNotification(localID);
				}
			} else
			{
				// unlock the collection			
				ApplicationDBAdapter db = new ApplicationDBAdapter(this);
				
				db.open();
				
				db.updateCollectionType(localID, Collection.TYPE_DOWNLOADED_UNLOCKED);
				
				db.close();		
				showFailedNotification(localID);
				
			}
			updateOngoingNotificationOnFinish();
			
		}
	}	

	/**
	 * 
	 * Method that shows notification that collection was uploaded successfully.
	 * 
	 * @param localID local collection ID
	 */
	private void unlockAndShowNotification(long localID)
	{
		int icon = android.R.drawable.stat_sys_download_done;
		CharSequence tickerText = getString(R.string.update_successful);
		long when = System.currentTimeMillis();
		CharSequence contentTitle = getString(R.string.app_name);

		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		Collection collection = db.getCollectionById(localID);
		db.updateCollectionType(localID, Collection.TYPE_DOWNLOADED_UNLOCKED);

		db.close();

		CharSequence contentText = getString(R.string.collection) + " \"" + collection.getTitle()
				+ "\" " + getString(R.string.was_updated_successfully);

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
		CharSequence tickerText = getString(R.string.update_failed); // ticker-text
		long when = System.currentTimeMillis(); // notification time
		Context context = getApplicationContext(); // application Context
		CharSequence contentTitle = getString(R.string.app_name); // expanded

		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		
		db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_SHARED_COLLECTION);
		Collection collection = db.getCollectionById(collectionID);

		db.close();

		CharSequence contentText = getString(R.string.collection) + " \"" + collection.getTitle()
				+ "\" " + getString(R.string.was_not_updated_successfully); // expanded

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
			int icon = android.R.drawable.stat_sys_download;
			CharSequence tickerText = getString(R.string.collection_being_updated);
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.app_name);

			CharSequence contentText = getString(R.string.collection_being_updated);

			Intent notificationIntent = new Intent(this, LearningActivity.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			mNotification = new Notification(icon, tickerText, when);
			mNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
			mNotification.flags = mNotification.flags | Notification.FLAG_ONGOING_EVENT;
		}
		else
		{
			int icon = android.R.drawable.stat_sys_download;
			CharSequence tickerText = getString(R.string.multiple_collections_being_updated);
			long when = System.currentTimeMillis();
			CharSequence contentTitle = getString(R.string.app_name);

			CharSequence contentText = getString(R.string.multiple_collections_being_updated);

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
				text = getString(R.string.collection_being_updated);
			}
			else
			{
				text = getString(R.string.multiple_collections_being_updated);
			}
			
			mNotification.setLatestEventInfo(this, getString(R.string.app_name), text, contentIntent);
			mNotificationManager.notify(ONGOING_NOTIFICATION, mNotification);
		}
	}
	
}
