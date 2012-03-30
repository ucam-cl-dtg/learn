package uk.ac.cam.cl.dtg.android.language;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 
 * Class that deals with communicating with the online sharing server.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ServerHelper
{
	private static final String LOG_TAG = "ServerHelper";

	private static final String URL_SERVER = "http://www.cl.cam.ac.uk/research/dtg/language/";

	private static final String URL_RATE = URL_SERVER + "rate_collection.php";
	private static final String URL_UNSHARE = URL_SERVER + "unshare_collection.php";
	private static final String URL_UPDATE = URL_SERVER + "update_collection.php";
	private static final String URL_UPLOAD = URL_SERVER + "upload_collection.php";
	private static final String URL_DOWNLOAD = URL_SERVER + "download_collection.php";
	private static final String URL_CHECK_UPDATES = URL_SERVER + "check_for_updates.php";
	public static final String URL_HELP_PAGE = URL_SERVER + "help.html";
	public static final String URL_REGISTER = URL_SERVER + "register_user.php";
	public static final String URL_LOGIN = URL_SERVER + "check_user.php";
	public static final String URL_REPORT_ERROR = URL_SERVER + "report_error.php";
	public static final String URL_GET_COLLECTIONS = URL_SERVER + "get_collections.php";
	public static final String URL_REGAIN_RIGHTS = URL_SERVER + "regain_rights.php";
	private static final int ATTEMPT_LIMIT = 10;

	protected static InputStream obtainInputStream(String urlAddress, MultipartEntity form)
			throws ClientProtocolException, IOException
	{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpUriRequest request = new HttpPost(urlAddress);
		client.getParams().setBooleanParameter("http.protocol.expect-continue", false);

		// set the form as an entity of the request
		((HttpEntityEnclosingRequestBase) request).setEntity(form);

		// execute the request
		HttpResponse response = client.execute(request);

		// get the response input stream
		return response.getEntity().getContent();
	}

	protected static String getResponseString(String urlAddress, MultipartEntity form)
			throws ClientProtocolException, IOException
	{
		InputStream in = obtainInputStream(urlAddress, form);

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder str = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			str.append(line);
		}
		in.close();
		return str.toString();
	}

	public static void updateRating(Context mContext, long globalID, int rating)
	{
		try
		{
			// setting up the Apache HTTP client
			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			String uniqueID = preferences.getString(
					mContext.getString(R.string.preferences_unique_id), "");

			form.addPart("unique_id", new StringBody(uniqueID));
			form.addPart("rating", new StringBody(String.valueOf(rating)));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));

			String responseString = getResponseString(URL_RATE, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while rating collection - " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void unshareCollection(Context context, long globalID)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String email = preferences.getString(
					context.getString(R.string.preferences_code_email_address), "");

			String password = preferences.getString(
					context.getString(R.string.preferences_code_password), "");

			form.addPart("email_address", new StringBody(email));
			form.addPart("password", new StringBody(password));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));

			String responseString = getResponseString(URL_UNSHARE, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while rating collection - " + e.getMessage());
			e.printStackTrace();

			try
			{
				Thread.sleep(5000);
				unshareCollection(context, globalID);
			} catch (Exception e1)
			{
				return;
			}

		}
	}

	/**
	 * 
	 * Zips all the collection files into one zip file
	 * 
	 * @param collectionID
	 *            collection ID
	 * @param tempFileName
	 *            temporary file name that will be created under the collections
	 *            folder
	 * @param collectionType
	 *            collection type which would be returned to in case of
	 *            unsuccessful zipping
	 * @return whether zipping went successfully or not
	 */
	public static boolean zipCollection(Context mContext, long collectionID, String tempFileName,
			int collectionType)
	{
		try
		{
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(ApplicationInitializer.COLLECTIONS_FOLDER
					+ tempFileName)));

			// directory to zip from
			File zipDir = new File(ApplicationInitializer.COLLECTIONS_FOLDER + collectionID);

			// get a list of the directory content
			String[] dirList = zipDir.list();
			byte[] readBuffer = new byte[2156];
			int bytesIn = 0;

			// loop through dirList, and zip the files
			for (int i = 0; i < dirList.length; i++)
			{
				MyLog.d(LOG_TAG, "Adding " + dirList[i] + " to temporary zip file");

				File f = new File(zipDir, dirList[i]);

				// zipping files only
				if (!f.isDirectory())
				{
					// create a FileInputStream on top of the file
					FileInputStream fis = new FileInputStream(f);
					// create a new zip entry
					ZipEntry anEntry = new ZipEntry(f.getName());
					// place the zip entry in the ZipOutputStream object
					zos.putNextEntry(anEntry);
					// now write the content of the file to the ZipOutputStream
					while ((bytesIn = fis.read(readBuffer)) != -1)
					{
						zos.write(readBuffer, 0, bytesIn);
					}
					// close entry and input stream
					zos.flush();
					zos.closeEntry();
					fis.close();
				}

			}
			zos.flush();
			zos.close();

			return true;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while zipping a file - " + e.getMessage());

			// delete the file
			try
			{
				new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
			} catch (Exception e1)
			{
				MyLog.d(LOG_TAG, "File could not have been deleted - it might be not created yet");
			}

			// release the lock from the collection
			ApplicationDBAdapter db = new ApplicationDBAdapter(mContext);
			db.open();
			try {
			  db.updateCollectionType(collectionID, collectionType);
			} finally {
			  db.close();
			}

			return false;
		}

	}

	public static boolean updateFile(Context context, String tempFileName, long collectionID,
			long globalID, int attempt)
	{
		try
		{
			// setting up the Apache HTTP client
			MultipartEntity form = new MultipartEntity();

			// add the temporary file
			form.addPart(
					"uploadedfile",
					new FileBody(new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName)));

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String email = preferences.getString(
					context.getString(R.string.preferences_code_email_address), "");

			String password = preferences.getString(
					context.getString(R.string.preferences_code_password), "");

			form.addPart("email_address", new StringBody(email));
			form.addPart("password", new StringBody(password));

			// get the collection
			ApplicationDBAdapter db = new ApplicationDBAdapter(context);
			db.open();
			Collection collection;
			try {
			  collection = db.getCollectionById(collectionID);
			} finally {
			  db.close();
			}

			// add the collection attributes
			form.addPart("title", new StringBody(collection.getTitle()));
			form.addPart("description", new StringBody(collection.getDescription()));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));

			String responseString = getResponseString(URL_UPDATE, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);

			// get the response code
			long responseCode = Long.parseLong(responseString);
			boolean result;

			// reopen the database
			db.open();

			if (responseCode == 0)
			{
				db.updateUploadTime(collectionID);
				result = true;
			} else
			{
				result = false;
			}

			// unlock the collection
			db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_SHARED_COLLECTION);

			db.close();

			new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();

			return result;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while uploading file - " + e.getMessage());
			if (attempt < ATTEMPT_LIMIT)
			{
				try
				{
					Thread.sleep(5000);
				} catch (InterruptedException e1)
				{
					e1.printStackTrace();

					try
					{
						new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
					} catch (Exception e2)
					{
						MyLog.d(LOG_TAG,
								"File could not have been deleted - it might be not created yet");
					}

					return false;
				}

				return updateFile(context, tempFileName, collectionID, globalID, attempt + 1);
			} else
			{
				// give up

				e.printStackTrace();

				// delete the file
				try
				{
					new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
				} catch (Exception e1)
				{
					MyLog.d(LOG_TAG, "File could not have been deleted - it might be not created yet");
				}

				// release the lock from the collection
				ApplicationDBAdapter db = new ApplicationDBAdapter(context);
				db.open();
				db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_SHARED_COLLECTION);
				db.close();

				return false;
			}
		}
	}

	public static boolean uploadFile(Context context, String tempFileName, long collectionID,
			int attempt)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();

			// add the temporary file
			form.addPart(
					"uploadedfile",
					new FileBody(new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName)));

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String email = preferences.getString(
					context.getString(R.string.preferences_code_email_address), "");

			String password = preferences.getString(
					context.getString(R.string.preferences_code_password), "");

			form.addPart("email_address", new StringBody(email));
			form.addPart("password", new StringBody(password));

			// get the collection
			ApplicationDBAdapter db = new ApplicationDBAdapter(context);
			db.open();
			Collection collection;
			try {
			  collection = db.getCollectionById(collectionID);
			} finally {
			  db.close();
			}

			// add the collection attributes
			form.addPart("title", new StringBody(collection.getTitle()));
			form.addPart("description", new StringBody(collection.getDescription()));

			String responseString = getResponseString(URL_UPLOAD, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);

			// get the response code
			long responseCode = Long.parseLong(responseString);
			boolean result;

			// reopen the database
			db.open();

			if (responseCode > 0)
			{

				db.updateGlobalID(collectionID, Long.parseLong(responseString));
				db.updateUploadTime(collectionID);
				db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_SHARED_COLLECTION);

				result = true;
			} else
			{
				// have a look at what the error was and report it to the user

				// unlock the collection
				db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_NON_SHARED);
				result = false;
			}
			db.close();

			new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();

			return result;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while uploading file - " + e.getMessage());
			if (attempt < ATTEMPT_LIMIT)
			{
				try
				{
					Thread.sleep(5000);
				} catch (InterruptedException e1)
				{
					e1.printStackTrace();

					try
					{
						new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
					} catch (Exception e2)
					{
						MyLog.d(LOG_TAG,
								"File could not have been deleted - it might be not created yet");
					}

					return false;
				}

				return uploadFile(context, tempFileName, collectionID, attempt + 1);
			} else
			{

				e.printStackTrace();

				// delete the file
				try
				{
					new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
				} catch (Exception e1)
				{
					MyLog.d(LOG_TAG, "File could not have been deleted - it might be not created yet");
				}

				// release the lock from the collection
				ApplicationDBAdapter db = new ApplicationDBAdapter(context);
				db.open();
				db.updateCollectionType(collectionID, Collection.TYPE_PRIVATE_NON_SHARED);
				db.close();

				return false;
			}
		}
	}

	public static boolean downloadFile(Context context, long globalID, String tempFileName,
			int attempt)
	{
		try
		{
			MyLog.d(LOG_TAG, "downloadFile() called");

			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

			String uniqueID = preferences.getString(
					context.getString(R.string.preferences_unique_id), "");

			form.addPart("unique_id", new StringBody(uniqueID));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));

			String response = getResponseString(URL_DOWNLOAD, form);

			MyLog.d(LOG_TAG, "Response of the server is - " + response);

			BufferedInputStream bufferedInputStream = new BufferedInputStream(obtainInputStream(
					response, null));

			FileOutputStream fileOutputStream = new FileOutputStream(ApplicationInitializer.COLLECTIONS_FOLDER
					+ tempFileName);

			byte[] buffer = new byte[2048];
			int bytesRead;

			while ((bytesRead = bufferedInputStream.read(buffer, 0, 2048)) != -1)
			{
				fileOutputStream.write(buffer, 0, bytesRead);
			}

			fileOutputStream.close();
			bufferedInputStream.close();

			return true;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Error occured while downloading the file - retrying in a few seconds");

			e.printStackTrace();

			if (attempt > ATTEMPT_LIMIT)
			{
				try
				{
					new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
				} catch (Exception e2)
				{
					MyLog.d(LOG_TAG, "File could not have been deleted - it might be not created yet");
				}

				return false;
			} else
			{
				try
				{
					Thread.sleep(60000);
					return downloadFile(context, globalID, tempFileName, attempt + 1);
				} catch (InterruptedException e1)
				{
					try
					{
						new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName).delete();
					} catch (Exception e2)
					{
						MyLog.d(LOG_TAG,
								"File could not have been deleted - it might be not created yet");
					}
					return false;
				}
			}
		}
	}

	/**
	 * 
	 * Unzips a temporary file from the collections folder. If a file is
	 * unzipped successfully, it will be deleted anyway. In case unzipping is
	 * unsuccessful, the deletion depends on deleteIfFailed value.
	 * 
	 * 
	 * @param localID
	 *            local collection ID - determines the folder where the files
	 *            will be unzipped to
	 * @param tempFileName
	 *            - ZIP file name
	 * @param deleteIfFailed
	 *            - flag telling whether to delete the file if unzipping fails
	 * @return
	 */
	public static boolean unzipFile(long localID, String tempFileName, boolean deleteIfFailed)
	{
		try
		{
			MyLog.d(LOG_TAG, "unzipFile() called");
			File targetFolder = new File(ApplicationInitializer.COLLECTIONS_FOLDER + localID + "/");
			targetFolder.mkdirs();

			File archiveFile = new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName);

			ZipFile zipFile = new ZipFile(archiveFile);

			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			ZipEntry entry;

			int count;

			byte[] data = new byte[2048];

			while (entries.hasMoreElements())
			{
				entry = (ZipEntry) entries.nextElement();

				if (!entry.isDirectory())
				{
					InputStream fis = zipFile.getInputStream(entry);

					FileOutputStream fos = new FileOutputStream(targetFolder.getAbsolutePath()
							+ "/" + entry.getName());
					BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);

					while ((count = fis.read(data, 0, 2048)) > 0)
					{
						dest.write(data, 0, count);
					}

					dest.flush();
					dest.close();
				}
			}
			zipFile.close();

			archiveFile.delete();

			return true;

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while unzipping file - printing stack trace. "
					+ e.getMessage());

			if (deleteIfFailed)
				(new File(ApplicationInitializer.COLLECTIONS_FOLDER + tempFileName)).delete();

			e.printStackTrace();
			return false;
		}
	}

	public static long[] checkForUpdates(Context context, int attempt)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String uniqueID = preferences.getString(
					context.getString(R.string.preferences_unique_id), "");

			form.addPart("unique_id", new StringBody(uniqueID));

			// get the collection
			ApplicationDBAdapter db = new ApplicationDBAdapter(context);
			db.open();
      ArrayList<Collection> collections;
      try {
        collections = db.getAllCollections();
      } finally {
        db.close();
      }

			StringBuilder requestString = new StringBuilder();
			for (Collection c : collections)
			{
				if (c.getType() == Collection.TYPE_DOWNLOADED_UNLOCKED)
				{
					requestString.append(c.getGlobalID());
					requestString.append(",");
				}
			}

			MyLog.d(LOG_TAG, "Sending request string - " + requestString);

			// add the collection attributes
			form.addPart("globalIDs", new StringBody(requestString.toString()));

			String responseString = getResponseString(URL_CHECK_UPDATES, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);

			if (!responseString.equals(""))
			{
				MyLog.d(LOG_TAG, "Response string is not empty");
				String[] globalIDs = responseString.split("\\,");

				long[] result = new long[globalIDs.length];

				for (int i = 0; i < globalIDs.length; i++)
				{
					MyLog.d(LOG_TAG, "Split string - " + globalIDs[i]);

					result[i] = Long.parseLong(globalIDs[i]);
				}

				return result;
			} else
				return new long[0];

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while receiving update list - " + e.getMessage());
			e.printStackTrace();

			if (attempt < ATTEMPT_LIMIT)
			{
				try
				{
					Thread.sleep(200);
					return checkForUpdates(context, attempt + 1);
				} catch (Exception e1)
				{
					return null;
				}
			} else
				return null;
		}
	}

	public static void reportError(Context context, long globalID, long cardID, String cardTitle,
			String message)
	{
		try
		{
			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String uniqueID = preferences.getString(
					context.getString(R.string.preferences_unique_id), "");

			form.addPart("unique_id", new StringBody(uniqueID));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));
			form.addPart("cardID", new StringBody(String.valueOf(cardID)));
			form.addPart("cardTitle", new StringBody(cardTitle));
			form.addPart("message", new StringBody(message));

			String responseString = getResponseString(URL_REPORT_ERROR, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while rating collection - " + e.getMessage());
			e.printStackTrace();

			try
			{
				Thread.sleep(5000);
				reportError(context, globalID, cardID, cardTitle, message);
			} catch (Exception e1)
			{
				return;
			}

		}
	}

	public static boolean regainRights(Context context, long globalID, int attempt) throws Exception
	{
		try
		{
			MultipartEntity form = new MultipartEntity();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			String emailAddress = preferences.getString(
					context.getString(R.string.preferences_code_email_address), "");
			String password = preferences.getString(
					context.getString(R.string.preferences_code_password), "");

			form.addPart("email_address", new StringBody(emailAddress));
			form.addPart("password", new StringBody(password));
			form.addPart("globalID", new StringBody(String.valueOf(globalID)));

			String responseString = getResponseString(URL_REGAIN_RIGHTS, form);

			MyLog.d(LOG_TAG, "Response of the server is " + responseString);
			
			if (responseString.equals("1"))
			{
				return true;
			}
			else if (responseString.equals("0"))
			{
				return false;
			}
			else
				throw new Exception();
			
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while rating collection - " + e.getMessage());
			e.printStackTrace();
			
			if (attempt < ATTEMPT_LIMIT)
			{
				
				try
				{
					Thread.sleep(200);
					return regainRights(context, globalID, ++attempt);
				} catch (Exception e1)
				{
					throw new Exception();
				}
			}
			else
				throw new Exception();
		}

	}
}
