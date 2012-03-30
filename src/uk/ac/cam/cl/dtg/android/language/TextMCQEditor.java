package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * 
 * {@link Activity} for creating and editing text-based multiple-choice answers.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TextMCQEditor extends Activity
{
	private final static String LOG_TAG = "TextMCQEditor";

	public final static String INTENT_OPTION_1 = "option1", INTENT_OPTION_2 = "option2",
			INTENT_OPTION_3 = "option3", INTENT_OPTION_4 = "option4", INTENT_CORRECT = "correct",
			INTENT_ACTION = "action";

	public final static int ACTION_NEW = 0, ACTION_EDIT = 1;
	private RadioGroup mRadioGroup;

	private EditText mText1, mText2, mText3, mText4;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MyLog.d(LOG_TAG, "onCreate() called");

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		super.onCreate(savedInstanceState);

		// set the content view
		setContentView(R.layout.textmcqeditor);

		// initialize all the buttons and radio groups
		mRadioGroup = (RadioGroup) findViewById(R.id.correctGroup);

		mText1 = (EditText) findViewById(R.id.option1_text);
		mText1.setSingleLine();
		
		mText2 = (EditText) findViewById(R.id.option2_text);
		mText2.setSingleLine();
		
		mText3 = (EditText) findViewById(R.id.option3_text);
		mText3.setSingleLine();
		
		mText4 = (EditText) findViewById(R.id.option4_text);
		mText4.setSingleLine();

		Button okButton = (Button) findViewById(R.id.okbutton);
		okButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				// check if everything's alright
				if (checkValues())
				{

					// pack the result up into the intent and send it back
					Intent resultIntent = new Intent();

					resultIntent.putExtra(INTENT_OPTION_1, mText1.getEditableText().toString());
					resultIntent.putExtra(INTENT_OPTION_2, mText2.getEditableText().toString());
					resultIntent.putExtra(INTENT_OPTION_3, mText3.getEditableText().toString());
					resultIntent.putExtra(INTENT_OPTION_4, mText4.getEditableText().toString());

					int correctAnswer = 0;

					switch (mRadioGroup.getCheckedRadioButtonId())
					{
					case R.id.option1:
						correctAnswer = 0;
						break;
					case R.id.option2:
						correctAnswer = 1;
						break;
					case R.id.option3:
						correctAnswer = 2;
						break;
					case R.id.option4:
						correctAnswer = 3;
						break;
					}

					resultIntent.putExtra(INTENT_CORRECT, correctAnswer);

					setResult(RESULT_OK, resultIntent);
					finish();
				}
			}
		});

		// get the intent and analyse it
		Intent intent = this.getIntent();

		int action = intent.getIntExtra(INTENT_ACTION, 0);

		if (action == ACTION_EDIT)
		{
			// initialize the fields
			mText1.setText(intent.getStringExtra(INTENT_OPTION_1));
			mText2.setText(intent.getStringExtra(INTENT_OPTION_2));
			mText3.setText(intent.getStringExtra(INTENT_OPTION_3));
			mText4.setText(intent.getStringExtra(INTENT_OPTION_4));

			int correctAnswer = intent.getIntExtra(INTENT_CORRECT, 0);
			switch (correctAnswer)
			{
			case 0:
				mRadioGroup.check(R.id.option1);
				break;
			case 1:
				mRadioGroup.check(R.id.option2);
				break;
			case 2:
				mRadioGroup.check(R.id.option3);
				break;
			case 3:
				mRadioGroup.check(R.id.option4);
				break;
			}
		}

		MyLog.d(LOG_TAG, "onCreate() finished");

	}

	/**
	 * 
	 * Method to check whether valid values were entered. <b>Note:</b> this only
	 * checks whether any of the answers is selected, need to add functionality
	 * for checking whether none of the options is empty.
	 * 
	 * @return true if values are valid, false otherwise
	 */
	private boolean checkValues()
	{
		if (!(mText1.getEditableText().toString().equals(""))
				&& !(mText2.getEditableText().toString().equals(""))
				&& !(mText3.getEditableText().toString().equals(""))
				&& !(mText4.getEditableText().toString().equals("")))
		{
			// everything's fine
			if (mRadioGroup.getCheckedRadioButtonId() != -1)
			{
				return true;
			} else
			{
				Toast.makeText(this, R.string.none_of_the_answers_selected_correct,
						Toast.LENGTH_SHORT).show();
				return false;
			}
		} else
		{
			Toast.makeText(this, R.string.some_of_text_fields_empty, Toast.LENGTH_SHORT).show();
			return false;
		}

	}
}
