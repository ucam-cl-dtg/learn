package uk.ac.cam.cl.dtg.android.language;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 
 * Class for dealing with the application-wide database. This is the database
 * that has collections (cards themselves are stored elsewhere) and statistics
 * tables.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ApplicationDBAdapter
{
	// Process tag
	private static final String LOG_TAG = "LearningDBAdapter";

	/*
	 * Keys follow KEY_table_key Tables have DATABASE_TABLE_tablecode = Table
	 * creation SQL code follows DATABASE_CREATE_table
	 */
	public static final String KEY_COLLECTIONS_ROWID = "_id";
	public static final String KEY_COLLECTIONS_TITLE = "title";
	public static final String KEY_COLLECTIONS_AUTHORID = "author_id";
	public static final String KEY_COLLECTIONS_TYPE = "type";
	public static final String KEY_COLLECTIONS_TAGS = "tags";
	public static final String KEY_COLLECTIONS_RATING = "rating";
	public static final String KEY_COLLECTIONS_UPDATETIME = "update_time";
	public static final String KEY_COLLECTIONS_PATHTODB = "path_to_db";
	public static final String KEY_COLLECTIONS_DESCRIPTION = "description";
	public static final String KEY_COLLECTIONS_LAST_LEARNED = "last_learned";
	public static final String KEY_COLLECTIONS_GLOBAL_ID = "global_id";
	public static final String KEY_COLLECTIONS_UPLOAD_TIME = "last_upload_time";
	
	private static final String[] COLLECTIONS_COLUMNS = new String[]
	    { KEY_COLLECTIONS_ROWID, KEY_COLLECTIONS_TITLE, KEY_COLLECTIONS_AUTHORID,
    KEY_COLLECTIONS_TYPE, KEY_COLLECTIONS_TAGS, KEY_COLLECTIONS_RATING,
    KEY_COLLECTIONS_UPDATETIME, KEY_COLLECTIONS_PATHTODB, KEY_COLLECTIONS_DESCRIPTION,
    KEY_COLLECTIONS_LAST_LEARNED, KEY_COLLECTIONS_UPLOAD_TIME,
    KEY_COLLECTIONS_GLOBAL_ID };
	
	public static final String KEY_STATISTICS_ROWID = "_id";
	public static final String KEY_STATISTICS_COLLECTION_ID = "collection_id";
	public static final String KEY_STATISTICS_TESTED = "tested";
	public static final String KEY_STATISTICS_CARD_ID = "card_id";
	public static final String KEY_STATISTICS_TIME = "time";
	public static final String KEY_STATISTICS_EXPOSURE_TIME = "exposure_time";
	public static final String KEY_STATISTICS_CORRECT = "correct";

	/** CREATE SQL statement for the collections table */
	private static final String DATABASE_CREATE_COLLECTIONS = "CREATE TABLE `collections` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "`title` TEXT NOT NULL, "
			+ "`author_id` INTEGER NOT NULL, "
			+ "`type` INTEGER, "
			+ "`tags` TEXT, "
			+ "`rating` INTEGER, "
			+ "`last_learned` INTEGER, "
			+ "`update_time` INTEGER, "
			+ "`global_id` INTEGER, "
			+ "`last_upload_time` INTEGER, "
			+ "`resource_count` INTEGER, " + "`path_to_db` TEXT," + "`description` TEXT);";

	/** CREATE SQL statement for the statistics table. */
	private static final String DATABASE_CREATE_STATISTICS = "CREATE TABLE `statistics` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "`collection_id` INTEGER NOT NULL, "
			+ "`card_id` INTEGER NOT NULL, "
			+ "`tested` INTEGER NOT NULL, "
			+ "`time` INTEGER, "
			+ "`exposure_time` INTEGER, "
			+ "`correct` INTEGER);";

	private static final String DATABASE_TABLE_COLLECTIONS = "collections";
	private static final String DATABASE_TABLE_STATISTICS = "statistics";

	/**
	 * String that defines which collections are uploadable and which ones are
	 * not.
	 */
	private static final String UPLOADABLE_WHERE_STRING = KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_PRIVATE_SHARED_COLLECTION + " OR " + KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_PRIVATE_NON_SHARED + " OR " + KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_CURRENTLY_UPLOADING;

	/**
	 * String that defines which collections are treated as editable.
	 */
	private static final String EDITABLE_WHERE_STRING = KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_PRIVATE_SHARED_COLLECTION + " OR " + KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_PRIVATE_NON_SHARED + " OR " + KEY_COLLECTIONS_TYPE + "="
			+ Collection.TYPE_CURRENTLY_UPLOADING;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 9;

	private final Context mCtx;

	/**
	 * 
	 * Class that deals with creation and upgrading of the database. If massive
	 * changes are done to the database, make sure the old user data is not
	 * dropped by changing the onUpgrade() method. Currently, if database
	 * version changes, it just drops the old tables, and executes the SQL
	 * CREATE statements, thus, all of the user data is lost
	 * 
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper
	{

		DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(DATABASE_CREATE_COLLECTIONS);
			db.execSQL(DATABASE_CREATE_STATISTICS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			MyLog.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");

			// delete tables if they exist
			db.execSQL("DROP TABLE IF EXISTS `collections`");
			db.execSQL("DROP TABLE IF EXISTS `statistics`");

			onCreate(db);
		}
	}

	/**
	 * Constructor method - does not open the database connection, still need to
	 * call open() method.
	 */
	public ApplicationDBAdapter(Context ctx)
	{
		this.mCtx = ctx;
	}

	/**
	 * 
	 * Opens the database. Must be called before any of the other methods in the
	 * class.
	 * 
	 * @return the same instance of ApplicationDBAdapter
	 * @throws SQLException
	 */
	public ApplicationDBAdapter open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * 
	 * Closes the database. Must be called after the job with the database is
	 * done, to avoid leaks.
	 * 
	 */
	public void close()
	{
		mDbHelper.close();
		mDb.close();
	}

	/**
	 * 
	 * Deletes particular collection. Also, deletes the corresponding collection
	 * folder.
	 * 
	 * @param rowId
	 *            local collection ID (row ID in the collections table) of the
	 *            collection to be deleted
	 * @return whether collection was deleted from the database successfully -
	 *         the success/failure of deleting the folder is not reflected
	 */
	public boolean deleteCollection(long rowId)
	{
		// delete the DB file
		String path = ApplicationInitializer.COLLECTIONS_FOLDER + String.valueOf(rowId);

		File f = new File(path);
		boolean deleted = ResourceHelper.deleteDir(f);

		if (deleted)
			MyLog.d(LOG_TAG, "DB file for the collection was deleted successfully.");
		else
			MyLog.d(LOG_TAG,
					"DB file for the collection was not deleted successfully - it might be non-existing.");

		// delete the DB entry
		return mDb.delete(DATABASE_TABLE_COLLECTIONS, KEY_COLLECTIONS_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * 
	 * Returns all the collections in the database.
	 * 
	 * @return {@link java.util.ArrayList} containing collections (empty if
	 *         there aren't any)
	 */
	public ArrayList<Collection> getAllCollections()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_COLLECTIONS, COLLECTIONS_COLUMNS, null, null, null, null, KEY_COLLECTIONS_RATING
				+ " DESC");

		ArrayList<Collection> result = new ArrayList<Collection>();

		if (!cursor.isAfterLast())
		{
			while (!cursor.isLast())
			{
				cursor.moveToNext();

				result.add(collectionFromCursor(cursor));

			}
		}
		cursor.close();
		return result;
	}

	/**
	 * Returns all the uploadable collections as defined by the
	 * {@link #UPLOADABLE_WHERE_STRING}.
	 * 
	 * @return {@link java.util.ArrayList} of all the collections, empty if
	 *         there aren't any
	 */
	public ArrayList<Collection> getUploadableCollections()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_COLLECTIONS, COLLECTIONS_COLUMNS, UPLOADABLE_WHERE_STRING, null, null, null, null);

		ArrayList<Collection> result = new ArrayList<Collection>();

		if (!cursor.isAfterLast())
		{
			while (!cursor.isLast())
			{
				cursor.moveToNext();

				result.add(collectionFromCursor(cursor));

			}
		}
		cursor.close();
		return result;
	}

	/**
	 * 
	 * Method to get all the editable collections from the database, as defined
	 * by (@link #EDITABLE_WHERE_STRING).
	 * 
	 * @return (@link java.util.ArrayList) of collections that are editable
	 *         (empty if there aren't any editable collections)
	 */
	public ArrayList<Collection> getEditableCollections()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_COLLECTIONS, COLLECTIONS_COLUMNS, EDITABLE_WHERE_STRING, null, null, null, null);

		ArrayList<Collection> result = new ArrayList<Collection>();

		if (!cursor.isAfterLast())
		{
			while (!cursor.isLast())
			{
				cursor.moveToNext();

				result.add(collectionFromCursor(cursor));

			}
		}
		cursor.close();
		return result;
	}

	/**
	 * 
	 * Returns the collection by local ID (row ID in the collections table)
	 * 
	 * @param rowID
	 *            local collection ID (row ID in the table)
	 * @return {@link Collection} object that has the passed row ID or null if
	 *         the collection could have been found
	 */
	public Collection getCollectionById(Long rowID)
	{
		Cursor cursor = mDb.query(true, DATABASE_TABLE_COLLECTIONS, COLLECTIONS_COLUMNS, KEY_COLLECTIONS_ROWID + "=" + rowID, null, null, null,
				null, null);
		try
		{
			cursor.moveToFirst();

			Collection c = collectionFromCursor(cursor);

			cursor.close();

			return c;

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while reading cursor for getCollectionById - "
					+ e.getMessage());

			return null;
		}
	}

	/**
	 * 
	 * Returns the local collection that has the global ID as issued by the
	 * online server.
	 * 
	 * @param globalID
	 *            global collection ID that was issued by the online server
	 * @return {@link Collection} object that has the given global ID, the first
	 *         one if there is more than one, or null in case there aren't any
	 * @throws IOException if something goes wrong
	 */
	public Collection getCollectionByGlobalID(Long globalID) throws IOException
	{
	  try
    {
		Cursor cursor = mDb.query(true, DATABASE_TABLE_COLLECTIONS, COLLECTIONS_COLUMNS, KEY_COLLECTIONS_GLOBAL_ID + "=" + globalID, null,
				null, null, null, null);

			cursor.moveToFirst();

			Collection c = collectionFromCursor(cursor);

			cursor.close();

			return c;

		} catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}
	}

	private Collection collectionFromCursor(Cursor cursor){
	  return new Collection(cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_ROWID)),
        cursor.getString(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_TITLE)),
        cursor.getInt(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_RATING)),
        cursor.getInt(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_TYPE)),
        cursor.getString(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_TAGS)),
        cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_UPDATETIME)),
        cursor.getString(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_PATHTODB)),
        cursor.getString(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_DESCRIPTION)),
        cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_LAST_LEARNED)),
        cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_UPLOAD_TIME)),
        cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_COLLECTIONS_GLOBAL_ID)));
	}
	
	/**
	 * 
	 * Method to get number of uploadable collections. Again, which collections
	 * are uploadable is defined by {@link #UPLOADABLE_WHERE_STRING}.
	 * 
	 * @return the number of uploadable collections currently in the database
	 */
	public int getUploadableCount()
	{
		Cursor cursor = mDb.query(DATABASE_TABLE_COLLECTIONS, new String[]
		{}, UPLOADABLE_WHERE_STRING, null, null, null, null);

		return cursor.getCount();

	}

	/**
	 * 
	 * Updates the rating of particular collection.
	 * 
	 * @param rowId
	 *            local collection ID (row ID in the database table)
	 * @param rating
	 *            new rating of the collection (ought to be 0 to 5)
	 * @return true if collection rating was updated successfully, false
	 *         otherwise
	 */
	public boolean updateRating(long rowId, int rating)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_RATING, rating);
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "=" + rowId,
				null) > 0;
	}

	/**
	 * 
	 * Updates the date when last learnt for the particular collection.
	 * 
	 * @param rowId
	 *            local collection ID (row ID in the DB table)
	 * @return true if entry was updated successfully, false otherwise
	 */
	public boolean updateLastLearned(long rowId)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_LAST_LEARNED, System.currentTimeMillis());
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "=" + rowId,
				null) > 0;
	}

	/**
	 * 
	 * Updates the global ID of the local collection. Usually called upon
	 * uploading the collection.
	 * 
	 * @param collectionID
	 *            local collection ID
	 * @param globalID
	 *            global collection ID (as issued by the server)
	 * @return true if global ID was updated successfully, false otherwise
	 */
	public boolean updateGlobalID(long collectionID, long globalID)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_GLOBAL_ID, globalID);
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "="
				+ collectionID, null) > 0;
	}

	/**
	 * 
	 * Updates upload time of the collection to now.
	 * 
	 * @param collectionID
	 *            local collection ID
	 * @return true if upload time was updated successfully, false otherwise
	 */
	public boolean updateUploadTime(long collectionID)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_UPLOAD_TIME, System.currentTimeMillis());
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "="
				+ collectionID, null) > 0;

	}

	/**
	 * 
	 * Updates the collection type of the given collection.
	 * 
	 * @param collectionID
	 *            local collection ID
	 * @param typeID
	 *            new type, value must correspond to one of the static fields
	 *            defined in {@link Collection}
	 * @return true if the type was updated successfully, false otherwise
	 */
	public boolean updateCollectionType(long collectionID, int typeID)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_TYPE, typeID);
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "="
				+ collectionID, null) > 0;

	}

	/**
	 * Method to update title and description of existing collection.
	 * 
	 * @param collectionID
	 *            local collection ID
	 * @param title
	 *            new title
	 * @param description
	 *            new description
	 * @return true if title and description were updated successfully, false
	 *         otherwise
	 */
	public boolean updateTitleAndDescription(long collectionID, String title, String description)
	{
		ContentValues args = new ContentValues();
		args.put(KEY_COLLECTIONS_TITLE, title);
		args.put(KEY_COLLECTIONS_DESCRIPTION, description);
		return mDb.update(DATABASE_TABLE_COLLECTIONS, args, KEY_COLLECTIONS_ROWID + "="
				+ collectionID, null) > 0;

	}

	/**
	 * 
	 * Method to insert new collection into the database. It also initializes
	 * collection folder: in case folder exists, it will be erased, thus, the
	 * execution of this method might take quite a while. Therefore, it is
	 * adviseable to call this method from a non-UI thread.
	 * 
	 * @param title
	 *            title of the collection
	 * @param type
	 *            collection type as defined in the static fields of
	 *            {@link Collection}
	 * @param rating
	 *            rating of the collection (give -1 for a fresh unrated
	 *            collection)
	 * @param description
	 *            description of the collection
	 * @param globalID
	 *            global ID as issued by the online server (-1 if collection is
	 *            being created locally)
	 * @return local collection ID
	 */

	public long insertCollection(String title, int type, int rating, String description,
			long globalID)
	{
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_COLLECTIONS_TITLE, title);
		initialValues.put(KEY_COLLECTIONS_TYPE, type);
		initialValues.put(KEY_COLLECTIONS_RATING, rating);
		initialValues.put(KEY_COLLECTIONS_UPDATETIME, 0);
		initialValues.put(KEY_COLLECTIONS_DESCRIPTION, description);
		initialValues.put(KEY_COLLECTIONS_LAST_LEARNED, 0);
		initialValues.put(KEY_COLLECTIONS_UPLOAD_TIME, 0);
		initialValues.put(KEY_COLLECTIONS_GLOBAL_ID, globalID);

		// fields that are not currently used
		initialValues.put(KEY_COLLECTIONS_PATHTODB, "");
		initialValues.put(KEY_COLLECTIONS_TAGS, "");
		initialValues.put(KEY_COLLECTIONS_AUTHORID, 0);

		long id = mDb.insert(DATABASE_TABLE_COLLECTIONS, null, initialValues);

		ResourceHelper.initializeCollectionFolder(id);

		return id;
	}

	/**
	 * Method to insert statistics item into the database.
	 * 
	 * @param collectionID
	 *            local collection ID
	 * @param cardID
	 *            card ID in the collection-specific database
	 * @param exposure_time
	 *            how long the card was shown for in milliseconds
	 * @param correct
	 *            should be true if user answered to the card correctly, false
	 *            otherwise
	 * @param tested
	 *            true if the card was shown in the testing mode, false
	 *            otherwise
	 */
	public void insertStatisticsItem(long collectionID, long cardID, long exposure_time,
			boolean correct, boolean tested)
	{
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_STATISTICS_COLLECTION_ID, collectionID);
		initialValues.put(KEY_STATISTICS_CARD_ID, cardID);
		initialValues.put(KEY_STATISTICS_EXPOSURE_TIME, exposure_time);

		if (tested)
			initialValues.put(KEY_STATISTICS_TESTED, 1);
		else
			initialValues.put(KEY_STATISTICS_TESTED, 0);

		MyLog.d(LOG_TAG, "Inserting statistics item: testing - " + tested + ", correct - " + correct);

		if (correct)
			initialValues.put(KEY_STATISTICS_CORRECT, 1);
		else
			initialValues.put(KEY_STATISTICS_CORRECT, 0);

		// put in record time
		initialValues.put(KEY_STATISTICS_TIME, System.currentTimeMillis());

		mDb.insert(DATABASE_TABLE_STATISTICS, null, initialValues);
	}

	/**
	 * 
	 * Method to obtain the first 3000 statistics entries for that particular
	 * collection. Thus, it is advised to use SQL ORDER BY string to order the
	 * entries by the time in descending order (so that the latest entries show
	 * up first).
	 * 
	 * @param collectionID
	 *            local collection ID (row ID in the database)
	 * @param orderBy
	 *            SQL ORDER BY string, null if no ordering is required
	 * @return {@link java.util.ArrayList} of {@link StatisticsItem}s, empty if
	 *         no entries exist
	 */
	protected ArrayList<StatisticsItem> getAllStatisticsForCollection(long collectionID,
			String orderBy)
	{
		Cursor cursor = mDb.query(true, DATABASE_TABLE_STATISTICS, new String[]
		{ KEY_STATISTICS_ROWID, KEY_STATISTICS_COLLECTION_ID, KEY_STATISTICS_CARD_ID,
				KEY_STATISTICS_CORRECT, KEY_STATISTICS_TIME, KEY_STATISTICS_TESTED,
				KEY_STATISTICS_EXPOSURE_TIME }, KEY_STATISTICS_COLLECTION_ID + "=" + collectionID,
				null, null, null, orderBy, null);

		ArrayList<StatisticsItem> result = new ArrayList<StatisticsItem>();

		if (!cursor.isAfterLast())
		{
			boolean correct, tested;
			int correctInt, testedInt;

			int i = 0;

			while ((!cursor.isLast()) && i < 3000)
			{
				cursor.moveToNext();

				correctInt = cursor.getInt(cursor.getColumnIndex(KEY_STATISTICS_CORRECT));

				if (correctInt == 1)
					correct = true;
				else
					correct = false;

				testedInt = cursor.getInt(cursor.getColumnIndex(KEY_STATISTICS_TESTED));

				if (testedInt == 1)
					tested = true;
				else
					tested = false;

				result.add(new StatisticsItem(cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_STATISTICS_ROWID)),
				    cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_STATISTICS_COLLECTION_ID)),
				    cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_STATISTICS_CARD_ID)),
				    cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_STATISTICS_TIME)),
				    cursor.getLong(cursor.getColumnIndex(ApplicationDBAdapter.KEY_STATISTICS_EXPOSURE_TIME)), correct, tested));

				i++;

			}
		}
		cursor.close();
		return result;

	}

	/**
	 * 
	 * Clears all the statistics entries for that collection.
	 * 
	 * @param collectionID
	 *            local collection ID
	 */
	protected void clearStatisticsForCollection(long collectionID)
	{
		mDb.delete(DATABASE_TABLE_STATISTICS, KEY_STATISTICS_COLLECTION_ID + "=" + collectionID,
				null);
	}
}
