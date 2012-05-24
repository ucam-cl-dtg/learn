package uk.ac.cam.cl.dtg.android.language;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.Toast;

/**
 * 
 * Class responsible for dealing with initialization of the application as a
 * whole and initialization of each activity. Also deals with collection updates
 * and unique install ID which is used to identify downloads from the website.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ApplicationInitializer
{
	private static final String LOG_TAG = "ApplicationInitializer";

	/** The folder where collections will be saved. */
	public static final String COLLECTIONS_FOLDER = Environment.getExternalStorageDirectory() + "/collections/";

	/** Progress dialog shown when you have imports going. */
	private ProgressDialog mProgressDialog;

	/** Activity which is being initialized. */
	private Activity mContext;

	/**
	 * 
	 * Sets the context to the passed {@link android.app.Activity}
	 * 
	 * @param activity
	 *            context of the initializer
	 */
	public ApplicationInitializer(Activity activity)
	{
		mContext = activity;
	}

	/**
	 * 
	 * Only checks whether the collections folder exists. Main reason for it not
	 * to exist might be the removal of SD card, thus the error message tells
	 * that.
	 * 
	 * @return whether the collections folder has been located or created
	 *         successfully
	 */
	protected boolean initializeActivity()
	{
		return initializeCollectionsFolder();
	}

	/**
	 * 
	 * Initializes the application - sets the learning algorithm if it has not
	 * been set, checks for updates, tries importing collections and also checks
	 * whether the collections folder exists, thus,
	 * {@link #initializeActivity()} does not have to be called afterwards.
	 * 
	 * @return tells whether the app should continue (whether the initialization
	 *         went smoothly)
	 */
	protected boolean initializeApp()
	{
		// if the algorithm is not set - set the leitner algorithm as a default
		// one
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String prefString = prefs.getString(
				mContext.getString(R.string.preferences_code_learning_algorithm), "-1");

		int algorithmCode = Integer.valueOf(prefString);
		if (algorithmCode == -1)
		{
			Editor editor = prefs.edit();
			editor.putString(mContext.getString(R.string.preferences_code_learning_algorithm),
					String.valueOf(LearningAlgorithm.NEGATIVE_LEITNER_ALGORITHM));
			editor.commit();
		}

		initializeUniqueID();

		boolean folderResult = initializeCollectionsFolder();

		// import any new collections that were dropped in the folder

		if (folderResult)
		{
			importCollections();
			/*
			 * 
			 * Cannot really do this due to a bug in the Android system which
			 * deletes images if folder is marked with .nomedia.
			 * 
			 * boolean resourcesHidden = prefs.getBoolean(mContext
			 * .getString(R.string.preferences_code_resources_hidden), false);
			 * 
			 * if (!resourcesHidden) {
			 * 
			 * hideResources(); Editor editor = prefs.edit();
			 * editor.putBoolean(mContext
			 * .getString(R.string.preferences_code_resources_hidden),true);
			 * editor.commit();
			 * 
			 * }
			 */

		}

		return folderResult;
	}

	/**
	 * 
	 * Checks the collections folder and attempts importing any .zip files found
	 * in there.
	 * 
	 */
	private void importCollections()
	{
		final int MESSAGE_IMPORTED = 0, MESSAGE_NOT_IMPORTED = 1;

		final ProgressDialog dialog = new ProgressDialog(mContext);
		final Handler handler = new Handler()
		{

			@Override
			public void handleMessage(Message msg)
			{
				dialog.dismiss();

				switch (msg.what)
				{
				case MESSAGE_IMPORTED:
					Toast.makeText(mContext, R.string.collections_imported, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_NOT_IMPORTED:
					break;
				}

				super.handleMessage(msg);
			}
		};

		dialog.setTitle(mContext.getString(R.string.importing_collections));
		dialog.setMessage(mContext.getString(R.string.importing_collections_desc));
		dialog.show();

		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				// get the file list from the application folder
				File collectionsFolder = new File(COLLECTIONS_FOLDER);

				boolean imported = false;

				ApplicationDBAdapter db = new ApplicationDBAdapter(mContext);
				db.open();

				try {
				  File[] files = collectionsFolder.listFiles();
				  for (File f : files)
				  {

				    if (!f.isDirectory())
				    {
				      // if it is a zip file, try importing
				      if (f.getName().endsWith("zip"))
				      {
				        // try unzipping it and importing
				        String formattedTime = (String) DateFormat.getDateFormat(mContext).format(
				            new Date(System.currentTimeMillis()));

				        String title = mContext.getString(R.string.imported) + " "
				            + formattedTime;

				        long id = db.insertCollection(title,
				            Collection.TYPE_PRIVATE_NON_SHARED, 0, "", -1);

				        // try unzipping, the files are to be deleted if
				        // import is unsuccessful
				        boolean unzipResult = ServerHelper.unzipFile(id, f.getName(), true);

				        if (!unzipResult)
				        {
				          db.deleteCollection(id);
				        } else
				        {
				          // check whether collection folder contains the
				          // data.db file
				          File dbFile = new File(COLLECTIONS_FOLDER + id + "/data.db");
				          if (dbFile.exists())
				          {
				            // this means that we have just imported a
				            // valid
				            // collection
				            imported = true;
				          } else
				          {
				            // no database file - no use from this
				            // collection
				            db.deleteCollection(id);
				          }

				        }
				      }
				    }
				  }
				} finally {
				  db.close();
				}

				if (imported)
					handler.sendEmptyMessage(MESSAGE_IMPORTED);
				else
					handler.sendEmptyMessage(MESSAGE_NOT_IMPORTED);
			}
		});
		t.start();
	}

	/*
	 * 
	 * Goes through all collections that were downloaded from the web and hides
	 * their resources if they are not already hidden.
	 * 
	 * 
	 * private void hideResources() { final int MESSAGE_FINISHED = 0;
	 * 
	 * final ProgressDialog dialog = new ProgressDialog(mContext); final Handler
	 * handler = new Handler() {
	 * 
	 * @Override public void handleMessage(Message msg) { dialog.dismiss();
	 * 
	 * switch (msg.what) { case MESSAGE_FINISHED: Toast.makeText(mContext,
	 * R.string.resources_hidden, Toast.LENGTH_SHORT).show(); break; }
	 * super.handleMessage(msg); } };
	 * 
	 * dialog.setTitle(mContext.getString(R.string.hiding_resources));
	 * dialog.setMessage(mContext.getString(R.string.hiding_resources_desc));
	 * dialog.show();
	 * 
	 * Thread t = new Thread(new Runnable() {
	 * 
	 * @Override public void run() { ApplicationDBAdapter db = new
	 * ApplicationDBAdapter(mContext); db.open(); ArrayList<Collection>
	 * collections = db.getAllCollections(); db.close();
	 * 
	 * for (Collection c : collections) { if (c.getType() ==
	 * Collection.TYPE_DOWNLOADED_UNLOCKED) {
	 * hideCollectionResources(c.getRowID()); } }
	 * handler.sendEmptyMessage(MESSAGE_FINISHED); } }); t.start(); }
	 */

	/**
	 * 
	 * Checks whether the collections folder exists and attempts creating one if
	 * it does not.
	 * 
	 * @return collection folder exists or it was successfully created
	 */
	private boolean initializeCollectionsFolder()
	{
		boolean folderExists;

		L.d(LOG_TAG, COLLECTIONS_FOLDER);
		
		// check whether collections folder exists
		File collectionsFolder = new File(COLLECTIONS_FOLDER);
		if (collectionsFolder.exists())
		{
			L.d(LOG_TAG, "Folder for the collections exists");
			folderExists = true;
		} else
		{
			L.d(LOG_TAG, "Collection folder does not exist - creating one");
			if (collectionsFolder.mkdir())
			{
				L.d(LOG_TAG, "Collection folder successfully created");
				folderExists = true;

			} else
			{
				L.e(LOG_TAG, "Collection folder could not have been created");
				AlertDialog.Builder noCollectionsFolderAlert = new AlertDialog.Builder(mContext);
				noCollectionsFolderAlert.setTitle(R.string.alert_cannot_locate_collections_folder_title);
				noCollectionsFolderAlert.setIcon(android.R.drawable.ic_dialog_alert);
				noCollectionsFolderAlert.setMessage(R.string.alert_cannot_locate_collections_folder_desc);
				noCollectionsFolderAlert.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener()
						{
							@Override
              public void onClick(DialogInterface dialog, int whichButton)
							{
								mContext.finish();
							}
						});

				noCollectionsFolderAlert.setOnCancelListener(new DialogInterface.OnCancelListener()
				{

					@Override
					public void onCancel(DialogInterface dialog)
					{
						mContext.finish();
					}
				});

				noCollectionsFolderAlert.show();

				folderExists = false;
			}
		}

		return folderExists;
	}

	/**
	 * 
	 * Handler for responding to the messages from the thread that goes off to
	 * the server and checks for any updates.
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private class MyUpdateHandler extends Handler
	{
		private boolean disabled = false;

		private static final int MESSAGE_CODE_NO_UPDATES = 0;
		private static final int MESSAGE_CODE_UPDATES_FOUND_AND_SCHEDULED = 1;
		private static final int MESSAGE_CODE_NO_UPDATES_NO_TOAST = 2;
		private static final int MESSAGE_CODE_CONNECTION_ERROR = 3;

		private static final String MESSAGE_BUNDLE_GLOBAL_IDS_FOR_UPDATES = "globalIDs";

		@Override
		public void handleMessage(Message msg)
		{
			if (!disabled)
			{
				int messageCode = msg.what;

				// switch off the progress dialog
				if (mProgressDialog != null)
				{
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}

				// do the message handling
				switch (messageCode)
				{
				case MESSAGE_CODE_NO_UPDATES:
					Toast.makeText(mContext, R.string.no_updates_exist, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_CODE_UPDATES_FOUND_AND_SCHEDULED:
					// get the collection numbers that have updates

					Bundle bundle = msg.getData();

					long[] updateables = bundle.getLongArray(MESSAGE_BUNDLE_GLOBAL_IDS_FOR_UPDATES);

            ApplicationDBAdapter db = new ApplicationDBAdapter(mContext);
            db.open();
            long[] localIDs;
            try {

              localIDs = new long[updateables.length];

              // get all the local IDs and lock all the collections that
              // are about to be updated
              for (int i = 0; i < updateables.length; i++) {
                try {
                  Collection c = db.getCollectionByGlobalID(updateables[i]);
                  db.updateCollectionType(c.getRowID(), Collection.TYPE_CURRENTLY_DOWNLOADING);

                  localIDs[i] = c.getRowID();
                } catch (IOException e){
                  L.e(LOG_TAG, e.getMessage());
                }
              }

            } finally {
              db.close();
            }

					Intent intent = new Intent(mContext, DownloadUpdatesService.class);

					intent.putExtra(DownloadUpdatesService.INTENT_COLLECTION_GLOBAL_IDS,
							updateables);
					intent.putExtra(DownloadUpdatesService.INTENT_COLLECTION_LOCAL_IDS, localIDs);

					mContext.startService(intent);

					Toast.makeText(mContext, R.string.update_found_downloading, Toast.LENGTH_SHORT).show();

					break;
				case MESSAGE_CODE_NO_UPDATES_NO_TOAST:
					// nothing needs to be done
					break;
				case MESSAGE_CODE_CONNECTION_ERROR:

					Toast.makeText(mContext, R.string.connection_problem, Toast.LENGTH_SHORT).show();

					break;
				}

			}
			super.handleMessage(msg);
		}

		/**
		 * 
		 * Disable the handler so that it does not accidentally receive any
		 * messages from the thread that has been interrupted.
		 * 
		 */
		public void disable()
		{
			disabled = true;
		}

	}

	/**
	 * 
	 * Method to check for updates
	 * 
	 * @param showToastWhenNoUpdatesFound
	 *            tells whether to show toast when no updates are found
	 */
	public void checkForUpdates(boolean showToastWhenNoUpdatesFound)
	{
		mProgressDialog = new ProgressDialog(mContext);

		mProgressDialog.setTitle(R.string.checking_for_updates);
		mProgressDialog.setMessage(mContext.getString(R.string.communicating_with_server));

		final MyUpdateHandler updateHandler = new MyUpdateHandler();

		mProgressDialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface arg0)
			{
				updateHandler.disable();
			}
		});

		mProgressDialog.show();

		final boolean showToast = showToastWhenNoUpdatesFound;

		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				long[] updateables = ServerHelper.checkForUpdates(mContext, 0);

				if (updateables == null)
				{
					// error has occurred
					updateHandler.sendEmptyMessage(MyUpdateHandler.MESSAGE_CODE_CONNECTION_ERROR);
				} else if (updateables.length == 0)
				{
					if (showToast)
						updateHandler.sendEmptyMessage(MyUpdateHandler.MESSAGE_CODE_NO_UPDATES);
					else
						updateHandler.sendEmptyMessage(MyUpdateHandler.MESSAGE_CODE_NO_UPDATES_NO_TOAST);
				} else
				{
					Message msg = new Message();
					msg.what = MyUpdateHandler.MESSAGE_CODE_UPDATES_FOUND_AND_SCHEDULED;

					Bundle bundle = new Bundle();

					bundle.putLongArray(MyUpdateHandler.MESSAGE_BUNDLE_GLOBAL_IDS_FOR_UPDATES,
							updateables);
					msg.setData(bundle);

					updateHandler.sendMessage(msg);
				}
			}
		});

		t.start();

	}

	/**
	 * Initializes the unique ID for application: if it exists, does nothing, if
	 * it doesn't exist, generates and saves a new one. Introduced from version
	 * 1.1. Used as an install ID to track collection downloads. There are 62^40
	 * unique values for these strings - should not collide despite the fact
	 * that they are generated on user behalf. Even if they do, it is not a huge
	 * problem as it would only introduce a small miscounting of collection
	 * downloads.
	 * 
	 */
	private void initializeUniqueID()
	{
		L.d(LOG_TAG, "Initializing unique install ID");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String uniqueID = prefs.getString(mContext.getString(R.string.preferences_unique_id), "");

		if (uniqueID.equals(""))
		{
			// ID is not set, initialize a random one

			char[] chars = "ABCDEFGHIJKLMNOPQRSTUVwXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

			Random r = new Random();

			StringBuilder strBuilder = new StringBuilder();

			int i = 0;

			while (i < 40)
			{
				strBuilder.append(chars[r.nextInt(chars.length)]);
				i++;
			}

			String newUniqueID = strBuilder.toString();

			Editor editor = prefs.edit();
			editor.putString(mContext.getString(R.string.preferences_unique_id), newUniqueID);

			editor.commit();

			L.d(LOG_TAG, "ID not set, setting new one - " + newUniqueID);
		} else
			L.d(LOG_TAG, "ID already set, it is - " + uniqueID);

	}

	/**
	 * Note that this method should not be called for an existing collection as
	 * all the images will get deleted in case they are already found by
	 * MediaScanner - this is a reported Android bug. This will be updated when
	 * I find out that the bug is no longer existing.
	 * 
	 * @param collectionID
	 */
	public static void hideCollectionResources(long collectionID)
	{
		File f = new File(COLLECTIONS_FOLDER + collectionID + "/.nomedia");
		try
		{
			boolean fileResult = f.createNewFile();
			L.d(LOG_TAG, f.getAbsolutePath() + " file has been created - " + fileResult);
		} catch (IOException e)
		{
			L.e(LOG_TAG,
					"Exception caught while trying to create a .nomedia file in the directory");
		}
	}

}
