package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.AnswerListener;
import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.R;
import uk.ac.cam.cl.dtg.android.language.TypeInAnswerEditor;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * Component for representing type-in answers.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TypeInAnswer extends Component
{
	private static final String LOG_TAG = "TypeInAnswer";

	private final static int QUESTION_FONT_SIZE = 17;

	private Activity mContext;
	private AnswerListener mListener;
	private String mQuestion;
	private String mCorrectAnswer;
	private boolean mTesting;

	public TypeInAnswer(Activity context, AnswerListener listener, String question,
			String correctAnswer)
	{
		mContext = context;
		mListener = listener;
		mQuestion = question;
		mCorrectAnswer = correctAnswer;
		mTesting = true;
	}

	public void setTesting(boolean testing)
	{
		mTesting = testing;
	}

	public void render()
	{
		LinearLayout layout = new LinearLayout(mContext);

		if (mTesting)
		{

			// create new EditText box
			final EditText te = new EditText(mContext);
			te.setWidth(200);

			// create a button
			Button b = new Button(mContext);
			b.setText(mContext.getString(R.string.ok));
			b.setWidth(60);

			b.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					String typed_in = te.getText().toString();
					typed_in = typed_in.trim();
					typed_in = typed_in.toLowerCase();

					String correctAnswer = mCorrectAnswer.trim();
					correctAnswer = correctAnswer.toLowerCase();

					if (typed_in.equals(correctAnswer))
						mListener.answerCorrect();
					else
						mListener.answerIncorrect();

				}
			});

			// set up the linear layout and put the button and the text field in
			// it

			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setGravity(Gravity.CENTER);
			layout.setPadding(5, 10, 5, 5);

			layout.addView(
					te,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			layout.addView(
					b,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		} else
		{
			TextView tv = new TextView(mContext);
			tv.setTextSize(16);
			tv.setTextColor(Color.WHITE);
			tv.setText(mContext.getString(R.string.correct_answer) + ": ");

			TextView tv2 = new TextView(mContext);
			tv2.setTextSize(16);
			tv2.setText(mCorrectAnswer);

			layout.addView(
					tv,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			layout.addView(
					tv2,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		if (mQuestion.equals(""))
			mView = layout;
		else
		{
			// create another layout that holds both the question and the
			// previous linear layout

			LinearLayout mainHolder = new LinearLayout(mContext);
			mainHolder.setOrientation(LinearLayout.VERTICAL);
			mainHolder.setGravity(Gravity.CENTER);

			TextView tv = new TextView(mContext);
			tv.setTextSize(QUESTION_FONT_SIZE);
			tv.setText(mQuestion);

			mainHolder.addView(
					tv,
					new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			mainHolder.addView(
					layout,
					new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			mView = mainHolder;

		}
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_ANSWER);
			serializer.attribute("", XMLStrings.XML_ANSWER_TYPE, XMLStrings.XML_ANSWER_TYPE_TYPEIN);
			serializer.attribute("", XMLStrings.XML_ANSWER_QUESTION, mQuestion);
			serializer.attribute("", XMLStrings.XML_ANSWER_CORRECT_ANSWER, mCorrectAnswer);

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing type in answer - " + e.getMessage());
		}
	}

	@Override
	protected void stop()
	{
	// TODO Auto-generated method stub

	}

	@Override
	public void editComponent(int requestCode)
	{
		// launch text editor
		Intent intent = new Intent(mContext, TypeInAnswerEditor.class);
		intent.putExtra(TypeInAnswerEditor.INTENT_CORRECT_ANSWER, mCorrectAnswer);
		intent.putExtra(TypeInAnswerEditor.INTENT_QUESTION, mQuestion);

		mContext.startActivityForResult(intent, requestCode);
	}

	@Override
	public void handleEditResult(Intent intent)
	{
		mCorrectAnswer = intent.getStringExtra(TypeInAnswerEditor.INTENT_CORRECT_ANSWER);
		mQuestion = intent.getStringExtra(TypeInAnswerEditor.INTENT_QUESTION);

		// rerender the view
		this.render();
	}

	@Override
	public void deleteResources()
	{
	// TODO Auto-generated method stub

	}
}
