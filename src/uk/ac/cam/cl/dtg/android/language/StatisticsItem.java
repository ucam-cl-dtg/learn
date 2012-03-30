package uk.ac.cam.cl.dtg.android.language;

/**
 * Class that represents a statistics record.
 * 
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class StatisticsItem
{
	private long rowID;
	private long collectionID;
	private long cardID;
	private long timestamp;
	private long exposureTime;
	private boolean correct;
	private boolean tested;

	public StatisticsItem(long rowID, long collectionID, long cardID, long timestamp,
			long exposureTime, boolean correct, boolean tested)
	{
		super();
		this.rowID = rowID;
		this.collectionID = collectionID;
		this.cardID = cardID;
		this.timestamp = timestamp;
		this.exposureTime = exposureTime;
		this.correct = correct;
		this.tested = tested;
	}

	public long getRowID()
	{
		return rowID;
	}

	public long getCollectionID()
	{
		return collectionID;
	}

	public long getCardID()
	{
		return cardID;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public long getExposureTime()
	{
		return exposureTime;
	}

	public boolean isCorrect()
	{
		return correct;
	}

	public boolean wasInTestingMode()
	{
		return tested;
	}
}
