package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * Activity used to browse over all the cards in a given collection.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CardBrowser extends ListActivity
{
	private static final String LOG_TAG = "CardBrowser";

	/**
	 * {@link Intent} code for collection ID (must carry long value)
	 */
	public static final String INTENT_COLLECTION_ID = "collectionID";

	/**
	 * {@link Intent} code for action - int, must be either
	 * {@link #ACTION_BROWSE_FOR_EDIT} or {@link #ACTION_BROWSE_FOR_PICK}
	 */
	public static final String INTENT_ACTION = "action";
	/**
	 * {@link Intent} code for boolean value whether to allow adding.
	 */
	public static final String INTENT_ALLOW_ADDING = "allowAdding";
	/**
	 * {@link Intent} code for boolean value whether to allow deleting.
	 */
	public static final String INTENT_ALLOW_DELETING = "allowDeleting";
	/**
	 * {@link Intent} code for boolean value whether to allow editing.
	 */
	public static final String INTENT_ALLOW_EDITING = "allowEditing";

	public static final int ACTION_BROWSE_FOR_PICK = 0;
	public static final int ACTION_BROWSE_FOR_EDIT = 1;

	private static final int ACTIVITY_NEW_CARD = 0;
	private static final int ACTIVITY_EDIT_CARD = 1;

	/**
	 * {@link Intent} string for returning card ID selected
	 */
	public static final String RESULT_CARD_ID = "cardID";

	private final static int TITLE_TEXT_SIZE = 18;
	private final static int DESC_TEXT_SIZE = 16;

	private final static int CONTEXT_MENU_DELETE = 0;
	private final static int CONTEXT_MENU_EDIT = 1;
	private final static int CONTEXT_MENU_RENAME = 2;

	private long mCollectionID;
	private int mAction;

	private boolean allowAdding, allowDeleting, allowEditing;

	private ArrayList<Card> mCards;

	private CardAdapter mAdapter;

	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		L.d(LOG_TAG, "onCreate() called");

		super.onCreate(savedInstanceState);

		ApplicationInitializer initializer = new ApplicationInitializer(this);
		if (initializer.initializeActivity())
		{

			setVolumeControlStream(AudioManager.STREAM_MUSIC);

			Intent intent = this.getIntent();
			mCollectionID = intent.getLongExtra(INTENT_COLLECTION_ID, 0);
			mAction = intent.getIntExtra(INTENT_ACTION, ACTION_BROWSE_FOR_PICK);
			allowAdding = intent.getBooleanExtra(INTENT_ALLOW_ADDING, false);
			allowDeleting = intent.getBooleanExtra(INTENT_ALLOW_DELETING, false);
			allowEditing = intent.getBooleanExtra(INTENT_ALLOW_EDITING, true);

			if (allowAdding)
			{
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View view = vi.inflate(R.layout.simplemenurow, null);

				TextView tv = (TextView) view.findViewById(R.id.text1);

				tv.setTextColor(Color.WHITE);
				tv.setTextSize(20f);
				tv.setText(this.getString(R.string.add_new_card));
				this.getListView().addFooterView(view, null, true);
			}

			getListView().setOnCreateContextMenuListener(this);
			getListView().setTextFilterEnabled(true);

			fillData();
			L.d(LOG_TAG, "onCreate() finished");
		}
	}

	/**
	 * Fills the data for the {@link ListView}
	 * 
	 */
	private void fillData()
	{
		L.d(LOG_TAG, "fillData() called");

		CardDBAdapter db = new CardDBAdapter();

		db.open(mCollectionID);
		try {
		  mCards = db.getAllCards();
		} finally {
		  db.close();
		}

		mAdapter = new CardAdapter(this, R.layout.menurow, mCards);
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

		L.d(LOG_TAG, "fillData() finished");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
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
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Launches {@link CardEditor} for creating a new card.
	 * 
	 */
	private void launchNewForm()
	{
		Intent intent = new Intent(this, CardEditor.class);

		intent.putExtra(CardEditor.INTENT_ACTION, CardEditor.ACTION_NEW);
		intent.putExtra(CardEditor.INTENT_COLLECTION_ID, mCollectionID);
		startActivityForResult(intent, ACTIVITY_NEW_CARD);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		Intent intent;

		if (position != mAdapter.mItemsShown.size())
		{
			if (mAction == ACTION_BROWSE_FOR_PICK)
			{
				intent = new Intent();
				intent.putExtra(RESULT_CARD_ID, mAdapter.mItemsShown.get(position).getId());

				this.setResult(RESULT_OK, intent);
				this.finish();
			} else
			{
				// launch card editor
				launchCardEditor(mAdapter.mItemsShown.get(position).getId());
			}
		} else
		{
			launchNewForm();
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		final long cardID = mAdapter.mItemsShown.get((int) info.id).getId();

		switch (item.getItemId())
		{
		case CONTEXT_MENU_DELETE:

			ResourceHelper resHelper = new ResourceHelper(this);
			resHelper.updateResourcesBeforeDeleting(mCollectionID, cardID);

			CardDBAdapter db = new CardDBAdapter();
			db.open(mCollectionID);
			try {
			  db.deleteCard(cardID);
			} finally {
			  db.close();
			}

			fillData();

			return true;
		case CONTEXT_MENU_EDIT:
			launchCardEditor(cardID);
			return true;
		case CONTEXT_MENU_RENAME:

			// show dialog that contains the EditText
			LinearLayout holder = new LinearLayout(this);
			final EditText newNameField = new EditText(this);
			holder.setPadding(5, 2, 5, 2);
			holder.addView(
					newNameField,
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			CardDBAdapter db2 = new CardDBAdapter();
			db2.open(mCollectionID);
			Card c;
			try {
			  c = db2.getCardById(cardID);
			} finally {
			  db2.close();
			}

			newNameField.setText(c.getTitle());

			AlertDialog.Builder renameDialog = new AlertDialog.Builder(this);
			renameDialog.setTitle(R.string.rename_card);
			renameDialog.setView(holder);
			renameDialog.setPositiveButton(R.string.ok, new OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					CardDBAdapter db3 = new CardDBAdapter();
					db3.open(mCollectionID);
					try {
					  db3.updateTitle(cardID, newNameField.getEditableText().toString());
					} finally {
					  db3.close();
					}

					fillData();
				}
			});

			renameDialog.show();

			return true;

		}
		return super.onContextItemSelected(item);
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

			menu.add(0, CONTEXT_MENU_RENAME, 0, R.string.rename_card);

			if (allowEditing)
				menu.add(0, CONTEXT_MENU_EDIT, 0, R.string.edit_card);

			if (allowDeleting)
				menu.add(0, CONTEXT_MENU_DELETE, 0, R.string.delete_card);

		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		L.d(LOG_TAG, "onActivityResult() called");

		// refresh the data
		fillData();

		super.onActivityResult(requestCode, resultCode, data);

		L.d(LOG_TAG, "onActivityResult() finished");
	}
	
	/**
	 * 
	 * Launches {@link CardEditor} for editing a card with a given card ID.
	 * 
	 * @param cardID
	 */
	private void launchCardEditor(long cardID)
	{
		Intent intent = new Intent(this, CardEditor.class);

		intent.putExtra(CardEditor.INTENT_ACTION, CardEditor.ACTION_EDIT);
		intent.putExtra(CardEditor.INTENT_COLLECTION_ID, mCollectionID);
		intent.putExtra(CardEditor.INTENT_CARD_ID, cardID);
		startActivityForResult(intent, ACTIVITY_EDIT_CARD);

	}
	
	/**
	 * Class for custom {@link ArrayAdapter} for this {@ListView}
	 * 
	 * @author Vytautas Vaitukaitis
	 *
	 */
	private class CardAdapter extends ArrayAdapter<Card> implements Filterable
	{
		private Context mContext;
		private ArrayList<Card> mCards;
		private ArrayList<Card> mItemsShown;

		private Filter mFilter = new MyFilter();

		private class MyFilter extends Filter
		{

			@Override
			protected FilterResults performFiltering(CharSequence arg0)
			{
				L.d(LOG_TAG, "performFiltering(" + arg0 + ") called");

				ArrayList<Card> result = new ArrayList<Card>();

				String keyword = arg0.toString();
				keyword = keyword.toLowerCase();

				for (Card c : mCards)
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
				mItemsShown = (ArrayList<Card>) results.values;
				if (results.count > 0)
				{
					notifyDataSetChanged();
				} else
				{
					notifyDataSetInvalidated();
				}

			}

		}

		public CardAdapter(Context context, int textViewResourceId, ArrayList<Card> cards)
		{
			super(context, textViewResourceId, cards);
			mContext = context;
			this.mCards = cards;
			this.mItemsShown = cards;
		}

		class ViewHolder
		{
			TextView topText;
			TextView botText;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			L.d(LOG_TAG, "getView() called for position " + position);

			View view;
			ViewHolder viewHolder;

			// check whether the convertView is there - if not, inflate the
			// views
			if (convertView == null)
			{
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.cardrow, null);

				viewHolder = new ViewHolder();
				viewHolder.topText = (TextView) view.findViewById(R.id.toptext);
				viewHolder.topText.setTextSize(TITLE_TEXT_SIZE);

				viewHolder.botText = (TextView) view.findViewById(R.id.bottomtext);
				viewHolder.botText.setTextSize(DESC_TEXT_SIZE);

				view.setTag(viewHolder);

			} else
			{
				view = convertView;
				viewHolder = (ViewHolder) convertView.getTag();
			}

			// get the collection
			Card card = mItemsShown.get(position);
			if (card != null)
			{

				viewHolder.topText.setText(card.getTitle());

				String formattedTime = (String) DateFormat.getDateFormat(mContext).format(
						new Date(card.getUpdateTime()))
						+ " "
						+ (String) DateFormat.getTimeFormat(mContext).format(
								new Date(card.getUpdateTime()));

				viewHolder.botText.setText(formattedTime);

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
	 * Opens up and shows a search dialog.
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
					CardBrowser.this.getListView().setFilterText(searchString);
				}

				// hide the soft keyboard
				InputMethodManager imm = (InputMethodManager) CardBrowser.this.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

				mAlertDialog.dismiss();
			}
		});

		searchDialog.setView(mainHolder);

		mAlertDialog = searchDialog.show();
	}

}
