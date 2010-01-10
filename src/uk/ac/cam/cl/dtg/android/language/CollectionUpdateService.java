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
 * {@link Service} that goes off and re-uploads collection to the server.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CollectionUpdateService extends Service implements Runnable
{
	private static final String LOG_TAG = "CollectionUpdateService";

	public static final String INTENT_COLLECTION_ID = "collectionID";
	public static final String INTENT_GLOBAL_ID = "globalID";
	private long mCollectionID, mGlobalID;
	
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	
	private int mNumber = 0;
	private static final int ONGOING_NOTIFICATION = 0;
	private int mNotificationCode = 1;

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		mCollectionID = intent.getLongExtra(INTENT_COLLECTION_ID, -1);
		mGlobalID = intent.getLongExtra(INTENT_GLOBAL_ID, -1);
		
		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		
		
		if (mCollectionID != -1 && mGlobalID != -1)
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
		String tempFileName = String.valueOf(System.currentTimeMillis());

		// change the collection type to "currently uploading"
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		db.updateCollectionType(mCollectionID, Collection.TYPE_CURRENTLY_UPLOADING);

		Collection c = db.getCollectionById(mCollectionID);
		// if the IDs don't match - return
		if (c.getGlobalID() != mGlobalID)
			return;

		db.close();

		// preserve these values for the thread
		long collectionID = mCollectionID, globalID = mGlobalID;
		
		this.updateOngoingNotificationOnStart();
		
		if (ServerHelper.zipCollection(this, collectionID, tempFileName,
				Collection.TYPE_PRIVATE_SHARED_COLLECTION))
		{
			// upload it to the web server
			if (ServerHelper.updateFile(this, tempFileName, collectionID, globalID, 0))
			{
				showNotification(collectionID);

				MyLog.d(LOG_TAG, "Zipping and update went successfully");
			} else
			{
				showFailedNotification(collectionID);
				MyLog.e(LOG_TAG, "Update failed");
			}
		} else
		{
			showFailedNotification(collectionID);
			MyLog.e(LOG_TAG, "Zipping failed");
		}
		
		this.updateOngoingNotificationOnFinish();
		
		//this.stopSelf();

	}

	/**
	 * 
	 * Method that shows notification that collection was uploaded successfully.
	 * 
	 * @param localID local collection ID
	 */
	private void showNotification(long localID)
	{
		MyLog.d(LOG_TAG, "showNotification() called");
		int icon = android.R.drawable.stat_sys_upload_done;
		CharSequence tickerText = getString(R.string.update_successful);
		long when = System.currentTimeMillis();
		CharSequence contentTitle = getString(R.string.app_name);

		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		Collection collection = db.getCollectionById(localID);
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
			MyLog.d(LOG_TAG, "showNotification() called");
			int icon = android.R.drawable.stat_sys_upload;
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
			MyLog.d(LOG_TAG, "showNotification() called");
			int icon = android.R.drawable.stat_sys_upload;
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
