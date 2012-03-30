package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * {@link Activity} that deals with collection choosing.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CollectionBrowser extends ListActivity
{
	private final static String LOG_TAG = "CollectionBrowser";

	private final static int ACTION_NEW_COLLECTION = 0;
	private final static int ACTION_COLLECTION_INFO = 1;

	/**
	 * String code for the collection ID to be returned in the {@link Intent}
	 */
	public final static String INTENT_COLLECTION_ID = "collectionID";

	/**
	 * String code for the action to be passed in the starting {@link Intent}.
	 * Must be one of the {@link #INTENT_ACTION_PICK_FOR_EDITING},
	 * {@link #INTENT_ACTION_PICK_FOR_LEARNING},
	 * {@link #INTENT_ACTION_PICK_FOR_UPLOADING}.
	 * 
	 */
	public final static String INTENT_ACTION = "action";

	public final static int INTENT_ACTION_PICK_FOR_LEARNING = 0;
	public final static int INTENT_ACTION_PICK_FOR_EDITING = 1;
	public final static int INTENT_ACTION_PICK_FOR_UPLOADING = 2;

	private int mAction;

	private final static int TITLE_TEXT_SIZE = 18;
	private final static int DESC_TEXT_SIZE = 16;

	private final static int CONTEXT_MENU_DELETE = 0;
	private final static int CONTEXT_MENU_INFO = 1;
	private final static int CONTEXT_MENU_EDIT = 2;
	private final static int CONTEXT_MENU_UNSHARE = 3;

	private ArrayList<Collection> mCollectionList;
	private CollectionAdapter mAdapter;
	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())
		{
			Intent intent = this.getIntent();

			mAction = intent.getIntExtra(INTENT_ACTION, INTENT_ACTION_PICK_FOR_LEARNING);

			if (mAction == INTENT_ACTION_PICK_FOR_EDITING)
			{
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = vi.inflate(R.layout.simplemenurow, null);

				TextView tv = (TextView) view.findViewById(R.id.text1);
				tv.setTextColor(Color.WHITE);				
				tv.setTextSize(20f);
				
				tv.setText(R.string.add_new_collection);
				this.getListView().addFooterView(view, null, true);
			}

			getListView().setTextFilterEnabled(true);
			getListView().setOnCreateContextMenuListener(this);

			populateCollections();
		}

	}

	/**
	 * Method to fill {@link ListView} with collections.
	 * 
	 */
	private void populateCollections()
	{
		ApplicationDBAdapter db = new ApplicationDBAdapter(this);

		db.open();

		try {
		  switch (mAction)
		  {
		    case INTENT_ACTION_PICK_FOR_LEARNING:
		      mCollectionList = db.getAllCollections();
		      break;
		    case INTENT_ACTION_PICK_FOR_EDITING:
		      mCollectionList = db.getEditableCollections();
		      break;
		    case INTENT_ACTION_PICK_FOR_UPLOADING:
		      mCollectionList = db.getUploadableCollections();
		      break;
		    default:
		      mCollectionList = db.getAllCollections();
		      break;
		  }
		} finally {
		  db.close();
		}

		// check whether the list is empty
		if (mCollectionList.size() == 0 && mAction == INTENT_ACTION_PICK_FOR_LEARNING)
		{
			this.setResult(RESULT_CANCELED);

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.no_collections);
			alert.setMessage(R.string.no_collections_for_learning_desc);

			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Intent intent = new Intent(CollectionBrowser.this, DownloadActivity.class);
					startActivity(intent);

					finish();
				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});

			alert.setOnCancelListener(new DialogInterface.OnCancelListener()
			{

				@Override
				public void onCancel(DialogInterface dialog)
				{
					finish();
				}
			});

			alert.show();

		}

		mAdapter = new CollectionAdapter(this, R.layout.menurow, mCollectionList);
		setListAdapter(mAdapter);

		// refilter the list
		CharSequence constrSequence = getListView().getTextFilter();

		if (constrSequence != null)
		{
			String constraint = constrSequence.toString();

			// make sure that we filter only when it is necessary
			if (!constraint.equals(""))
			{
				mAdapter.mFilter.filter(constrSequence);
			}
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent;

		if (position != mAdapter.mItemsShown.size())
		{
			Collection selected = mAdapter.mItemsShown.get(position);

			MyLog.d(LOG_TAG, "Selected collection that has ID - " + selected.getRowID());

			switch (mAction)
			{
			case INTENT_ACTION_PICK_FOR_LEARNING:
			case INTENT_ACTION_PICK_FOR_UPLOADING:
				// check whether the collection is not being downloaded or
				// uploaded
				if (selected.getType() == Collection.TYPE_CURRENTLY_DOWNLOADING
						|| selected.getType() == Collection.TYPE_CURRENTLY_UPLOADING)
				{
					Toast.makeText(this, R.string.collection_busy_toast, Toast.LENGTH_SHORT).show();
					return;
				} else if (mAction == INTENT_ACTION_PICK_FOR_UPLOADING
						&& selected.getType() == Collection.TYPE_PRIVATE_SHARED_COLLECTION)
				{
					// show the dialog asking whether to update collection
					showUpdateDialog(selected.getRowID());
				} else
				{
					// check card count first as picking empty collection is not
					// allowed
					checkCountAndReturn(selected.getRowID());

				}
				break;
			case INTENT_ACTION_PICK_FOR_EDITING:
				if (selected.getType() == Collection.TYPE_CURRENTLY_DOWNLOADING
						|| selected.getType() == Collection.TYPE_CURRENTLY_UPLOADING)
				{
					Toast.makeText(this, R.string.collection_busy_toast, Toast.LENGTH_SHORT).show();
					return;
				} else
				{
					intent = new Intent(this, CardBrowser.class);
					intent.putExtra(CardBrowser.INTENT_COLLECTION_ID, selected.getRowID());
					intent.putExtra(CardBrowser.INTENT_ALLOW_ADDING, true);
					intent.putExtra(CardBrowser.INTENT_ALLOW_DELETING, true);
					intent.putExtra(CardBrowser.INTENT_ACTION, CardBrowser.ACTION_BROWSE_FOR_EDIT);
					startActivity(intent);
				}
				break;

			}

		} else if (position == mCollectionList.size() && mAction == INTENT_ACTION_PICK_FOR_EDITING)
		{
			launchNewForm();
		}
	}

	/**
	 * Method that checks number of cards in the collection and, if it is
	 * greater than 0, packs everything to the {@link Intent} and returns to the
	 * calling activity.
	 * 
	 * @param collectionID
	 *            collection ID
	 */
	private void checkCountAndReturn(long collectionID)
	{
		CardDBAdapter db = new CardDBAdapter();
		db.open(collectionID);
		int cardCount;
		try {
		  cardCount = db.getCardCount();
		} finally {
		  db.close();
		}

		if (cardCount < 1)
		{
			Toast.makeText(this, R.string.collection_empty_toast, Toast.LENGTH_SHORT).show();
			return;
		} else
		{
			Intent intent = new Intent();
			intent.putExtra(INTENT_COLLECTION_ID, collectionID);
			this.setResult(RESULT_OK, intent);
			this.finish();
		}

	}

	/**
	 * Shows dialog suggesting to reupload collection to the server.
	 * 
	 * 
	 * @param collID
	 *            collection ID
	 */
	private void showUpdateDialog(long collID)
	{
		final long collectionID = collID;

		AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(this);
		updateDialogBuilder.setTitle(R.string.collection_already_uploaded);
		updateDialogBuilder.setMessage(R.string.would_you_like_to_update);

		updateDialogBuilder.setPositiveButton(R.string.yes, new OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if (which == AlertDialog.BUTTON_POSITIVE)
					checkCountAndReturn(collectionID);
			}
		});

		updateDialogBuilder.setNegativeButton(R.string.no, new OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
			// do not have to do anything
			}
		});

		updateDialogBuilder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (mAction == INTENT_ACTION_PICK_FOR_EDITING)
		{
			MenuItem addNewButton = menu.add(getString(R.string.create_new));
			addNewButton.setIcon(android.R.drawable.ic_menu_add);
			addNewButton.setOnMenuItemClickListener(new OnMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					launchNewForm();
					return false;
				}
			});
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId())
		{
		case CONTEXT_MENU_DELETE:

			showDeleteDialog(mAdapter.mItemsShown.get((int) info.id).getRowID());

			return true;
		case CONTEXT_MENU_INFO:
			long collectionID = mAdapter.mItemsShown.get((int) info.id).getRowID();

			Intent intent = new Intent(this, ViewCollection.class);

			intent.putExtra(ViewCollection.INTENT_COLLECTION_ID, collectionID);

			this.startActivityForResult(intent, ACTION_COLLECTION_INFO);

			return true;

		case CONTEXT_MENU_EDIT:

			Intent launchIntent = new Intent(this, CollectionEditor.class);
			launchIntent.putExtra(CollectionEditor.INTENT_ACTION, CollectionEditor.INTENT_ACTION_EDIT);
			launchIntent.putExtra(CollectionEditor.INTENT_COLLECTION_ID, mAdapter.mItemsShown.get(
					(int) info.id).getRowID());

			startActivityForResult(launchIntent, ACTION_NEW_COLLECTION);

			break;
		case CONTEXT_MENU_UNSHARE:
			final Collection c = mAdapter.mItemsShown.get((int) info.id);

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.removing_collection_from_server);
			alert.setMessage(R.string.removing_collection_from_server_desc);

			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{

					// change the type in the Database
					ApplicationDBAdapter dbUnshare = new ApplicationDBAdapter(CollectionBrowser.this);
					dbUnshare.open();
					try {
					  dbUnshare.updateCollectionType(c.getRowID(), Collection.TYPE_PRIVATE_NON_SHARED);
					} finally {
					  dbUnshare.close();
					}

					// fire off the intent to the web server
					Intent unshareIntent = new Intent(CollectionBrowser.this, CollectionUnshareService.class);
					unshareIntent.putExtra(CollectionUnshareService.INTENT_GLOBAL_ID,
							c.getGlobalID());

					startService(unshareIntent);

					populateCollections();

					Toast.makeText(CollectionBrowser.this, R.string.collection_unshared,
							Toast.LENGTH_SHORT).show();

				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
				// TODO Auto-generated method stub

				}
			});

			alert.show();

			break;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * 
	 * Method that shows the delete dialog for a particular collection.
	 * 
	 * @param collectionID
	 *            collection ID
	 */
	private void showDeleteDialog(long collectionID)
	{
		final long cID = collectionID;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_collection);
		builder.setMessage(R.string.delete_collection_confirm);
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				deleteCollection(cID);
			}
		});

		builder.setNegativeButton(R.string.no, null);

		builder.show();

	}

	/**
	 * 
	 * Method that deletes collection. Shows a {@link ProgressDialog} and
	 * deletes a collection in a separate thread.
	 * 
	 * @param collectionID
	 */
	private void deleteCollection(long collectionID)
	{
		final long cID = collectionID;

		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(R.string.deleting_collection);
		progressDialog.setMessage(this.getString(R.string.deleting_collection));
		progressDialog.show();

		final Handler myHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				progressDialog.dismiss();
				populateCollections();
				super.handleMessage(msg);
			}
		};

		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				ApplicationDBAdapter db = new ApplicationDBAdapter(CollectionBrowser.this);
				db.open();
				try {
				  db.deleteCollection(cID);
				} finally {
				  db.close();
				}
				myHandler.sendEmptyMessage(0);
			}
		});
		t.start();

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		long position = info.id;

		// if it is not a "add new collection button" - show context menu
		if (position != -1)
		{
			menu.setHeaderTitle(R.string.choose_action);

			int collectionType = mCollectionList.get((int) position).getType();

			// allow editing title and description of your own collections only
			if (collectionType == Collection.TYPE_PRIVATE_NON_SHARED
					|| collectionType == Collection.TYPE_PRIVATE_SHARED_COLLECTION)
				menu.add(0, CONTEXT_MENU_EDIT, 0, R.string.edit_title_description);

			// do not show the delete option if collection is being downloaded
			// or uploaded
			if ((mAction == INTENT_ACTION_PICK_FOR_LEARNING || mAction == INTENT_ACTION_PICK_FOR_EDITING)
					&& !(collectionType == Collection.TYPE_CURRENTLY_DOWNLOADING || collectionType == Collection.TYPE_CURRENTLY_UPLOADING))
				menu.add(0, CONTEXT_MENU_DELETE, 0, R.string.delete_collection);

			// only show unshare option if we are in the uploading activity and
			// collection is already shared
			if (mAction == INTENT_ACTION_PICK_FOR_UPLOADING
					&& collectionType == Collection.TYPE_PRIVATE_SHARED_COLLECTION)
				menu.add(0, CONTEXT_MENU_UNSHARE, 0, R.string.remove_from_server);

			// show more information anyway
			menu.add(0, CONTEXT_MENU_INFO, 0, R.string.information_collection);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		// just refill collections with new data in case anything changed
		populateCollections();
	}

	/**
	 * Method to launch {@link CollectionEditor} for creating a new collection.
	 * 
	 */
	private void launchNewForm()
	{
		Intent launchIntent = new Intent(this, CollectionEditor.class);
		launchIntent.putExtra(CollectionEditor.INTENT_ACTION, CollectionEditor.INTENT_ACTION_NEW);

		startActivityForResult(launchIntent, ACTION_NEW_COLLECTION);
	}

	/**
	 * 
	 * Custom adapter for showing collections in a {@link ListView}.
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private class CollectionAdapter extends ArrayAdapter<Collection>
	{
		public ArrayList<Collection> mItemsShown;
		private ArrayList<Collection> mItems;

		private Filter mFilter = new MyFilter();

		private class MyFilter extends Filter
		{

			@Override
			protected FilterResults performFiltering(CharSequence arg0)
			{
				ArrayList<Collection> result = new ArrayList<Collection>();

				String keyword = arg0.toString();
				keyword = keyword.toLowerCase();

				for (Collection c : mItems)
				{
					if (c.getTitle().toLowerCase().startsWith(keyword))
						result.add(c);
				}

				Filter.FilterResults results = new Filter.FilterResults();
				results.count = result.size();
				results.values = result;

				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results)
			{
				mItemsShown = (ArrayList<Collection>) results.values;
				if (results.count > 0)
				{
					notifyDataSetChanged();
				} else
				{
					notifyDataSetInvalidated();
				}

			}

		}

		public CollectionAdapter(Context context, int textViewResourceId,
				ArrayList<Collection> items)
		{
			super(context, textViewResourceId, items);
			this.mItems = items;
			this.mItemsShown = items;
		}

		class ViewHolder
		{
			FrameLayout iconLayout;
			TextView topText;
			TextView botText;
			RatingBar ratingBar;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view;
			ViewHolder viewHolder;

			if (convertView == null)
			{
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.collectionrow, null);

				// instantiate ViewHolder
				viewHolder = new ViewHolder();
				viewHolder.topText = (TextView) view.findViewById(R.id.toptext);
				viewHolder.topText.setTextSize(TITLE_TEXT_SIZE);

				viewHolder.botText = (TextView) view.findViewById(R.id.bottomtext);
				viewHolder.botText.setTextSize(DESC_TEXT_SIZE);

				viewHolder.ratingBar = (RatingBar) view.findViewById(R.id.ratingbar);

				viewHolder.iconLayout = (FrameLayout) view.findViewById(R.id.iconLayout);

				view.setTag(viewHolder);

			} else
			{
				view = convertView;
				viewHolder = (ViewHolder) convertView.getTag();
			}

			// get the collection
			Collection collection = mItemsShown.get(position);
			if (collection != null)
			{

				viewHolder.topText.setText(collection.getTitle());

				viewHolder.botText.setText(collection.getDescription());

				viewHolder.ratingBar.setRating((float) collection.getRating());

				// TODO : fix this up so that it is efficient

				viewHolder.iconLayout.removeAllViews();

				ImageView collView = new ImageView(CollectionBrowser.this);
				collView.setImageResource(R.drawable.collection);

				ImageView lockedOverlay;

				switch (collection.getType())
				{
				case Collection.TYPE_CURRENTLY_DOWNLOADING:
					viewHolder.iconLayout.addView(collView);

					lockedOverlay = new ImageView(CollectionBrowser.this);
					lockedOverlay.setImageResource(R.drawable.lockoverlay);

					viewHolder.iconLayout.addView(lockedOverlay);
					break;
				case Collection.TYPE_PRIVATE_NON_SHARED:
					viewHolder.iconLayout.addView(collView);
					break;
				case Collection.TYPE_DOWNLOADED_LOCKED:

					viewHolder.iconLayout.addView(collView);

					lockedOverlay = new ImageView(CollectionBrowser.this);
					lockedOverlay.setImageResource(R.drawable.lockoverlay);

					viewHolder.iconLayout.addView(lockedOverlay);

					break;
				case Collection.TYPE_DOWNLOADED_UNLOCKED:
					ImageView shareView = new ImageView(CollectionBrowser.this);
					shareView.setImageResource(R.drawable.downloaded);

					viewHolder.iconLayout.addView(shareView);

					break;
				case Collection.TYPE_CURRENTLY_UPLOADING:
					viewHolder.iconLayout.addView(collView);

					lockedOverlay = new ImageView(CollectionBrowser.this);
					lockedOverlay.setImageResource(R.drawable.lockoverlay);

					viewHolder.iconLayout.addView(lockedOverlay);
					break;
				case Collection.TYPE_PRIVATE_SHARED_COLLECTION:

					viewHolder.iconLayout.addView(collView);

					ImageView sharedOverlay = new ImageView(CollectionBrowser.this);
					sharedOverlay.setImageResource(R.drawable.shareoverlay);

					viewHolder.iconLayout.addView(sharedOverlay);

					break;
				}

			}
			return view;
		}

		@Override
		public Filter getFilter()
		{
			return mFilter;
		}

		@Override
		public int getCount()
		{
			return mItemsShown.size();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_SEARCH)
		{
			// show the search bar
			showSearchDialog();

			return true;
		} else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * Method to show search dialog. Invoked when Search button is pressed.
	 * 
	 */
	private void showSearchDialog()
	{
		final AlertDialog.Builder searchDialog = new AlertDialog.Builder(this);
		searchDialog.setTitle(R.string.search);

		final EditText searchField = new EditText(this);
		searchField.setText(this.getListView().getTextFilter());
		searchField.setSingleLine();

		ImageButton iButton = new ImageButton(this);
		iButton.setImageResource(R.drawable.ic_btn_search);

		LinearLayout mainHolder = new LinearLayout(this);
		mainHolder.setOrientation(LinearLayout.HORIZONTAL);
		mainHolder.setGravity(Gravity.CENTER);

		mainHolder.addView(searchField,
				new ViewGroup.LayoutParams(200, ViewGroup.LayoutParams.WRAP_CONTENT));
		mainHolder.addView(
				iButton,
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mainHolder.setPadding(5, 5, 5, 5);

		// set up the search button listener
		iButton.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				String searchString = searchField.getEditableText().toString();

				if (!searchString.equals(""))
				{
					CollectionBrowser.this.getListView().setFilterText(searchString);
				}

				// hide the soft keyboard
				InputMethodManager imm = (InputMethodManager) CollectionBrowser.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

				mAlertDialog.dismiss();
			}
		});

		searchDialog.setView(mainHolder);

		mAlertDialog = searchDialog.show();
	}

}
