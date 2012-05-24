package uk.ac.cam.cl.dtg.android.language;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * 
 * Class for dealing with collection-specific database which is saved in
 * collections folder on SD card.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CardDBAdapter
{
	// Process tag
	private static final String LOG_TAG = "CardDBAdapter";

	/*
	 * Keys follow KEY_table_key Tables have DATABASE_TABLE_tablecode = Table
	 * creation SQL code follows DATABASE_CREATE_table
	 */
	public static final String KEY_CARDS_ROWID = "_id";
	public static final String KEY_CARDS_TITLE = "title";
	public static final String KEY_CARDS_AUTHORID = "author_id";
	public static final String KEY_CARDS_TYPE = "type";
	public static final String KEY_CARDS_RATING = "rating";
	public static final String KEY_CARDS_UPDATETIME = "update_time";
	public static final String KEY_CARDS_CONTENT = "contents";
	public static final String KEY_CARDS_PREREQUISITES = "prerequisites";
	public static final String KEY_CARDS_TIMESSHOWN = "times_shown";

	public static final String KEY_RESOURCES_ROWID = "_id";
	public static final String KEY_RESOURCES_REFERENCE_COUNT = "reference_count";
	public static final String KEY_RESOURCES_SUFFIX = "suffix";

	private static final String DATABASE_CREATE_CARDS = "CREATE TABLE IF NOT EXISTS `cards` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "`title` TEXT NOT NULL, "
			+ "`author_id` INTEGER NOT NULL, "
			+ "`type` INTEGER, "
			+ "`rating` INTEGER, "
			+ "`update_time` INTEGER, "
			+ "`contents` TEXT,"
			+ "`prerequisites` TEXT," + "`times_shown` INTEGER);";

	private static final String DATABASE_CREATE_RESOURCES = "CREATE TABLE IF NOT EXISTS `resources` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "`suffix` TEXT, " + "`reference_count` INTEGER);";

	private static final String DATABASE_TABLE_CARDS = "cards";
	private static final String DATABASE_TABLE_RESOURCES = "resources";

	private SQLiteDatabase mDb;

	/**
	 * 
	 * Opens up a collection-specific database.
	 * 
	 * @param collectionID
	 *            local collection ID for which the DB should be opened
	 * @return true if DB was opened or created successfully, false otherwise
	 * @throws SQLException
	 */
	public CardDBAdapter open(long collectionID)
	{

		String path = ApplicationInitializer.COLLECTIONS_FOLDER + collectionID;
		L.d(LOG_TAG, "Opening database at the path " + path);

		File databaseFolder = new File(path);
		if (!databaseFolder.exists())
		{
			L.d(LOG_TAG, "Database folder does not exist - creating a new one");
			databaseFolder.mkdirs();
		}

		File databaseFile = new File(databaseFolder.getAbsolutePath() + "/data.db");

		mDb = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);

		// create tables if they do not exist
		mDb.execSQL(DATABASE_CREATE_CARDS);
		mDb.execSQL(DATABASE_CREATE_RESOURCES);

		return this;
	}

	/**
	 * Closes down the database connection.
	 */
	public void close()
	{
		if (mDb != null)
			mDb.close();
	}

	/**
	 * Deletes card with a given card ID
	 * 
	 * @param rowId
	 *            card ID
	 * @return true if the card was deleted successfully, false otherwise
	 */
	public boolean deleteCard(long rowId)
	{
		return mDb.delete(DATABASE_TABLE_CARDS, KEY_CARDS_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return all the cards in the order they were added to the table.
	 * 
	 * @return {@link ArrayList} of cards, which is empty in case there aren't
	 *         any cards
	 */
	public ArrayList<Card> getAllCards()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_CARDS, new String[]
		{ KEY_CARDS_ROWID, KEY_CARDS_TITLE, KEY_CARDS_AUTHORID, KEY_CARDS_TYPE, KEY_CARDS_RATING,
				KEY_CARDS_UPDATETIME, KEY_CARDS_CONTENT, KEY_CARDS_PREREQUISITES,
				KEY_CARDS_TIMESSHOWN }, null, null, null, null, null);

		ArrayList<Card> result = new ArrayList<Card>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			result.add(new Card(cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_ROWID)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TITLE)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_CONTENT)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_AUTHORID)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TYPE)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_RATING)), cursor.getLong(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_UPDATETIME)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_PREREQUISITES)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TIMESSHOWN))));
			cursor.moveToNext();
		}
		cursor.close();

		return result;
	}

	/**
	 * 
	 * Returns a {@link Card} with a given ID.
	 * 
	 * @param rowID
	 *            card ID
	 * @return {@link Card} with a given ID, null if it was not found
	 */
	public Card getCardById(Long rowID)
	{

		Cursor cursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[]
		{ KEY_CARDS_TITLE, KEY_CARDS_AUTHORID, KEY_CARDS_TYPE, KEY_CARDS_RATING,
				KEY_CARDS_UPDATETIME, KEY_CARDS_CONTENT, KEY_CARDS_PREREQUISITES,
				KEY_CARDS_TIMESSHOWN }, KEY_CARDS_ROWID + "=" + rowID, null, null, null, null, null);

		Card card = null;

		if (!cursor.isAfterLast())
		{
			cursor.moveToFirst();
			card = new Card(rowID.intValue(), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TITLE)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_CONTENT)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_AUTHORID)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TYPE)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_RATING)), cursor.getLong(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_UPDATETIME)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_PREREQUISITES)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_CARDS_TIMESSHOWN)));
		}

		cursor.close();

		return card;
	}

	/**
	 * 
	 * Returns card count in the collection.
	 * 
	 * @return number of cards in a collection
	 */
	public int getCardCount()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_CARDS, new String[]
		{}, null, null, null, null, null);
		int count = cursor.getCount();

		cursor.close();

		return count;

	}

	/**
	 * Method to update rating of a card.
	 * 
	 * @param rowId
	 *            card ID
	 * @param rating
	 *            new rating of a card (0 to 5)
	 * @return true if a card was updated successfully, false otherwise
	 */
	public boolean updateRating(long rowId, int rating)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_CARDS_RATING, rating);
		return mDb.update(DATABASE_TABLE_CARDS, args, KEY_CARDS_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * 
	 * Method to update XML description of a card.
	 * 
	 * @param rowId
	 * @param content
	 * @param type
	 *            new {@link Card} type, see {@link Card.type}
	 * @return true if card was updated successfully, false otherwise
	 */
	public boolean updateContent(long rowId, String content, int type)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_CARDS_CONTENT, content);
		args.put(KEY_CARDS_UPDATETIME, System.currentTimeMillis());
		args.put(KEY_CARDS_TYPE, type);
		return mDb.update(DATABASE_TABLE_CARDS, args, KEY_CARDS_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * 
	 * Method to update card title
	 * 
	 * @param rowId
	 *            card ID
	 * @param title
	 *            new title of a card
	 * @return true if a card was updated successfully, false otherwise
	 */
	public boolean updateTitle(long rowId, String title)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_CARDS_TITLE, title);
		return mDb.update(DATABASE_TABLE_CARDS, args, KEY_CARDS_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * 
	 * Method to insert a resource into a database.
	 * 
	 * @param suffix
	 *            file suffix of the resource file.
	 * @return resource ID
	 */
	public long insertResource(String suffix)
	{
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_RESOURCES_REFERENCE_COUNT, 1);
		initialValues.put(KEY_RESOURCES_SUFFIX, suffix);

		return mDb.insert(DATABASE_TABLE_RESOURCES, null, initialValues);
	}

	/**
	 * 
	 * Method to insert a card into a collection
	 * 
	 * @param title
	 *            card title
	 * @param authorid
	 *            author ID - <b>not used</b>
	 * @param type
	 *            card type, see {@link Card.type} for more information
	 * @param content
	 *            XML contents of a card
	 * @param rating
	 *            card rating
	 * @param prerequisites
	 *            prerequisite cards - <b>not used</b>
	 * @return
	 */
	public long insertCard(String title, int authorid, int type, String content, int rating,
			String prerequisites)
	{
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_CARDS_TITLE, title);
		initialValues.put(KEY_CARDS_AUTHORID, authorid);
		initialValues.put(KEY_CARDS_TYPE, type);
		initialValues.put(KEY_CARDS_CONTENT, content);
		initialValues.put(KEY_CARDS_RATING, rating);

		initialValues.put(KEY_CARDS_UPDATETIME, System.currentTimeMillis());

		initialValues.put(KEY_CARDS_PREREQUISITES, prerequisites);

		initialValues.put(KEY_CARDS_TIMESSHOWN, 0);

		return mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
	}

	/**
	 * 
	 * Method to update resource reference count in a collection-specific
	 * database.
	 * 
	 * @param resourceID
	 *            resource ID
	 * @param referenceCount
	 *            new reference count
	 * @return true if resource was updated successfully, false otherwise
	 */
	public boolean updateResourceReferenceCount(long resourceID, int referenceCount)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_RESOURCES_REFERENCE_COUNT, referenceCount);
		return mDb.update(DATABASE_TABLE_RESOURCES, args, KEY_RESOURCES_ROWID + "=" + resourceID,
				null) > 0;
	}

	/**
	 * 
	 * Method to get {@link Resource} with specific resource ID.
	 * 
	 * @param resourceID
	 *            resource ID
	 * @return resource with the specific ID, null if resource could not be
	 *         found
	 * @throws ResourceNotFoundException 
	 */
	public Resource getResource(long resourceID) throws ResourceNotFoundException
	{//TODO(drt24) this can throw runtime exceptions and fail.
	  try {
	  Cursor cursor = mDb.query(true, DATABASE_TABLE_RESOURCES, new String[]
	      { KEY_RESOURCES_ROWID, KEY_RESOURCES_SUFFIX, KEY_RESOURCES_REFERENCE_COUNT },
	      KEY_RESOURCES_ROWID + "=" + resourceID, null, null, null, null, null);

	  Resource resource = null;

	  if (!cursor.isAfterLast())
	  {
	    cursor.moveToFirst();
	    resource = new Resource(resourceID, cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_RESOURCES_SUFFIX)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_RESOURCES_REFERENCE_COUNT)));
	  }

	  cursor.close();
	  if (resource == null){
	    throw new ResourceNotFoundException("Could not get resource with id: " + resourceID);
	  }

	  return resource;
	  } catch (Exception e){
	    throw new ResourceNotFoundException(e);
	  }
	}

	/**
	 * 
	 * Method to delete resource from a table
	 * 
	 * @param resourceID
	 *            resource ID
	 * @return true if resource was removed successfully, false otherwise
	 */
	public boolean deleteResource(long resourceID)
	{
		return mDb.delete(DATABASE_TABLE_RESOURCES, KEY_RESOURCES_ROWID + "=" + resourceID, null) > 0;
	}

	public ArrayList<Resource> getAllResources()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_RESOURCES, new String[]
		{KEY_RESOURCES_REFERENCE_COUNT, KEY_RESOURCES_SUFFIX, KEY_RESOURCES_ROWID}, null, null, null, null, null);

		ArrayList<Resource> result = new ArrayList<Resource>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Resource res = new Resource(cursor.getLong(cursor.getColumnIndex(KEY_RESOURCES_ROWID)), cursor.getString(cursor.getColumnIndex(CardDBAdapter.KEY_RESOURCES_SUFFIX)), cursor.getInt(cursor.getColumnIndex(CardDBAdapter.KEY_RESOURCES_REFERENCE_COUNT)));
			
			result.add(res);
			cursor.moveToNext();
		}
		cursor.close();

		return result;

	}
	
	public boolean updateSuffix(long resourceID, String newSuffix)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_RESOURCES_SUFFIX, newSuffix);
		return mDb.update(DATABASE_TABLE_RESOURCES, args, KEY_RESOURCES_ROWID + "=" + resourceID,
				null) > 0;
	}

}
