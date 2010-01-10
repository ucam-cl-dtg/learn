package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * {@link Activity} that deals with the application settings.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class SettingsActivity extends PreferenceActivity
{
	private static final String LOG_TAG = "SettingsActivity";

	private String mPassword;
	private String mEmailAddress;

	private SharedPreferences mPreferences;

	private EditTextPreference mEmailPreference, mPasswordPreference;

	private RegisterAccountDialogPreference mRegisterAccountPreference;

	private ClearAccountDialogPreference mClearAccountPreference;

	private PreferenceGroup mAccountPreferenceGroup;

	private boolean mAccountDataSet;

	private static final int ACTIVITY_REGISTER = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.addPreferencesFromResource(R.xml.preferences);

		// prepare email preference
		mEmailPreference = (EditTextPreference) findPreference(getString(R.string.preferences_code_email_address));
		// update summary when the preference is changed
		mEmailPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1)
			{
				// set a new email address as a summary
				String newEmail = (String) arg1;

				mEmailAddress = newEmail;

				redraw(false);

				return true;
			}
		});
		EditText emailText = mEmailPreference.getEditText();
		emailText.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

		// prepare password preference
		mPasswordPreference = (EditTextPreference) findPreference(getString(R.string.preferences_code_password));

		// set the edit text of the password to be of *** type
		EditText passwordText = mPasswordPreference.getEditText();
		passwordText.setInputType(passwordText.getInputType()
				+ InputType.TYPE_TEXT_VARIATION_PASSWORD);

		// update the password summary when the preference is changed
		mPasswordPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				mPassword = (String) newValue;

				redraw(false);

				return true;
			}
		});

		// add the clear collections active preference
		mAccountPreferenceGroup = (PreferenceGroup) findPreference(getString(R.string.account_details));

		redraw(true);
	}

	/**
	 * 
	 * Method for updating the views in this {@link Activity}. Should be called
	 * when something changes as there is quite a lot of custom stuff going on.
	 * 
	 * @param resetValues
	 *            true if the account values has been changed and should be
	 *            reset, false if this should not be done
	 */
	private void redraw(boolean resetValues)
	{
		if (resetValues)
		{
			// get the password and email address from preferences
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

			mAccountDataSet = mPreferences.getBoolean(
					getString(R.string.preferences_code_account_registered), false);

			mEmailAddress = mPreferences.getString(
					getString(R.string.preferences_code_email_address), "");
			mPassword = mPreferences.getString(getString(R.string.preferences_code_password), "");
		}

		if (!mEmailAddress.equals(""))
			mEmailPreference.setSummary(mEmailAddress);
		else
			mEmailPreference.setSummary(R.string.email_address_summary);

		if (!mEmailAddress.equals("") && !mPassword.equals(""))
			setAccountDataSet(true);

		// clear the email address editable if needed
		if (mEmailAddress.equals(""))
		{
			mEmailPreference.setText("");
		}

		// clear the password editable if needed
		if (mPassword.equals(""))
			mPasswordPreference.setText("");

		String passwordStub = "";
		int length = mPassword.length();
		for (int i = 0; i < length; i++)
			passwordStub = passwordStub + "*";
		if (!passwordStub.equals(""))
		{
			mPasswordPreference.setSummary(passwordStub);

		} else
			mPasswordPreference.setSummary(R.string.password_summary);

		// add the register new account dialog preference
		if (mRegisterAccountPreference == null)
		{
			mRegisterAccountPreference = new RegisterAccountDialogPreference(this, null)
			{};
			mRegisterAccountPreference.setTitle(R.string.register_new_account);
			mRegisterAccountPreference.setSummary(R.string.register_new_account_summary);

			mRegisterAccountPreference.setDialogTitle(R.string.register_new_account);
			mRegisterAccountPreference.setDialogMessage(R.string.are_you_sure_you_want_register_new_account);

			mRegisterAccountPreference.setPositiveButtonText(R.string.yes);
			mRegisterAccountPreference.setNegativeButtonText(R.string.no);

			mAccountPreferenceGroup.addPreference(mRegisterAccountPreference);
		}

		MyLog.d(LOG_TAG, "Account data set - " + mAccountDataSet);

		if (mAccountDataSet)
		{
			if (mClearAccountPreference == null)
			{
				mClearAccountPreference = new ClearAccountDialogPreference(this, null);
				mClearAccountPreference.setTitle(R.string.clear_account_data);
				mClearAccountPreference.setSummary(R.string.clear_account_data_summary);

				mClearAccountPreference.setDialogTitle(R.string.clear_account_data);
				mClearAccountPreference.setDialogMessage(R.string.are_you_sure_you_want_to_delete_account_data);

				mClearAccountPreference.setPositiveButtonText(R.string.yes);
				mClearAccountPreference.setNegativeButtonText(R.string.no);

				mAccountPreferenceGroup.addPreference(mClearAccountPreference);
			}
		} else if (mClearAccountPreference != null)
		{
			mAccountPreferenceGroup.removePreference(mClearAccountPreference);
			mClearAccountPreference = null;
		}

	}

	/**
	 * Custom preference for reseting the account settings.
	 * 
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private class ClearAccountDialogPreference extends DialogPreference
	{

		public ClearAccountDialogPreference(Context context, AttributeSet attrs)
		{
			super(context, attrs);
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if (which == DialogInterface.BUTTON_POSITIVE)
			{
				setAccountDataSet(false);

				Toast.makeText(SettingsActivity.this, R.string.account_data_cleared,
						Toast.LENGTH_SHORT).show();

				redraw(true);

			}

			super.onClick(dialog, which);
		}

	}

	/**
	 * Method called upon setting or reseting the account data. It sorts out the 
	 * 
	 * @param set
	 *            true if the account data was set, false if it was removed
	 */
	private void setAccountDataSet(boolean set)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
		SharedPreferences.Editor editor = preferences.edit();

		mAccountDataSet = set;

		editor.putBoolean(getString(R.string.preferences_code_account_registered), set);

		if (!set)
		{
			editor.remove(getString(R.string.preferences_code_email_address));
			editor.remove(getString(R.string.preferences_code_password));
		}

		editor.commit();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		this.redraw(true);
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 
	 * Custom preference for "register new account" button.
	 * 
	 * @author Vytautas Vaitukaitis
	 *
	 */
	private class RegisterAccountDialogPreference extends DialogPreference
	{

		public RegisterAccountDialogPreference(Context context, AttributeSet attrs)
		{
			super(context, attrs);
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if (which == DialogInterface.BUTTON_POSITIVE)
			{
				Intent intent = new Intent(SettingsActivity.this, UserRegistrationActivity.class);
				SettingsActivity.this.startActivityForResult(intent, ACTIVITY_REGISTER);
			}

			super.onClick(dialog, which);
		}

	}

}
