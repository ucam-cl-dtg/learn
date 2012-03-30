package uk.ac.cam.cl.dtg.android.language;

/**
 * 
 * Class that represents a single flashcard.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class Card
{
	/**
	 * Card is meant to be for learning only, i.e. it does not have any answer
	 * components
	 */
	public static final int TYPE_LEARNING_ONLY = 0;

	/**
	 * Card can be used for both testing and learning, i.e. it must have an
	 * answer component.
	 */
	public static final int TYPE_LEARNING_AND_TESTING = 1;

	/** Card ID in a collection-specific database */
	private long id;

	/** Title of a card */
	private String title;

	/** XML description of a card */
	private String xmlDescription;

	/** Author ID - not used currently. */
	private int authorID;

	/**
	 * Type of the card - one of the {@link #TYPE_LEARNING_ONLY} and
	 * {@link #TYPE_LEARNING_AND_TESTING}
	 */
	private int type;

	/** Rating of the card - 0 to 5 */
	private int rating;

	/** Time in milliseconds when the card was last updated */
	private long updateTime;

	/**
	 * <b>Not used</b> at the moment, meant to store comma-separated IDs of the
	 * prerequisite cards.
	 */
	private String prerequisites;

	/**
	 * <b>Not used</b> at the moment, meant to store how many times card has
	 * been shown. However, all of the statistics are being saved in the
	 * application-wide database.
	 * 
	 * @see StatisticsHelper
	 * @see StatisticsItem
	 */
	private int timesShown;

	/**
	 * Constructor for a card.
	 * 
	 * @param id
	 *            row ID of the card in cards table
	 * @param title
	 *            title of the card
	 * @param xmlDescription
	 *            XML description of the card
	 * @param authorID
	 *            <b>not used</b>
	 * @param type
	 *            type of the card - see {@link #type} for more info
	 * @param rating
	 *            rating of the card (0 to 5)
	 * @param updateTime
	 *            time when the card was last updated
	 * @param prerequisites
	 *            <b>not used</b>
	 * @param timesShown
	 *            <b>not used</b>
	 */
	public Card(long id, String title, String xmlDescription, int authorID, int type, int rating,
			long updateTime, String prerequisites, int timesShown)
	{
		super();

		this.id = id;
		this.title = title;
		this.xmlDescription = xmlDescription;
		this.authorID = authorID;
		this.type = type;
		this.rating = rating;
		this.updateTime = updateTime;
		this.prerequisites = prerequisites;
		this.timesShown = timesShown;
	}

	/**
	 * Method to get card row ID in the DB
	 * 
	 * @return card ID
	 */
	public long getId()
	{
		return id;
	}

	/**
	 * 
	 * Method to get title of a card
	 * 
	 * @return title of a card
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * Method to set title of a card
	 * 
	 * @param title
	 *            title of a card
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}

	/**
	 * 
	 * Method to get XML description of a card
	 * 
	 * @return XML description of a card
	 */
	public String getXmlDescription()
	{
		return xmlDescription;
	}

	/**
	 * 
	 * Method to set XML description of a card
	 * 
	 * @param xmlDescription
	 *            valid XML description of a card
	 */
	public void setXmlDescription(String xmlDescription)
	{
		this.xmlDescription = xmlDescription;
	}

	/**
	 * 
	 * Method to get author ID of a card - <b>not used</b>
	 * 
	 * @return most likely 0 or -1
	 */
	public int getAuthorID()
	{
		return authorID;
	}

	/**
	 * 
	 * Method to set author ID - <b>not used</b>
	 * 
	 * @param authorID
	 *            author ID
	 */
	public void setAuthorID(int authorID)
	{
		this.authorID = authorID;
	}

	/**
	 * Method to get card type.
	 * 
	 * @return card type - one of the {@link #TYPE_LEARNING_ONLY} and
	 *         {@link #TYPE_LEARNING_AND_TESTING}
	 * @see #type
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * 
	 * Method to set card type.
	 * 
	 * @param card
	 *            type - see {@link #type}
	 */
	public void setType(int type)
	{
		this.type = type;
	}

	/**
	 * 
	 * Method to get card rating.
	 * 
	 * @return card rating - 0 to 5
	 */
	public int getRating()
	{
		return rating;
	}

	/**
	 * 
	 * Method to set card rating.
	 * 
	 * @param rating
	 *            card rating - 0 to 5.
	 */
	public void setRating(int rating)
	{
		this.rating = rating;
	}

	/**
	 * Method to get the time in milliseconds when the card was last updated.
	 * 
	 * @return last update time
	 */
	public long getUpdateTime()
	{
		return updateTime;
	}

	/**
	 * Method to set last update time.
	 * 
	 * @param updateTime last update time in milliseconds
	 */
	public void setUpdateTime(long updateTime)
	{
		this.updateTime = updateTime;
	}
	
	/**
	 * Method to get prerequisites of a card - currently <b>not in use</b>.
	 * 
	 * @return
	 */
	public String getPrerequisites()
	{
		return prerequisites;
	}

	/**
	 * 
	 * Method to set prerequisites of a card - <b>not used</b>.
	 * 
	 * @param prerequisites comma-separated IDs of prerequisite cards.
	 */
	public void setPrerequisites(String prerequisites)
	{
		this.prerequisites = prerequisites;
	}
	
	/**
	 * Method to get how many times a card was shown - <b>not used</b>, thus would return 0.
	 * 
	 * @return how many times card was shown - <b>not used</b>
	 */
	public int getTimesShown()
	{
		return timesShown;
	}
	
	/**
	 * Method to set how many times a card was shown - <b>not used</b>
	 * 
	 * @param timesShown how many times a card was shown
	 */
	public void setTimesShown(int timesShown)
	{
		this.timesShown = timesShown;
	}

}
