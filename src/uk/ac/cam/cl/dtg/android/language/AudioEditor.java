package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * 
 * {@link Activity} for editing audio components from the {@link CardEditor}.
 * Used to set the delay and whether audio is played in test mode.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class AudioEditor extends Activity
{
	/** {@link Intent} value for the delay - must be of long type */
	public final static String INTENT_DELAY = "delay";

	/**
	 * {@link Intent} value for boolean telling whether audio is played in
	 * testing mode.
	 */
	public final static String INTENT_PLAY_IN_TEST_MODE = "playInTestingMode";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())
		{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			this.setContentView(R.layout.audioeditor);

			Intent intent = this.getIntent();

			// initialize delay field
			final EditText textField = (EditText) findViewById(R.id.delay);
			long delay = intent.getLongExtra(INTENT_DELAY, 0);
			textField.setText(String.valueOf(delay));

			// initialize play in test mode checkbox
			boolean playInTestMode = intent.getBooleanExtra(INTENT_PLAY_IN_TEST_MODE, false);
			final CheckBox playInTestModeCheckBox = (CheckBox) findViewById(R.id.play_in_test_mode);
			playInTestModeCheckBox.setChecked(playInTestMode);

			// initialize OK button
			Button button = (Button) findViewById(R.id.okbutton);
			button.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					String text = textField.getEditableText().toString();
					Intent resultIntent = new Intent();

					try
					{
						long delay = Long.parseLong(text);
						if (delay >= 0)
						{
							resultIntent.putExtra(INTENT_DELAY, delay);
							resultIntent.putExtra(INTENT_PLAY_IN_TEST_MODE,
									playInTestModeCheckBox.isChecked());
							setResult(RESULT_OK, resultIntent);
							finish();
						} else
						{
							showInvalidDialog();
						}

					} catch (Exception e)
					{
						// show the dialog and ask to enter valid long
						showInvalidDialog();
					}
				}
			});
		}
	}

	/**
	 * 
	 * Shows the dialog that the typed in delay value is invalid.
	 * 
	 */
	private void showInvalidDialog()
	{
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(R.string.alert);
		alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
		alertBuilder.setMessage(getString(R.string.type_in_valid_number));
		alertBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{

			}
		});

		alertBuilder.show();

	}

}
