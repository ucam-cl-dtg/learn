package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.TextEditor;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.tts.TTS;
import com.google.tts.TTS.InitListener;

/**
 * 
 * Class that represents text component.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
@SuppressWarnings("deprecation")
public class Text extends Component
{
	private static final String LOG_TAG = "Text";

	public static final int FONT_SIZE = 15;

	private Activity mContext;
	private String mString;
	private boolean mTTSable;
	private com.google.tts.TTS mTTS;

	public Text(Activity context, String s, boolean ttsable)
	{
		mContext = context;
		mString = s;
		mTTSable = ttsable;
	}

	@Override
  public void render()
	{
		// just use the default font size
		int fontSize = FONT_SIZE;

		TextView tv = new TextView(mContext);
		tv.setPadding(5, 5, 5, 5);
		tv.setText(mString);
		tv.setGravity(Gravity.CENTER);

		final String text = mString;
		int length = mString.length();

		if (length < 100)
			fontSize += (10 - length / 10);

		tv.setTextSize(fontSize);

		// for very short pieces of text - no need to use scroll view...
		if (length < 50)
		{
		  LinearLayout mainHolder = new LinearLayout(mContext);
		  mainHolder.setGravity(Gravity.CENTER);
		  mainHolder.addView(
		      tv,
		      new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		  mView = mainHolder;
		} else
		{
		  ScrollView sv = new ScrollView(mContext);

		  sv.addView(
		      tv,
		      new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		  mView = sv;

		}


		if (mTTSable)
		{
			tv.setOnLongClickListener(new View.OnLongClickListener()
			{

				@Override
				public boolean onLongClick(View v)
				{
					mTTS = new TTS(mContext, new InitListener() {
						
						@Override
						public void onInit(int arg0) {
							mTTS.speak(text, 0, null);
							mTTS.shutdown();							
						}
					}, true);

					return true;
				}
			});
		}
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_TEXT);
			serializer.attribute("", XMLStrings.XML_TEXT_VALUE, mString);
			serializer.attribute("", XMLStrings.XML_TEXT_TTSABLE, String.valueOf(mTTSable));
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing text - " + e.getMessage());
		}

	}

	@Override
	protected void stop()
	{}

	@Override
	public void editComponent(int requestCode)
	{
		// launch text editor
		Intent intent = new Intent(mContext, TextEditor.class);
		intent.putExtra(TextEditor.INTENT_TEXT, mString);
		intent.putExtra(TextEditor.INTENT_TTSABLE, mTTSable);

		mContext.startActivityForResult(intent, requestCode);
	}

	@Override
	public void handleEditResult(Intent intent)
	{
		mString = intent.getStringExtra(TextEditor.INTENT_TEXT);
		mTTSable = intent.getBooleanExtra(TextEditor.INTENT_TTSABLE, false);

		// rerender the view
		this.render();
	}

	@Override
	public void deleteResources()
	{
	// TODO Auto-generated method stub

	}
}
