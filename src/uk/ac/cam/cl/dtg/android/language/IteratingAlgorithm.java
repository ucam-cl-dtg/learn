package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.os.Bundle;

/**
 * 
 * Simple learning algorithm that goes through all cards in turn - every second
 * time in learning mode, other times in testing mode.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class IteratingAlgorithm extends LearningAlgorithm
{
	private ArrayList<Card> mCards;

	private int mPosition;

	private boolean mTesting = false;

	private static final String BUNDLE_POSITION = "position";

	public IteratingAlgorithm(ArrayList<Card> cards)
	{
		mCards = cards;
		mPosition = 0;
	}

	public IteratingAlgorithm(ArrayList<Card> cards, Bundle savedState)
	{
		mCards = cards;
		mPosition = savedState.getInt(BUNDLE_POSITION);
	}

	@Override
	protected NextCard getNext()
	{
		try
		{
			NextCard result = new NextCard();
			result.card = mCards.get(mPosition++);
			result.testing = mTesting;

			return result;
		} catch (Exception e)
		{
			mPosition = 0;
			mTesting = !mTesting;
			return getNext();
		}
	}

	@Override
	protected void saveState(Bundle bundle)
	{
		bundle.putInt(BUNDLE_POSITION, mPosition);
	}

}
