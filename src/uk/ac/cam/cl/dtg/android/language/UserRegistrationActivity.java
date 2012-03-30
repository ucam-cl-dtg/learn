package uk.ac.cam.cl.dtg.android.language;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
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
 * {@link Activity} for dealing with user registration.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class UserRegistrationActivity extends Activity implements Runnable
{
	private static final String LOG_TAG = "UserRegistrationActivity";

	private static final String BUNDLE_RESPONSE = "response";
	private static final String BUNDLE_EMAIL_ADDRESS = "email_address";
	private static final String BUNDLE_PASSWORD = "password";

	private EditText mEmailAddress;
	private EditText mPassword;
	private EditText mPasswordRetyped;
	private EditText mFullName;
	private Button mOkButton, mLoginButton;
	private static ProgressDialog mProgressDialog;

	private static boolean showingDialog = false;

	private static final int SERVER_RESPONSE_OK = 0;
	private static final int SERVER_RESPONSE_EMAIL_INVALID = 1;
	private static final int SERVER_RESPONSE_EMAIL_ALREADY_REGISTERED = 2;
	private static final int SERVER_RESPONSE_PASSWORD_TOO_SHORT = 3;
	private static final int SERVER_RESPONSE_FULL_NAME_INVALID = 4;
	private static final int SERVER_RESPONSE_SYSTEM_FAULT = 5;

	private static final int ACTIVITY_LOGIN = 0;

	private class MyHandler extends Handler
	{
		private boolean disabled = false;

		@Override
		public void handleMessage(Message msg)
		{
			if (!disabled)
			{
				MyLog.d(LOG_TAG, "Handle message called");

				MyLog.d(LOG_TAG, "Handling the message");

				String response = msg.getData().getString(BUNDLE_RESPONSE);

				if (mProgressDialog != null)
				{
					MyLog.d(LOG_TAG, "Progress dialog is not null - dismissing it");
					mProgressDialog.dismiss();
				} else
					MyLog.d(LOG_TAG, "Progress dialog is null");

				showingDialog = false;

				int responseCode;

				try
				{
					responseCode = Integer.parseInt(response);
				} catch (Exception e)
				{
					responseCode = SERVER_RESPONSE_SYSTEM_FAULT;
				}

				MyLog.d(LOG_TAG, "Response code is - " + responseCode);

				switch (responseCode)
				{
				case SERVER_RESPONSE_OK:
					MyLog.d(LOG_TAG, "Handling positive response");

					// change the preferences
					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(UserRegistrationActivity.this);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean(getString(R.string.preferences_code_account_registered), true);
					editor.putString(getString(R.string.preferences_code_email_address),
							msg.getData().getString(BUNDLE_EMAIL_ADDRESS));
					editor.putString(getString(R.string.preferences_code_password),
							msg.getData().getString(BUNDLE_PASSWORD));
					editor.commit();

					Toast.makeText(UserRegistrationActivity.this, R.string.registration_completed,
							Toast.LENGTH_LONG).show();

					UserRegistrationActivity.this.setResult(RESULT_OK);
					UserRegistrationActivity.this.finish();
					break;
				case SERVER_RESPONSE_EMAIL_INVALID:
					Toast.makeText(UserRegistrationActivity.this, R.string.email_invalid,
							Toast.LENGTH_LONG).show();
					break;
				case SERVER_RESPONSE_EMAIL_ALREADY_REGISTERED:
					Toast.makeText(UserRegistrationActivity.this,
							R.string.email_already_registered, Toast.LENGTH_LONG).show();
					break;
				case SERVER_RESPONSE_PASSWORD_TOO_SHORT:
					Toast.makeText(UserRegistrationActivity.this, R.string.password_too_short,
							Toast.LENGTH_LONG).show();
					break;
				case SERVER_RESPONSE_FULL_NAME_INVALID:
					Toast.makeText(UserRegistrationActivity.this, R.string.full_name_invalid,
							Toast.LENGTH_LONG).show();
					break;
				case SERVER_RESPONSE_SYSTEM_FAULT:
					Toast.makeText(UserRegistrationActivity.this, R.string.system_fault,
							Toast.LENGTH_LONG).show();
					break;
				}

				// hide the soft keyboard which for some reason shows up
				InputMethodManager imm = (InputMethodManager) UserRegistrationActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
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

	private MyHandler mHandler;

	private static final String REGISTRATION_URL = ServerHelper.URL_REGISTER;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MyLog.d(LOG_TAG, "onCreate() called");

		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.userregistrationform);

		if (mHandler == null)
			mHandler = new MyHandler();

		// instantiate all the text views and buttons
		mEmailAddress = (EditText) findViewById(R.id.email_address);
		mEmailAddress.setSingleLine();
		
		mPassword = (EditText) findViewById(R.id.password);
		mPassword.setSingleLine();
		
		mPasswordRetyped = (EditText) findViewById(R.id.retyped_password);
		mPasswordRetyped.setSingleLine();
		
		mFullName = (EditText) findViewById(R.id.full_name);
		mFullName.setSingleLine();

		mOkButton = (Button) findViewById(R.id.okbutton);
		mLoginButton = (Button) findViewById(R.id.loginbutton);

		/*
		 * if (savedInstanceState != null) showingDialog =
		 * savedInstanceState.getBoolean(BUNDLE_SHOWING_DIALOG, false);
		 */
		mOkButton.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (checkForm())
				{
					showProgressDialog();

					mHandler.enable();

					Thread t = new Thread(UserRegistrationActivity.this);
					t.start();
				}
			}
		});

		mLoginButton.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(UserRegistrationActivity.this, UserLoginActivity.class);
				UserRegistrationActivity.this.startActivityForResult(intent, ACTIVITY_LOGIN);
			}
		});
	}

	/**
	 * Method to bring up a progress dialog.
	 * 
	 */
	private void showProgressDialog()
	{
		MyLog.d(LOG_TAG, "showProgressDialog() called");

		showingDialog = true;

		// show the progress dialog and start sending
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(R.string.registration);
		mProgressDialog.setMessage(getString(R.string.communicating_with_server));
		mProgressDialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface arg0)
			{
				// just finish the activity as user cancelled the dialog
				mHandler.disable();

				showingDialog = false;

				mProgressDialog = null;
			}
		});

		mProgressDialog.show();

	}

	/**
	 * Method to check form values before sending off the registration details
	 * to the server.
	 * 
	 * @return true if details where validated, false otherwise
	 */
	private boolean checkForm()
	{
		String password = mPassword.getEditableText().toString(), password_retyped = mPasswordRetyped.getEditableText().toString();

		if (password.equals(password_retyped))
			return true;
		else
		{
			Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	/**
	 * 
	 * Method to send registration request to the server.
	 * 
	 * @param emailAddress
	 *            email address
	 * @param password
	 *            password
	 * @param fullName
	 *            full name
	 * @return
	 */
	private String sendRegistrationForm(String emailAddress, String password, String fullName)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();
			
			// add the accompanying form data
			form.addPart("email_address", new StringBody(emailAddress));
			form.addPart("password", new StringBody(password));
			form.addPart("full_name", new StringBody(fullName));

			String response = ServerHelper.getResponseString(REGISTRATION_URL, form);
			
			MyLog.d(LOG_TAG, "Response of the server was - " + response);

			return response;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while sending the form - " + e.getMessage());
			return null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == ACTIVITY_LOGIN)
		{
			// finish the registration activity only if the login was successful
			if (resultCode == RESULT_OK)
			{
				this.setResult(RESULT_OK);
				this.finish();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void run()
	{
		String emailAddress = mEmailAddress.getEditableText().toString();
		String password = mPassword.getEditableText().toString();
		String fullName = mFullName.getEditableText().toString();

		String response = sendRegistrationForm(emailAddress, password, fullName);

		Bundle bundle = new Bundle();
		bundle.putString(BUNDLE_RESPONSE, response);
		bundle.putString(BUNDLE_EMAIL_ADDRESS, emailAddress);
		bundle.putString(BUNDLE_PASSWORD, password);

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

		if (showingDialog)
			showProgressDialog();
	}
}
