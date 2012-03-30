package uk.ac.cam.cl.dtg.android.language;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * {@link Activity} to allow user type in their login details if they have
 * account already.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class UserLoginActivity extends Activity implements Runnable
{
	private static final String LOG_TAG = "UserLoginActivity";

	private static final String USER_CHECK_URL = ServerHelper.URL_LOGIN;

	private static final String BUNDLE_RESPONSE = "response";

	private EditText mEmailAddress;
	private EditText mPassword;
	private Button mOkButton;
	private static ProgressDialog mProgressDialog;

	private static boolean mDialogShown = false;

	public static final int SERVER_RESPONSE_OK = 0;
	public static final int SERVER_RESPONSE_LOGIN_INVALID = 1;

	/**
	 * 
	 * Custom {@link Handler} for handling the messages from the {@link Thread}
	 * which deals with checking login details.
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private class MyHandler extends Handler
	{
		private boolean disabled = false;

		@Override
		public void handleMessage(Message msg)
		{
			if (!disabled)
			{

				String response = msg.getData().getString(BUNDLE_RESPONSE);

				if (mProgressDialog != null)
					mProgressDialog.dismiss();

				mDialogShown = false;

				int responseCode = Integer.parseInt(response);

				switch (responseCode)
				{
				case SERVER_RESPONSE_OK:
					UserLoginActivity.this.setResult(RESULT_OK);

					// change the preferences
					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserLoginActivity.this);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean(getString(R.string.preferences_code_account_registered), true);
					editor.putString(getString(R.string.preferences_code_email_address),
							mEmailAddress.getEditableText().toString());
					editor.putString(getString(R.string.preferences_code_password),
							mPassword.getEditableText().toString());
					editor.commit();

					Toast.makeText(UserLoginActivity.this, R.string.registration_completed,
							Toast.LENGTH_LONG).show();

					UserLoginActivity.this.setResult(RESULT_OK);
					UserLoginActivity.this.finish();
					break;
				case SERVER_RESPONSE_LOGIN_INVALID:
					Toast.makeText(UserLoginActivity.this, R.string.login_invalid,
							Toast.LENGTH_LONG).show();
					break;
				default:
					Toast.makeText(UserLoginActivity.this, R.string.connection_problem,
							Toast.LENGTH_LONG).show();
					break;
				}

				// hide the soft keyboard which for some reason shows up
				InputMethodManager imm = (InputMethodManager) UserLoginActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mEmailAddress.getWindowToken(), 0);

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

	private MyHandler mHandler = new MyHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.userloginform);

		// instantiate all the text views and buttons
		mEmailAddress = (EditText) findViewById(R.id.email_address);
		mEmailAddress.setSingleLine();
		
		mPassword = (EditText) findViewById(R.id.password);
		mPassword.setSingleLine();

		mOkButton = (Button) findViewById(R.id.okbutton);

		mOkButton.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				showProgressDialog();

				mHandler.enable();

				// show the progress dialog and start sending
				Thread t = new Thread(UserLoginActivity.this);
				t.start();
			}
		});

	}

	private void showProgressDialog()
	{
		mDialogShown = true;

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
				mHandler.disable();

				mDialogShown = false;

				mProgressDialog = null;
			}
		});

		mProgressDialog.show();
	}

	/**
	 * 
	 * Method to check login details.
	 * 
	 * @param email
	 *            email address
	 * @param password
	 *            password
	 * @return true if the login details where validated by the server, false
	 *         otherwise.
	 */
	public static String checkLoginDetails(String email, String password)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();
			
			// add the accompanying form data
			form.addPart("email_address", new StringBody(email));
			form.addPart("password", new StringBody(password));

			String response = ServerHelper.getResponseString(USER_CHECK_URL, form);
			
			MyLog.d(LOG_TAG, "Response of the server was - " + response);

			return response;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while sending the form - " + e.getMessage());
			return "-1";
		}
	}

	@Override
	public void run()
	{
		String response = checkLoginDetails(mEmailAddress.getEditableText().toString(),
				mPassword.getEditableText().toString());

		Bundle bundle = new Bundle();
		bundle.putString(BUNDLE_RESPONSE, response);

		Message msg = new Message();
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	@Override
	protected void onPause()
	{
		// stop the progress dialog
		MyLog.d(LOG_TAG, "onPause() is called");

		if (mProgressDialog != null)
		{
			MyLog.d(LOG_TAG, "Dismissing the dialog in onPause()");
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}

		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		MyLog.d(LOG_TAG, "onResume() is called");

		if (mDialogShown && mProgressDialog == null)
			showProgressDialog();

	}
}
