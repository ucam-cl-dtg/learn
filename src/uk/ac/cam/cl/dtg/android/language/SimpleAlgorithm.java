package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.os.Bundle;

/**
 * 
 * Very simple learning algorithm, shows all the cards for testing at once.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class SimpleAlgorithm extends LearningAlgorithm
{
	private ArrayList<Card> mCards;

	private int mPosition;

	private static final String BUNDLE_POSITION = "position";

	public SimpleAlgorithm(ArrayList<Card> cards)
	{
		mCards = cards;
		mPosition = 0;
	}

	public SimpleAlgorithm(ArrayList<Card> cards, Bundle savedState)
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
			result.testing = true;

			return result;
		} catch (Exception e)
		{
			return null;
		}
	}

	@Override
	protected void saveState(Bundle bundle)
	{
		bundle.putInt(BUNDLE_POSITION, mPosition);
	}

}
