package uk.ac.cam.cl.dtg.android.language;

/**
 * Class that represents a local collection.
 * 
 * @author Vytautas Vaitukaitis
 *
 */
public class Collection
{
	public static final int TYPE_PRIVATE_NON_SHARED = 0;
	public static final int TYPE_PRIVATE_SHARED_COLLECTION = 1;
	public static final int TYPE_DOWNLOADED_UNLOCKED = 2;
	public static final int TYPE_DOWNLOADED_LOCKED = 3;
	public static final int TYPE_CURRENTLY_DOWNLOADING = 4;
	public static final int TYPE_CURRENTLY_UPLOADING = 5;
	
	private long mRowID;
	private String mTitle;
	private int mRating;
	private int mType;
	private String mTags;
	private long mUpdateTime;
	private String mPathToDB;
	private String mDescription;
	private long mLastLearned;
	private long mLastUploaded;
	private long mGlobalID;
	
	public Collection(long mRowID, String mTitle, int mRating, int mType,
			String mTags, long mUpdateTime, String mPathToDB,
			String mDescription, long mLastLearned, long mLastUploaded, long mGlobalID) {
		super();
		this.mRowID = mRowID;
		this.mTitle = mTitle;
		this.mRating = mRating;
		this.mType = mType;
		this.mTags = mTags;
		this.mUpdateTime = mUpdateTime;
		this.mPathToDB = mPathToDB;
		this.mDescription = mDescription;
		this.mLastLearned = mLastLearned;
		this.mLastUploaded = mLastUploaded;
		this.mGlobalID = mGlobalID;
	}

	public String getDescription()
	{
		return mDescription;
	}

	public void setDescription(String description) {
		this.mDescription = description;
	}

	public long getRowID()
	{
		return mRowID;
	}
	
	public String getTitle()
	{
		return mTitle;
	}
	
	public void setTitle(String mTitle)
	{
		this.mTitle = mTitle;
	}
	
	public int getRating()
	{
		return mRating;
	}
	
	public void setRating(int mRating)
	{
		this.mRating = mRating;
	}
	
	public int getType()
	{
		return mType;
	}
	
	public void setType(int mType)
	{
		this.mType = mType;
	}
	
	public String getTags()
	{
		return mTags;
	}
	
	public void setTags(String mTags)
	{
		this.mTags = mTags;
	}
	
	public long getUpdateTime()
	{
		return mUpdateTime;
	}
	
	public void setUpdateTime(long mUpdateTime)
	{
		this.mUpdateTime = mUpdateTime;
	}
		
	public String getPathToDB()
	{
		return mPathToDB;
	}
	
	public void setPathToDB(String mPathToDB)
	{
		this.mPathToDB = mPathToDB;
	}
	
	public long getLastLearned()
	{
		return mLastLearned;
	}
	
	public long getLastUploaded()
	{
		return mLastUploaded;
	}
	
	public long getGlobalID()
	{
		return mGlobalID;
	}
}
