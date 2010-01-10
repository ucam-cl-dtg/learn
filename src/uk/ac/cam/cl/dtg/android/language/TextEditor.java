package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * 
 * {@link Activity} for creating and editing text fields for the cards.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TextEditor extends Activity
{
	public final static String INTENT_TEXT = "text";
	public final static String INTENT_TTSABLE = "ttsable";

	static final String[] COMMON_PHRASES = new String[]
	{ "What is in the picture?", "What is in the video?", "Write down what you hear" };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.setContentView(R.layout.texteditor);

		final AutoCompleteTextView textField = (AutoCompleteTextView) findViewById(R.id.text);
		textField.setGravity(Gravity.TOP | Gravity.LEFT);

		Intent intent = this.getIntent();

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, COMMON_PHRASES);
		textField.setAdapter(adapter);

		String text = intent.getStringExtra(INTENT_TEXT);
		if (text == null)
			text = "";
		textField.setText(text);

		boolean ttsable = intent.getBooleanExtra(INTENT_TTSABLE, false);

		final CheckBox ttsCheckBox = (CheckBox) findViewById(R.id.ttsable);
		ttsCheckBox.setChecked(ttsable);

		Button button = (Button) findViewById(R.id.okbutton);

		button.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String text = textField.getEditableText().toString();
				Intent resultIntent = new Intent();

				resultIntent.putExtra(INTENT_TEXT, text);
				resultIntent.putExtra(INTENT_TTSABLE, ttsCheckBox.isChecked());
				setResult(RESULT_OK, resultIntent);

				finish();
			}
		});
	}

}
