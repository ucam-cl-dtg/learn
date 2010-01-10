package uk.ac.cam.cl.dtg.android.language;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * {@link Activity} for editing collection titles and description
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CollectionEditor extends Activity
{
	public final static String INTENT_ACTION = "action";
	public final static int INTENT_ACTION_NEW = 0;
	public final static int INTENT_ACTION_EDIT = 1;

	public final static String INTENT_COLLECTION_ID = "collectionID";

	private int mAction;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		this.setContentView(R.layout.collectionform);

		super.onCreate(savedInstanceState);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())
		{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			// get the desired action from the intent
			Intent intent = this.getIntent();
			mAction = intent.getIntExtra(INTENT_ACTION, INTENT_ACTION_NEW);

			// sort out all the views
			final EditText titleView = (EditText) findViewById(R.id.title);
			titleView.setSingleLine();
			
			final EditText descView = (EditText) findViewById(R.id.description);
			descView.setGravity(Gravity.TOP | Gravity.LEFT);

			Button button = (Button) findViewById(R.id.okbutton);

			if (mAction == INTENT_ACTION_NEW)
			{
				button.setOnClickListener(new View.OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						String title = titleView.getEditableText().toString();

						if (checkTitle(title))
							createNewCollection(title, descView.getEditableText().toString());
					}
				});
			} else
			{
				ApplicationDBAdapter db = new ApplicationDBAdapter(this);
				db.open();

				final Collection c = db.getCollectionById(intent.getLongExtra(INTENT_COLLECTION_ID,
						-1));

				db.close();

				titleView.setText(c.getTitle());
				descView.setText(c.getDescription());

				button.setOnClickListener(new View.OnClickListener()
				{

					@Override
					public void onClick(View v)
					{
						String title = titleView.getEditableText().toString();

						if (checkTitle(title))
							updateTitleAndDescription(c.getRowID(), title,
									descView.getEditableText().toString());
					}
				});

			}
		}

	}

	/**
	 * 
	 * Creates a new collection with the given title and description. Does that
	 * in separate thread as it might take long to clean up a collection folder
	 * if something's in it.
	 * 
	 * @param title
	 *            collection title
	 * @param desc
	 *            collection description
	 */
	private void createNewCollection(String title, String desc)
	{
		final String titleFinal = title, descFinal = desc;

		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(R.string.creating_collection);
		progressDialog.setMessage(this.getString(R.string.creating_collection));
		progressDialog.setCancelable(false);
		progressDialog.show();

		final Handler myHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				progressDialog.dismiss();
				super.handleMessage(msg);

				setResult(RESULT_OK);
				finish();

			}
		};

		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				ApplicationDBAdapter db = new ApplicationDBAdapter(CollectionEditor.this);
				db.open();
				db.insertCollection(titleFinal, Collection.TYPE_PRIVATE_NON_SHARED, 0, descFinal,
						-1);
				db.close();
				myHandler.sendEmptyMessage(0);
			}
		});
		t.start();

	}

	/**
	 * Method that checks whether title is long enough (3 symbols or longer) and
	 * shows notification if it is not.
	 * 
	 * @param title
	 *            title to be checked
	 * @return true if title is long enough, false if it isn't and the toast was
	 *         shown
	 */
	private boolean checkTitle(String title)
	{
		if (title.length() > 2)
		{
			return true;
		} else
		{
			Toast.makeText(this, R.string.title_too_short, Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	/**
	 * Method to update title and description in the database. Finishes
	 * {@link Activity} after that is done.
	 * 
	 * @param collectionID local collection ID
	 * @param title title of the collection
	 * @param description new description of the collection
	 */
	private void updateTitleAndDescription(long collectionID, String title, String description)
	{
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);
		db.open();

		db.updateTitleAndDescription(collectionID, title, description);

		db.close();

		this.setResult(RESULT_OK);
		this.finish();
	}

}
