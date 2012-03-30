package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 
 * {@link Activity} for reporting errors spotted in cards created by other
 * users.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ErrorReporter extends Activity
{
	public final static String INTENT_COLLECTION_GLOBAL_ID = "globalID";
	public final static String INTENT_CARD_ID = "cardID";
	public final static String INTENT_CARD_TITLE = "cardTitle";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.setContentView(R.layout.errorreporter);

		final TextView cardString = (TextView) findViewById(R.id.card_string);
		final EditText messageField = (EditText) findViewById(R.id.message);

		messageField.setGravity(Gravity.TOP | Gravity.LEFT);

		Intent intent = this.getIntent();

		final long mGlobalID = intent.getLongExtra(INTENT_COLLECTION_GLOBAL_ID, -1);
		final long cardID = intent.getLongExtra(INTENT_CARD_ID, -1);
		final String cardTitle = intent.getStringExtra(INTENT_CARD_TITLE);

		cardString.setTextColor(Color.WHITE);
		cardString.setText(cardTitle + " (#" + cardID + ")");

		Button button = (Button) findViewById(R.id.okbutton);

		button.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String text = messageField.getEditableText().toString();
				Intent resultIntent = new Intent();

				try
				{
					Intent serviceIntent = new Intent(ErrorReporter.this, ErrorReportService.class);

					serviceIntent.putExtra(ErrorReportService.INTENT_CARD_ID, cardID);
					serviceIntent.putExtra(ErrorReportService.INTENT_CARD_TITLE, cardTitle);
					serviceIntent.putExtra(ErrorReportService.INTENT_COLLECTION_GLOBAL_ID,
							mGlobalID);
					serviceIntent.putExtra(ErrorReportService.INTENT_MESSAGE, text);

					startService(serviceIntent);

					setResult(RESULT_OK, resultIntent);
					finish();

				} catch (Exception e)
				{
					finish();
				}

			}
		});
	}

}
