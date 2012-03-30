package uk.ac.cam.cl.dtg.android.language;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * Receives the keys from the collection-specific applications sold on the
 * Android Market. <b>Not used</b> currently, need to have a look at broadcast
 * filtering, etc.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class KeyReceiver extends BroadcastReceiver
{
	private static final String INTENT_KEY = "key";
	private static final String INTENT_COLLECTION_GLOBAL_ID = "collectionTag";
	
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String key = intent.getStringExtra(INTENT_KEY);
		int collectionTag = intent.getIntExtra(INTENT_COLLECTION_GLOBAL_ID, -1);

		// TODO: save a key into the database

		Toast.makeText(
				context,
				"KeyReceiver - received intent, key is \"" + key + "\" and the collection tag is "
						+ String.valueOf(collectionTag), Toast.LENGTH_SHORT).show();

		Log.d("KeyReceiver", "onReceive() called");
	}

}
