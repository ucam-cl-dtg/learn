package uk.ac.cam.cl.dtg.android.language;

/**
 * 
 * Class that represents a multimedia resource for some particular collection.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class Resource
{
	private long mId;
	private String mSuffix;
	private int mReferenceCount;

	public Resource(long mId, String mSuffix, int mReferenceCount)
	{
		super();
		this.mId = mId;
		this.mSuffix = mSuffix;
		this.mReferenceCount = mReferenceCount;
	}

	public String getSuffix()
	{
		return mSuffix;
	}

	public void setSuffix(String suffix)
	{
		this.mSuffix = suffix;
	}

	public int getReferenceCount()
	{
		return mReferenceCount;
	}

	public void setReferenceCount(int mReferenceCount)
	{
		this.mReferenceCount = mReferenceCount;
	}

	public long getId()
	{
		return mId;
	}

	public String getFileName()
	{
		return String.valueOf(mId) + "." + mSuffix;
	}

}
