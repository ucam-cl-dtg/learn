package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * 
 * Activity that is used to edit delay for the
 * {@link uk.ac.cam.cl.dtg.android.language.graphics.Video} component.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class DelayEditor extends Activity
{
	public final static String INTENT_DELAY = "delay";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.setContentView(R.layout.delayeditor);

		final EditText textField = (EditText) findViewById(R.id.delay);

		Intent intent = this.getIntent();

		long delay = intent.getLongExtra(INTENT_DELAY, 0);

		textField.setText(String.valueOf(delay));

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
						setResult(RESULT_OK, resultIntent);
						finish();
					}
					else
						showInvalidDialog();

				} catch (Exception e)
				{
					// show the dialog and ask to enter valid long
					showInvalidDialog();
				}

			}
		});
	}
	
	/**
	 * Shows a dialog that entered delay is invalid.
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
