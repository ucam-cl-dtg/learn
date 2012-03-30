package uk.ac.cam.cl.dtg.android.language;

import android.os.Bundle;

/**
 * 
 * Abstract class that represents a learnign algorithm.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public abstract class LearningAlgorithm
{

	/**
	 * These codes must be the same as in the array with learning algorithm
	 * titles and codes
	 */
	public static final int SIMPLE_ALGORITHM = 0, ITERATING_ALGORITHM = 1, LEITNER_ALGORITHM = 2,
			NEGATIVE_LEITNER_ALGORITHM = 3;

	/**
	 * Method which defines and returns the number of the card to be shown next.
	 * 
	 * @return String code of the card to be shown next
	 */
	protected abstract NextCard getNext();

	/**
	 * 
	 * Saves state into the bundle
	 * 
	 * @param bundle
	 */
	protected abstract void saveState(Bundle bundle);
}
