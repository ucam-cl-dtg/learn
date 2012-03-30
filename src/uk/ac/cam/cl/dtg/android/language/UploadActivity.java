package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * 
 * {@link Activity} for collection uploading.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class UploadActivity extends Activity implements Runnable
{
	private static final String LOG_TAG = "UploadActivity";

	private static final int ACTIVITY_CHOOSE_COLLECTION = 0;
	private static final int ACTIVITY_REGISTRATION = 1;

	private final static String BUNDLE_FORM_SHOWN = "form_shown";
	private final static String BUNDLE_DETAILS_VERIFIED = "details_verified";

	private static final String BUNDLE_RESPONSE = "response";

	private boolean mFormShown = false;
	private boolean mDetailsVerified = false;

	private String mEmailAddress, mPassword;

	private ProgressDialog mProgressDialog;

	private class LoginHandler extends Handler
	{
		private boolean disabled = false;

		@Override
		public void handleMessage(Message msg)
		{
			if (!disabled)
			{

				MyLog.d(LOG_TAG, "Handling the message");

				String response = msg.getData().getString(BUNDLE_RESPONSE);

				if (mProgressDialog != null)
				{
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}

				int responseCode;

				try
				{
					responseCode = Integer.parseInt(response);
				} catch (Exception e)
				{
					responseCode = -1;
				}

				switch (responseCode)
				{
				case UserLoginActivity.SERVER_RESPONSE_OK:
					mDetailsVerified = true;
					startCollectionBrowser();
					break;
				case UserLoginActivity.SERVER_RESPONSE_LOGIN_INVALID:
					Toast.makeText(UploadActivity.this, R.string.login_invalid_full,
							Toast.LENGTH_LONG).show();
					UploadActivity.this.finish();
					break;
				default:
					Toast.makeText(UploadActivity.this, R.string.connection_problem,
							Toast.LENGTH_LONG).show();
					UploadActivity.this.finish();
					break;
				}
			}

		}

		private void disable()
		{
			disabled = true;
		}

		private void enable()
		{
			disabled = false;
		}
	}

	private LoginHandler mLoginHandler = new LoginHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			mFormShown = savedInstanceState.getBoolean(BUNDLE_FORM_SHOWN, false);
			mDetailsVerified = savedInstanceState.getBoolean(BUNDLE_DETAILS_VERIFIED, false);
		}

	}

	@Override
	protected void onResume()
	{
		fillData();
		super.onResume();
	}

	/**
	 * 
	 * Method that analyses all the settings (whether login has happened, etc.)
	 * and shows the right thing.
	 * 
	 */
	private void fillData()
	{
		MyLog.d(LOG_TAG, "fillData() called");

		// check if there are any uploadable collections
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();
		int count;
    try {
      count = db.getUploadableCount();
    } finally {
      db.close();
    }

		if (count > 0)
		{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			boolean accountExists = preferences.getBoolean(
					getString(R.string.preferences_code_account_registered), false);

			if (accountExists)
			{
				// check whether login is valid
				mEmailAddress = preferences.getString(
						getString(R.string.preferences_code_email_address), "");
				mPassword = preferences.getString(getString(R.string.preferences_code_password), "");

				if (!mDetailsVerified)
				{
					// start communication with server to verify the details
					showProgressDialog();

					mLoginHandler.enable();

					Thread t = new Thread(this);
					t.start();
				}

			} else
			{
				// start the registration activity

				if (!mFormShown)
				{
					Intent intent = new Intent(this, UserRegistrationActivity.class);

					mFormShown = true;

					Toast.makeText(this, R.string.no_account_yet_toast, Toast.LENGTH_LONG).show();

					this.startActivityForResult(intent, ACTIVITY_REGISTRATION);
				}
			}
		} else
		{
			// show dialog suggesting to go to the editing activity to create
			// collections
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.no_collections);
			alert.setMessage(R.string.no_collections_for_uploading_desc);

			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Intent intent = new Intent(UploadActivity.this, CollectionBrowser.class);
					intent.putExtra(CollectionBrowser.INTENT_ACTION,
							CollectionBrowser.INTENT_ACTION_PICK_FOR_EDITING);
					startActivity(intent);

					finish();
				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});

			alert.setOnCancelListener(new DialogInterface.OnCancelListener()
			{

				@Override
				public void onCancel(DialogInterface dialog)
				{
					finish();
				}
			});

			alert.show();

		}

	}

	/**
	 * Method to start collection browser that shows all the uploadable
	 * collections.
	 * 
	 */
	private void startCollectionBrowser()
	{
		Intent intent = new Intent(this, CollectionBrowser.class);

		intent.putExtra(CollectionBrowser.INTENT_ACTION,
				CollectionBrowser.INTENT_ACTION_PICK_FOR_UPLOADING);

		startActivityForResult(intent, ACTIVITY_CHOOSE_COLLECTION);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
			case ACTIVITY_CHOOSE_COLLECTION:
				MyLog.d(LOG_TAG, "Starting the upload service....");
				long collectionID = data.getLongExtra(CollectionBrowser.INTENT_COLLECTION_ID, 0);

				// check whether the collection has been uploaded before
				ApplicationDBAdapter db = new ApplicationDBAdapter(this);
				db.open();
				Collection collection;
				try {
				  collection = db.getCollectionById(collectionID);
				} finally {
				  db.close();
				}

				int type = collection.getType();

				switch (type)
				{
				case Collection.TYPE_PRIVATE_NON_SHARED:
					// start the upload service
					Intent intent = new Intent(this, CollectionUploadService.class);
					intent.putExtra(CollectionUploadService.INTENT_COLLECTION_ID, collectionID);

					this.startService(intent);

					Toast.makeText(this, R.string.upload_started, Toast.LENGTH_LONG).show();

					break;
				case Collection.TYPE_PRIVATE_SHARED_COLLECTION:

					Intent intentUpdate = new Intent(this, CollectionUpdateService.class);
					intentUpdate.putExtra(CollectionUpdateService.INTENT_COLLECTION_ID,
							collectionID);
					intentUpdate.putExtra(CollectionUpdateService.INTENT_GLOBAL_ID,
							collection.getGlobalID());

					this.startService(intentUpdate);

					Toast.makeText(this, R.string.update_started, Toast.LENGTH_LONG).show();

					break;
				}

				// restart the collection browser
				startCollectionBrowser();
				break;
			case ACTIVITY_REGISTRATION:
				mFormShown = false;
				break;
			}
		} else if (resultCode == RESULT_CANCELED)
		{
			this.finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 
	 * Method to show progress dialog whilst logging in.
	 * 
	 */
	private void showProgressDialog()
	{
		MyLog.d(LOG_TAG, "showProgressDialog() called");

		// show the progress dialog and start sending
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.login);
		mProgressDialog.setMessage(getString(R.string.communicating_with_server));
		mProgressDialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface arg0)
			{
				// just finish the activity as user cancelled the dialog
				mLoginHandler.disable();

				mProgressDialog = null;

				setResult(RESULT_CANCELED);
				finish();
			}
		});

		mProgressDialog.show();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(BUNDLE_FORM_SHOWN, this.mFormShown);
		outState.putBoolean(BUNDLE_DETAILS_VERIFIED, this.mDetailsVerified);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void run()
	{
		String response = UserLoginActivity.checkLoginDetails(mEmailAddress, mPassword);

		Bundle bundle = new Bundle();
		bundle.putString(BUNDLE_RESPONSE, response);

		Message msg = new Message();
		msg.setData(bundle);
		mLoginHandler.sendMessage(msg);
	}

}
