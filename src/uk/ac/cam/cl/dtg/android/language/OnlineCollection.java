package uk.ac.cam.cl.dtg.android.language;

/**
 * Class that represents an online collection. Used by {@link DownloadActivity}
 * and {@link OnlineCollectionViewer}
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class OnlineCollection
{
	private long globalID;
	private String title;
	private String description;
	private String authorName;
	private int rating;
	private int downloadCount;
	private float downloadSize;
	private long dateUploaded;
	private int ratingCount;

	public OnlineCollection(long globalID, String title, String description, String authorName,
			int rating, int downloadCount, float downloadSize, long dateUploaded, int ratingCount)
	{
		super();
		this.globalID = globalID;
		this.title = title;
		this.description = description;
		this.authorName = authorName;
		this.rating = rating;
		this.downloadCount = downloadCount;
		this.downloadSize = downloadSize;
		this.dateUploaded = dateUploaded;
		this.ratingCount = ratingCount;
	}

	public long getGlobalID()
	{
		return globalID;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public String getAuthorName()
	{
		return authorName;
	}

	public int getRating()
	{
		return rating;
	}

	public int getDownloadCount()
	{
		return downloadCount;
	}

	public float getDownloadSize()
	{
		return downloadSize;
	}

	public long getDateUploaded()
	{
		return dateUploaded;
	}

	public int getRatingCount()
	{
		return ratingCount;
	}

}
