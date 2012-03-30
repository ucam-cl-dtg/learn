package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import android.os.Bundle;

/**
 * 
 * One of the learning algorithms. Sorts the cards out into the buckets
 * depending on how well the user is doing on them.
 * 
 * When the user hasn't learnt the collection at all, all of the cards are in
 * the bucket 1. Exposure of the card or correct answer to it will move the card
 * one bucket up. Incorrect answer will move the card one bucket down. Cards
 * from the bucket 1 have twice greater probability of being shown than the ones
 * from the bucket 2, 3 times greater probability than the ones from bucket 3,
 * and so on.
 * 
 * Cards from the bucket 1 are shown for learning whilst the cards from the
 * further buckets are shown for testing. Bucket number cannot become less than
 * 1.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class LeitnerAlgorithm extends LearningAlgorithm
{
	protected static final String LOG_TAG = "ShowLeastLearntAlgorithm";

	protected static final String BUNDLE_TESTING = "testing";
	protected static final String BUNDLE_CARD_IDS = "cardIDs";

	protected HashMap<Long, BucketCard> mCards;

	protected HashMap<Integer, Bucket> mBuckets;

	protected ArrayList<StatisticsItem> mStatistics;

	protected ArrayList<NextCard> mToShow = new ArrayList<NextCard>();
	
	protected int mInitialBucket = 1;

	protected class Bucket
	{
		ArrayList<Card> cards;
		int probability;
	}

	protected class BucketCard
	{
		int bucket;
		Card card;
	}

	public LeitnerAlgorithm(ArrayList<Card> cards, ArrayList<StatisticsItem> stats)
	{
		// convert all the cards into bucket card format
		mCards = new HashMap<Long, BucketCard>();

		for (Card c : cards)
		{
			BucketCard bc = new BucketCard();
			bc.card = c;
			bc.bucket = 0;

			mCards.put(c.getId(), bc);
		}

		mStatistics = stats;
	}

	public LeitnerAlgorithm(ArrayList<Card> cards, ArrayList<StatisticsItem> stats, Bundle bundle)
	{
		// convert all the cards into bucket card format
		mCards = new HashMap<Long, BucketCard>();

		for (Card c : cards)
		{
			BucketCard bc = new BucketCard();
			bc.card = c;
			bc.bucket = 0;

			mCards.put(c.getId(), bc);
		}

		mStatistics = stats;

		// recreate the mToShow list from the bundle
		long[] cardIDs = bundle.getLongArray(BUNDLE_CARD_IDS);
		boolean[] testing = bundle.getBooleanArray(BUNDLE_TESTING);

		for (int i = 0; i < cardIDs.length; i++)
		{
			NextCard nc = new NextCard();
			nc.testing = testing[i];
			nc.card = mCards.get(cardIDs[i]).card;

			mToShow.add(nc);
		}
	}

	@Override
	protected NextCard getNext()
	{
		// check the last card - if it was incorrect
		int size = mStatistics.size();
		if (size > 0)
		{
			StatisticsItem lastAttempt = mStatistics.get(size - 1);

			// if it was tested on and was answered incorrectly - show the
			// result

			if (lastAttempt.wasInTestingMode() && !lastAttempt.isCorrect())
			{
				BucketCard c = mCards.get(lastAttempt.getCardID());
				
				// check if the card was not deleted afterwards
				if (c != null)
				{
					NextCard nextCard = new NextCard();
					nextCard.card = c.card;
					nextCard.testing = false;
	
					return nextCard;
				}
			}
		}

		if (mToShow.size() > 0)
		{
			return mToShow.remove(0);
		} else
		{
			compileToShow();

			try
			{
				return mToShow.remove(0);
			} catch (Exception e)
			{
				MyLog.d(LOG_TAG, "compileToShow() returned empty");
				return null;
			}
		}
	}

	/**
	 * Method that analyses statistics and compiles list of the cards to be
	 * shown next.
	 * 
	 */
	protected void compileToShow()
	{
		// reset the bucket count
		MyLog.d(LOG_TAG, "Starting compileToShow()");

		Set<Long> set = mCards.keySet();
		for (long l : set)
		{
			mCards.get(l).bucket = mInitialBucket;
		}

		// go through the statistics and sort out the bucket count
		BucketCard c;
		StatisticsItem lastItem = null;

		for (StatisticsItem item : mStatistics)
		{
			// find the card
			c = mCards.get(item.getCardID());

			if (c != null)
			{
				if (!item.wasInTestingMode())
				{
					// only count this if the card was shown not after it was
					// attempted and answered incorrectly
					if (lastItem == null
							|| !(lastItem.wasInTestingMode() && !lastItem.isCorrect() && lastItem.getCardID() == item.getCardID()))
						c.bucket++;
				} else
				{
					if (item.isCorrect())
						c.bucket++;
					else
					{
						if (c.bucket != 1)
							c.bucket--;
					}
				}
			}

			lastItem = item;
		}

		// sort all the cards into buckets
		mBuckets = new HashMap<Integer, Bucket>();

		BucketCard bc;

		for (long l : set)
		{
			bc = mCards.get(l);

			MyLog.d(LOG_TAG, "Putting card " + l + " into bucket " + bc.bucket);

			if (mBuckets.get(bc.bucket) == null)
			{
				Bucket newBucket = new Bucket();
				newBucket.cards = new ArrayList<Card>();
				newBucket.cards.add(bc.card);

				mBuckets.put(bc.bucket, newBucket);
			} else
			{
				mBuckets.get(bc.bucket).cards.add(bc.card);
			}
		}

		// MyLog.d(LOG_TAG, "Sorting cards into the buckets");

		// sort out the cards to the order they should be shown

		long i = 1;

		Set<Integer> bucketKeys = mBuckets.keySet();

		int limit = (mCards.size() / 2) + 1;

		if (limit == 0)
			limit = 1;
		else if (limit > 10)
			limit = 10;

		Bucket b;

		int probabilityTotal = 0;

		MyLog.d(LOG_TAG, "Total cards - " + mCards.size());

		int bucketMax = 0;

		for (int j : bucketKeys)
		{
			if (j > bucketMax)
				bucketMax = j;
		}

		bucketMax++;

		for (int j : bucketKeys)
		{
			b = mBuckets.get(j);
			// MyLog.d(LOG_TAG, "In bucket " + j + ", we have " + b.cards.size() +
			// " cards");
			b.probability = (bucketMax - j) * b.cards.size();
			// MyLog.d(LOG_TAG, "Probability - " + b.probability);

			probabilityTotal += b.probability;

			// shuffle the arraylists
			Collections.shuffle(b.cards);
		}

		MyLog.d(LOG_TAG, "Starting adding things to mToShow");

		Random random = new Random();

		while (mToShow.size() < limit)
		{
			for (int j : bucketKeys)
			{
				b = mBuckets.get(j);

				if (random.nextInt(probabilityTotal) <= b.probability)
				{
					// select that card if we haven't run out of them
					if (b.cards.size() != 0)
					{
						NextCard nc = new NextCard();
						nc.card = b.cards.get(0);
						b.cards.remove(0);
						if (j == 1 || nc.card.getType() == Card.TYPE_LEARNING_ONLY)
							nc.testing = false;
						else
							nc.testing = true;

						// MyLog.d(LOG_TAG, "Adding card " + nc.card.getId() +
						// " to mToShow");

						mToShow.add(nc);
					}
				}
			}
			i++;
		}

		MyLog.d(LOG_TAG, "Sorting finished, final i was - " + i);

	}

	@Override
	protected void saveState(Bundle bundle)
	{
		// create array list with all the things in the mToShow list

		boolean[] testing = new boolean[mToShow.size()];

		long[] cardIDs = new long[mToShow.size()];

		NextCard nc;
		for (int i = 0; i < mToShow.size(); i++)
		{
			nc = mToShow.get(i);

			testing[i] = nc.testing;
			cardIDs[i] = nc.card.getId();
		}

		bundle.putBooleanArray(BUNDLE_TESTING, testing);
		bundle.putLongArray(BUNDLE_CARD_IDS, cardIDs);

	}
}
