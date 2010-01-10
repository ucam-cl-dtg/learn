package uk.ac.cam.cl.dtg.android.language.graphics;

import java.util.ArrayList;
import java.util.Collections;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.AnswerListener;
import uk.ac.cam.cl.dtg.android.language.ApplicationInitializer;
import uk.ac.cam.cl.dtg.android.language.CardDBAdapter;
import uk.ac.cam.cl.dtg.android.language.ImageMCQEditor;
import uk.ac.cam.cl.dtg.android.language.MultipleChoiceAnswer;
import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.R;
import uk.ac.cam.cl.dtg.android.language.ResourceHelper;
import uk.ac.cam.cl.dtg.android.language.TextMCQEditor;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * Class that represents MCQ component - image or text.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class MultipleChoiceAnswerBox extends Component
{
	private static final String LOG_TAG = "MultipleChoiceAnswerBox";

	private MultipleChoiceAnswer mAnswer;
	private Activity mContext;
	private AnswerListener mListener;
	private boolean mTesting;

	private long mCollectionID;

	public MultipleChoiceAnswerBox(Activity context, AnswerListener listener,
			MultipleChoiceAnswer answer, long collectionID)
	{
		mContext = context;
		mListener = listener;
		mAnswer = answer;
		mCollectionID = collectionID;
		mTesting = true;
	}

	public void setTesting(boolean testing)
	{
		mTesting = testing;
	}

	public void render()
	{
		MyLog.d(LOG_TAG, "Starting rendering a multiple choice answer");

		LinearLayout mainHolder = new LinearLayout(mContext);
		mainHolder.setGravity(Gravity.CENTER);

		if (mTesting)
		{

			AnswerAdapter adapter = new AnswerAdapter(mAnswer);

			GridView gv = new GridView(mContext);
			gv.setNumColumns(2);

			// set these so that only the buttons in the grid could be pressed
			// on and focused on
			gv.setClickable(false);
			gv.setLongClickable(false);
			gv.setFocusable(false);

			gv.setAdapter(adapter);

			mainHolder.addView(
					gv,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		} else
		{
			// just show the correct answer
			TextView tv = new TextView(mContext);
			tv.setTextSize(17);
			tv.setTextColor(Color.WHITE);
			tv.setText(mContext.getString(R.string.correct_answer) + ":");

			mainHolder.setOrientation(LinearLayout.VERTICAL);

			mainHolder.addView(
					tv,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			if (mAnswer.isImageType())
			{
				long resourceID = Long.parseLong(mAnswer.getOptions().get(mAnswer.getCorrect()));

				CardDBAdapter db = new CardDBAdapter();
				db.open(mCollectionID);
				String suffix = db.getResource(resourceID).getSuffix();
				db.close();

				ImageView iView = Image.produceImageView(mContext, Uri.parse("file://"
						+ ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID + "/"
						+ resourceID + "." + suffix), false);
				mainHolder.addView(iView);

			} else
			{
				TextView correctAnswerView = new TextView(mContext);
				correctAnswerView.setTextSize(15);
				correctAnswerView.setText(mAnswer.getOptions().get(mAnswer.getCorrect()));

				mainHolder.addView(
						correctAnswerView,
						new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}

		}

		mView = mainHolder;

		MyLog.d(LOG_TAG, "Finished rendering a multiple choice answer");
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			ArrayList<String> options = mAnswer.getOptions();

			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_ANSWER);

			serializer.attribute("", XMLStrings.XML_ANSWER_TYPE, mAnswer.getType());

			serializer.attribute("", XMLStrings.XML_OPTION_1, options.get(0));
			serializer.attribute("", XMLStrings.XML_OPTION_2, options.get(1));
			serializer.attribute("", XMLStrings.XML_OPTION_3, options.get(2));
			serializer.attribute("", XMLStrings.XML_OPTION_4, options.get(3));

			serializer.attribute("", XMLStrings.XML_CORRECT_OPTION,
					String.valueOf(mAnswer.getCorrect()));
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing to XML - " + e.getMessage());
		}
	}

	class AnswerAdapter extends BaseAdapter
	{
		private MultipleChoiceAnswer mAnswer;
		private ArrayList<Integer> mOptionsOrder;

		public AnswerAdapter(MultipleChoiceAnswer answer)
		{
			mAnswer = answer;
			mOptionsOrder = new ArrayList<Integer>();

			for (int i = 0; i < mAnswer.getOptions().size(); i++)
				mOptionsOrder.add(i);

			Collections.shuffle(mOptionsOrder);
		}

		class CorrectListener implements View.OnClickListener
		{
			@Override
			public void onClick(View v)
			{
				mListener.answerCorrect();
			}
		}

		class WrongListener implements View.OnClickListener
		{
			@Override
			public void onClick(View v)
			{
				mListener.answerIncorrect();
			}
		}

		public int getCount()
		{
			return mAnswer.getOptions().size();
		}

		public Object getItem(int position)
		{
			return null;
		}

		public long getItemId(int position)
		{
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent)
		{
			int option = mOptionsOrder.get(position);

			if (mAnswer.isTextType())
			{

				String text = mAnswer.getOptions().get(option);

				if (!text.equals(""))
				{
					Button b = new Button(mContext);
					b.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					b.setText(text);
					b.setSingleLine();
					b.setEllipsize(TextUtils.TruncateAt.MARQUEE);

					if (mAnswer.getCorrect() == option)
						b.setOnClickListener(new CorrectListener());
					else
						b.setOnClickListener(new WrongListener());

					return b;
				} else
					return new View(mContext);
			} else if (mAnswer.isImageType())
			{
				ImageButton ibutton;

				Bitmap bmp = null;

				long resourceID = Long.parseLong(mAnswer.getOptions().get(position));

				CardDBAdapter db = new CardDBAdapter();
				db.open(mCollectionID);
				String suffix = db.getResource(resourceID).getSuffix();
				db.close();

				bmp = Image.produceBitmap(mContext, Uri.parse("file://"
						+ ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID + "/"
						+ mAnswer.getOptions().get(position) + "." + suffix), true);

				ibutton = new ImageButton(mContext);
				ibutton.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 100));
				ibutton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				ibutton.setImageBitmap(bmp);

				if (mAnswer.getCorrect() == option)
					ibutton.setOnClickListener(new CorrectListener());
				else
					ibutton.setOnClickListener(new WrongListener());

				return ibutton;
			} else
				return null;
		}
	}

	@Override
	protected void stop()
	{}

	@Override
	public void editComponent(int requestCode)
	{
		// send off the intent to launch the TextMCQEditor or ImageMCQEditor

		Intent intent;

		if (mAnswer.getType().equals(XMLStrings.XML_ANSWER_TYPE_MC_TEXT))
		{
			intent = new Intent(mContext, TextMCQEditor.class);

			intent.putExtra(TextMCQEditor.INTENT_ACTION, TextMCQEditor.ACTION_EDIT);

			ArrayList<String> options = mAnswer.getOptions();

			intent.putExtra(TextMCQEditor.INTENT_OPTION_1, options.get(0));
			intent.putExtra(TextMCQEditor.INTENT_OPTION_2, options.get(1));
			intent.putExtra(TextMCQEditor.INTENT_OPTION_3, options.get(2));
			intent.putExtra(TextMCQEditor.INTENT_OPTION_4, options.get(3));

			intent.putExtra(TextMCQEditor.INTENT_CORRECT, mAnswer.getCorrect());

			mContext.startActivityForResult(intent, requestCode);

		} else
		{
			intent = new Intent(mContext, ImageMCQEditor.class);

			int imageCount = ImageMCQEditor.IMAGE_COUNT;

			long[] options = new long[imageCount];
			ArrayList<String> optionsList = mAnswer.getOptions();

			for (int i = 0; i < imageCount; i++)
				options[i] = Long.parseLong(optionsList.get(i));

			intent.putExtra(ImageMCQEditor.INTENT_RESOURCE_IDS, options);
			intent.putExtra(ImageMCQEditor.INTENT_CORRECT_ANSWER, mAnswer.getCorrect());

			mContext.startActivityForResult(intent, requestCode);

		}

	}

	@Override
	public void handleEditResult(Intent intent)
	{
		ArrayList<String> options = mAnswer.getOptions();

		if (mAnswer.getType().equals(XMLStrings.XML_ANSWER_TYPE_MC_TEXT))
		{
			options.set(0, intent.getStringExtra(TextMCQEditor.INTENT_OPTION_1));
			options.set(1, intent.getStringExtra(TextMCQEditor.INTENT_OPTION_2));
			options.set(2, intent.getStringExtra(TextMCQEditor.INTENT_OPTION_3));
			options.set(3, intent.getStringExtra(TextMCQEditor.INTENT_OPTION_4));

			mAnswer.setCorrect(intent.getIntExtra(TextMCQEditor.INTENT_CORRECT, 0));
		} else
		{
			long[] imageSources = intent.getLongArrayExtra(ImageMCQEditor.INTENT_RESOURCE_IDS);

			for (int i = 0; i < ImageMCQEditor.IMAGE_COUNT; i++)
				options.set(i, String.valueOf(imageSources[i]));

			mAnswer.setCorrect(intent.getIntExtra(ImageMCQEditor.INTENT_CORRECT_ANSWER, 0));
		}

		this.render();
	}

	@Override
	public void drawYourselfOnto(ViewGroup v, LayoutParams params)
	{
		MyLog.d(LOG_TAG, "Drawing myself onto " + v.getWidth() + "x" + v.getHeight() + " frame");
		super.drawYourselfOnto(v, params);
	}

	@Override
	public void deleteResources()
	{
		if (mAnswer.getType().equals(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE))
		{
			ResourceHelper resHelper = new ResourceHelper(mContext);

			for (String resID : mAnswer.getOptions())
				resHelper.reduceReferenceCount(mCollectionID, Long.parseLong(resID));
		}
	}

}
