package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

/**
 * 
 * {@link Activity} for creating and editing type-in answers.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TypeInAnswerEditor extends Activity
{
	static final String[] COMMON_PHRASES = new String[]
	{ "What is in the picture?", "What is in the video?", "Write down what you hear" };

	public final static String INTENT_QUESTION = "question";
	public final static String INTENT_CORRECT_ANSWER = "correctAnswer";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.setContentView(R.layout.typeinanswereditor);

		final AutoCompleteTextView questionField = (AutoCompleteTextView) findViewById(R.id.question);
		questionField.setSingleLine();
		
		final EditText correctValueField = (EditText) findViewById(R.id.correctanswer);
		correctValueField.setSingleLine();

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, COMMON_PHRASES);
		questionField.setAdapter(adapter);

		Intent intent = this.getIntent();

		String question = intent.getStringExtra(INTENT_QUESTION);
		if (question == null)
			question = "";
		questionField.setText(question);

		String correctValue = intent.getStringExtra(INTENT_CORRECT_ANSWER);
		if (correctValue == null)
			correctValue = "";
		correctValueField.setText(correctValue);

		Button button = (Button) findViewById(R.id.okbutton);

		button.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String correctValue = correctValueField.getEditableText().toString().trim();
				String question = questionField.getEditableText().toString();
				Intent resultIntent = new Intent();

				resultIntent.putExtra(INTENT_CORRECT_ANSWER, correctValue);
				resultIntent.putExtra(INTENT_QUESTION, question);
				setResult(RESULT_OK, resultIntent);

				finish();
			}
		});
	}

}
