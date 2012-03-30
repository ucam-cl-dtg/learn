package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

/**
 * 
 * Class that represents multiple choice answer.
 * 
 * @author Vytautas Vaitukaitis
 *
 */
public class MultipleChoiceAnswer
{
	private String mType; // image or text
	private ArrayList<String> mAnswers; // either option strings or paths to the images
	private int mCorrect = 0;
	
	public MultipleChoiceAnswer(String type)
	{
		mType = type;
		mAnswers = new ArrayList<String>();
	}
	
	public void addOption(String value)
	{
		mAnswers.add(value);
	}
	
	public void addOption(String value, boolean correct)
	{
		mAnswers.add(value);
		
		if (correct)
			mCorrect = mAnswers.size() - 1;
	}
	
	public void setCorrect(int id)
	{
		mCorrect = id;
	}
	
	public ArrayList<String> getOptions()
	{
		return mAnswers;
	}
	
	public int getCorrect()
	{
		return mCorrect;
	}
	
	public boolean isImageType()
	{
		if (mType.equals(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE))
			return true;
		else
			return false;		
	}
	
	public boolean isTextType()
	{
		if (mType.equals(XMLStrings.XML_ANSWER_TYPE_MC_TEXT))
			return true;
		else
			return false;		
	}
	
	public String getType()
	{
		return mType;
	}
}
