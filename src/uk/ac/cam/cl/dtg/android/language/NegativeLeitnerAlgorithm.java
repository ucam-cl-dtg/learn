package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.os.Bundle;

/**
 * 
 * Implementation of negative Leitner algorithm. Different from the Leitner
 * algorithm in a sense that this one assumes that user knows all of the cards
 * when they are shown to him for the first time, thus the cards are shown for
 * testing the very first time. If the user doesn't answer correctly, then the
 * card will be shown for learning some other time.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class NegativeLeitnerAlgorithm extends LeitnerAlgorithm
{
	public NegativeLeitnerAlgorithm(ArrayList<Card> cards, ArrayList<StatisticsItem> stats)
	{
		super(cards, stats);
		mInitialBucket = 2;
	}

	public NegativeLeitnerAlgorithm(ArrayList<Card> cards, ArrayList<StatisticsItem> stats,
			Bundle bundle)
	{
		super(cards, stats, bundle);
		mInitialBucket = 2;
	}
}
