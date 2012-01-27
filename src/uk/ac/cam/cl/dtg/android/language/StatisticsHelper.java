package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.app.Activity;

/**
 * 
 * Class which is responsible for dealing with statistics - recording
 * exposure items, whether answer was correct or incorrect, and providing this
 * data to the learning algorithm. This class also saves collection data such
 * as your current level and how well you are doing on particular collections in
 * overall.
 * 
 * @author Vytautas
 *
 */
public class StatisticsHelper
{
	private long mCollectionID;
	
	private Activity mContext;
	
	private ArrayList<StatisticsItem> mStatistics;
	
	public StatisticsHelper(Activity context, long collectionID)
	{
		mContext = context;
		mCollectionID = collectionID;
	}
	
	/**
	 * 
	 * Loads the statistics from the database
	 * 
	 * @param orderBy statistics items will be ordered by this String, null if you want them ordered ascending by the time inserted into the database
	 */
	public void loadStatistics(String orderBy)
	{
		ApplicationDBAdapter db = new ApplicationDBAdapter(mContext);
		db.open();
		try {
		  mStatistics = db.getAllStatisticsForCollection(mCollectionID, orderBy);
		} finally {
		  db.close();
		}
	}
	
	/**
	 * 
	 * Method that returns the statistics that has been previously loaded
	 * 
	 * @return 
	 */
	public ArrayList<StatisticsItem> getStatistics()
	{
		return mStatistics;
	}
	
	/**
	 * 
	 * Inserts a record into database.
	 * 
	 * @param cardID card ID
	 * @param exposureTime time for which the card has been shown
	 * @param correct variable that tells whether the answer was correct
	 */
	public void insertStatistics(long cardID, long exposureTime, boolean correct, boolean tested)
	{
		
		ApplicationDBAdapter db = new ApplicationDBAdapter(mContext);
		db.open();
		try {
		  db.insertStatisticsItem(mCollectionID, cardID, exposureTime, correct, tested);
		} finally {
		  db.close();
		}
		
		// add this into statistics so that we don't have to reload everything
		mStatistics.add(new StatisticsItem(-1, mCollectionID, cardID, System.currentTimeMillis(), exposureTime, correct, tested));
	}
}
