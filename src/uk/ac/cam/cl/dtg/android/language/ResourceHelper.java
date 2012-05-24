package uk.ac.cam.cl.dtg.android.language;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;

/**
 * 
 * Class for dealing with multimedia resources.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ResourceHelper
{
	private static final String LOG_TAG = "ResourceHelper";

	private Activity mContext;

	private MediaScannerConnection mConnection;

	public ResourceHelper(Activity context)
	{
		mContext = context;
	}

	/**
	 * 
	 * Copies the resource to the right place, adds it to the database and
	 * returns resourceID.
	 * 
	 * 
	 * @param source
	 *            source of the resource
	 * @param collectionID
	 *            local collection ID
	 * @return resource ID in the database
	 */
	public long addResource(Uri source, long collectionID, boolean insertToContentResolver)
	{
		// analyse if the resource is at the same folder already

		String path = getPath(mContext, source);

		String destinationFolder = ApplicationInitializer.COLLECTIONS_FOLDER + collectionID + "/";

		try {
		if (path.equals(destinationFolder))
		{
			L.d(LOG_TAG, "The resource is already at the same folder - no need to copy it again");

			// it's already at the folder we need
			long resourceID = resolveIntoResourceID(mContext, source);

			CardDBAdapter db = new CardDBAdapter();
			db.open(collectionID);
			try {
			  Resource res = db.getResource(resourceID);

			  int referenceCount = res.getReferenceCount();

			  referenceCount++;

			  L.d(LOG_TAG, "Updating resource count to " + referenceCount
			      + " for the resource with ID " + resourceID);

			  db.updateResourceReferenceCount(resourceID, referenceCount);
			} finally {
			  db.close();
			}

			return resourceID;
		} 
		} catch (ResourceNotFoundException e){
		  L.w(LOG_TAG, e.getMessage());
		}
		// Else or if exception
		try
		{

		  InputStream inputSource = null;
		  if (source.getScheme().equals("file"))
		  {
		    inputSource = new FileInputStream(source.getPath());
		  } else
		  {
		    inputSource = mContext.getContentResolver().openInputStream(source);
		  }

		  String suffix = getSuffix(mContext, source);

		  L.d(LOG_TAG, "Suffix returned was ." + suffix);

		  CardDBAdapter db = new CardDBAdapter();
		  db.open(collectionID);
		  long id;
		  try {
		    id = db.insertResource(suffix);
		  } finally {
		    db.close();
		  }

		  // construct a file for the given collection ID and resource ID

		  final String destination = ApplicationInitializer.COLLECTIONS_FOLDER + collectionID
		      + "/" + id + "." + suffix;

		  FileOutputStream outputStream = new FileOutputStream(destination);

		  byte[] buf = new byte[1024];

		  int numRead = 0;
		  while ((numRead = inputSource.read(buf)) >= 0)
		  {
		    outputStream.write(buf, 0, numRead);
		  }
		  outputStream.close();

		  if (insertToContentResolver)
		  {

		    mConnection = new MediaScannerConnection(mContext, new MediaScannerConnection.MediaScannerConnectionClient()
		    {
		      @Override
          public void onMediaScannerConnected()
		      {
		        mConnection.scanFile(destination, null /* mimeType */);
		      }

		      @Override
          public void onScanCompleted(String path, Uri uri)
		      {
		        L.d(LOG_TAG, "Media scanner completed scan, path is - " + path
		            + ", Uri is - " + uri.toString());

		        mConnection.disconnect();
		      }
		    });

		    mConnection.connect();
		  }

		  return id;
		} catch (Exception e)
		{
		  L.e(LOG_TAG, "Exception caught while copying file - " + e.getMessage());
		  e.printStackTrace();
		  return -1;
		}

	}

	/**
	 * 
	 * Deletes the file with given Uri.
	 * 
	 * @param resource
	 *            source of the file to be deleted
	 * @return
	 */
	public boolean deleteUri(Uri resource)
	{
		if (resource.getScheme().equals("file"))
		{
			File file = new File(resource.getPath());
			return file.delete();
		} else
			return mContext.getContentResolver().delete(resource, null, null) > 0;
	}

	/**
	 * 
	 * Method for deleting the resource file with given ID.
	 * 
	 * @param collectionID
	 *            collection ID
	 * @param resourceID
	 *            resource ID
	 * @return true if the resource file was deleted successfully, false
	 *         otherwise
	 */
	public boolean deleteResourceFile(long collectionID, long resourceID)
	{
	  try {
	    CardDBAdapter db = new CardDBAdapter();
	    db.open(collectionID);
	    String suffix;
	    try {
	      Resource res = db.getResource(resourceID);

	      suffix = res.getSuffix();
	    } finally {
	      db.close();
	    }

	    File file = new File(ApplicationInitializer.COLLECTIONS_FOLDER + collectionID + "/"
	        + resourceID + "." + suffix);

	    return file.delete();
	  } catch (ResourceNotFoundException e){
	    L.w(LOG_TAG, e.getMessage());
	    return false;
	  }
	}

	/**
	 * 
	 * Method to get suffix given the Uri of the multimedia resource.
	 * 
	 * @param activity
	 *            context
	 * @param uri
	 *            Uri of the resource you want to get suffix for
	 * @return suffix of the file
	 */
	public static String getSuffix(Activity activity, Uri uri)
	{
		if (!uri.getScheme().equals("file"))
		{
			Cursor c = activity.managedQuery(uri, null, null, null, null);
			if (c.moveToFirst())
			{
				String value = c.getString(c.getColumnIndex("_display_name"));
				String[] splits = value.split("\\.");
				c.close();

				return splits[splits.length - 1];
			} else
			{
				L.e(LOG_TAG, "Could not decode the suffix for the resource");
				return "";
			}
		} else
		{
			L.d(LOG_TAG, "Last segment of the path is " + uri.getLastPathSegment());
			String[] splits = uri.getLastPathSegment().split("\\.");
			return splits[splits.length - 1];
		}
	}

	public static String getPath(Activity activity, Uri uri)
	{
		if (!uri.getScheme().equals("file"))
		{
			Cursor c = activity.managedQuery(uri, null, null, null, null);
			if (c.moveToFirst())
			{
				String value = c.getString(c.getColumnIndex("_data"));
				String[] splits = value.split("\\/");
				c.close();

				String result = "";

				for (int i = 0; i < splits.length - 1; i++)
					result = result.concat(splits[i] + "/");

				L.d(LOG_TAG, "getPath() result is - " + result);

				return result;
			} else
			{
				L.e(LOG_TAG, "Could not decode the path for the resource");
				return "";
			}
		} else
		{
			L.d(LOG_TAG, "Last segment of the path is " + uri.getPath());
			String[] splits = uri.getPath().split("\\/");

			String result = "";

			for (int i = 0; i < splits.length - 1; i++)
				result = result.concat(splits[i] + "/");

			L.d(LOG_TAG, "getPath() result is - " + result);

			return result;
		}
	}

	public static long resolveIntoResourceID(Activity activity, Uri uri)
	{
		Cursor c = activity.managedQuery(uri, null, null, null, null);
		if (c.moveToFirst())
		{
			String value = c.getString(c.getColumnIndex("_display_name"));
			String[] splits = value.split("\\.");

			c.close();

			return Long.parseLong(splits[0]);
		} else
			return -1;
	}

	public void updateResourcesBeforeDeleting(long collectionID, long cardID)
	{
		// parse the card and pick out all of the resourceIDs
		try
		{
			// let the parser fill in the ones
			// initiate factory and parser
			SAXParserFactory parsingFactory = SAXParserFactory.newInstance();
			SAXParser parser = parsingFactory.newSAXParser();

			// get the reader
			XMLReader reader = parser.getXMLReader();

			// create a new content handler and pass it onto the reader
			XMLDeleter xmlHandler = new XMLDeleter(collectionID);
			reader.setContentHandler(xmlHandler);

			CardDBAdapter db = new CardDBAdapter();
			db.open(collectionID);
			Card card;
			try {
			  card = db.getCardById(cardID);
			} finally {
			  db.close();
			}

			L.d(LOG_TAG, "Ready to read card, XML of it is - " + card.getXmlDescription());

			StringReader stringReader = new StringReader(card.getXmlDescription());

			InputSource iSource = new InputSource(stringReader);

			reader.parse(iSource);
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Error while parsing XML - " + e.getMessage()
					+ ". Printing stack trace:");

			e.printStackTrace();
		}

	}

	public void reduceReferenceCount(long collectionID, long resourceID)
	{
		CardDBAdapter db = new CardDBAdapter();
		db.open(collectionID);
		try {
		  Resource res = db.getResource(resourceID);

		  int referenceCount = res.getReferenceCount();

		  if (referenceCount > 1)
		  {
		    L.d(LOG_TAG, "Reducing reference count for resource " + resourceID + " to "
		        + (referenceCount - 1));
		    db.updateResourceReferenceCount(resourceID, referenceCount - 1);
		  } else
		  {
		    L.d(LOG_TAG, "Deleting the resource " + resourceID);
		    this.deleteResourceFile(collectionID, resourceID);

		    db.deleteResource(resourceID);

		  }
		} catch (ResourceNotFoundException e) {
		  L.w(LOG_TAG, e.getMessage());
		} finally {
		  db.close();
		}

	}

	/**
	 * Inner class for parsing the card before deletion
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	class XMLDeleter extends DefaultHandler
	{
		private long mCollectionID;

		public XMLDeleter(long collectionID)
		{
			mCollectionID = collectionID;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{}

		@Override
		public void endDocument() throws SAXException
		{}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{}

		@Override
		public void startDocument() throws SAXException
		{}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException
		{
			L.d(LOG_TAG, "startElement() called");
			if (localName.equals(XMLStrings.XML_ELEMENT))
			{
				String elType = attributes.getValue(XMLStrings.XML_TYPE);

				if (elType.equals(XMLStrings.XML_TYPE_AUDIO)
						|| elType.equals(XMLStrings.XML_TYPE_VIDEO)
						|| elType.equals(XMLStrings.XML_TYPE_IMAGE))
				{
					reduceReferenceCount(mCollectionID,
							Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)));
				} else if (elType.equals(XMLStrings.XML_TYPE_ANSWER)
						&& attributes.getValue(XMLStrings.XML_ANSWER_TYPE).equals(
								XMLStrings.XML_ANSWER_TYPE_MC_IMAGE))
				{
					reduceReferenceCount(mCollectionID,
							Long.parseLong(attributes.getValue(XMLStrings.XML_OPTION_1)));
					reduceReferenceCount(mCollectionID,
							Long.parseLong(attributes.getValue(XMLStrings.XML_OPTION_2)));
					reduceReferenceCount(mCollectionID,
							Long.parseLong(attributes.getValue(XMLStrings.XML_OPTION_3)));
					reduceReferenceCount(mCollectionID,
							Long.parseLong(attributes.getValue(XMLStrings.XML_OPTION_4)));
				}
			}
		}
	}

	/**
	 * 
	 * Method to recursively delete directory.
	 * 
	 * @param dir
	 *            directory to be deleted
	 * @return true if directory was deleted succesfully, false otherwise
	 */
	public static boolean deleteDir(File dir)
	{
		if (dir.isDirectory())
		{
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++)
			{
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success)
				{
					return false;
				}
			}
		}
		return dir.delete();
	}

	/**
	 * Method to initialize collection folder - deletes everything if folder is
	 * not empty.
	 * 
	 * @param collectionID
	 *            ID of the collection the folder should be prepared for.
	 * @return true if folder was initialised successfully, false otherwise.
	 */
	public static boolean initializeCollectionFolder(long collectionID)
	{
		File f = new File(ApplicationInitializer.COLLECTIONS_FOLDER + collectionID + "/");
		if (f.exists())
			deleteDir(f);
		return f.mkdir();
	}

}
