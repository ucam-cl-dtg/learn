package uk.ac.cam.cl.dtg.android.language;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TabHost;
import android.widget.TextView;

/**
 * 
 * {@link Activity} for browsing the collections at the server.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class DownloadActivity extends TabActivity implements
		TabHost.OnTabChangeListener, OnItemClickListener {
	private static final String LOG_TAG = "DownloadActivity";

	private static final String TAB_BY_POPULARITY = "popularity",
			TAB_BY_DATE = "date";

	private static final String BUNDLE_KEYWORD = "keyword";

	private static final int COUNT_DOWNLOAD_FIRST = 10;
	private static final int COUNT_LOAD_INCREMENT = 5;

	private static final int ACTIVITY_SHOW_COLLECTION = 0;

	private static final String URL_COLLECTION_LIST = ServerHelper.URL_GET_COLLECTIONS;

	private ArrayList<OnlineCollection> mCollectionsDate;
	private ArrayList<OnlineCollection> mCollectionsPopularity;

	private ListView mListPopularity, mListDate;

	private String mKeyword;

	private AlertDialog mAlertDialog;

	private CollectionFeedAdapter mAdapterDate = null,
			mAdapterPopularity = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(false);

		View frame = View.inflate(this, R.layout.onlinelistview, getTabHost()
				.getTabContentView());

		View emptyView = frame.findViewById(R.id.loadingLayout);

		mListPopularity = (ListView) frame.findViewById(R.id.listbypopularity);
		mListPopularity.setEmptyView(emptyView);

		mListDate = (ListView) frame.findViewById(R.id.listbydate);
		mListDate.setEmptyView(emptyView);

		TabHost tabs = getTabHost();

		tabs.setOnTabChangedListener(this);

		if (savedInstanceState != null)
			mKeyword = savedInstanceState.getString(BUNDLE_KEYWORD);
		else
			mKeyword = "";

		TabHost.TabSpec specPopularity = tabs.newTabSpec(TAB_BY_POPULARITY);
		specPopularity.setContent(new TabHost.TabContentFactory() {
			@Override
      public View createTabContent(String tag) {
				MyLog.d(LOG_TAG, "Adding create tab content for popularity");

				mCollectionsPopularity = new ArrayList<OnlineCollection>();

				mAdapterPopularity = new CollectionFeedAdapter(
						mCollectionsPopularity,
						CollectionFeedAdapter.ORDER_BY_POPULARITY, mKeyword);
				mListPopularity.setAdapter(mAdapterPopularity);

				mListPopularity.setOnItemClickListener(DownloadActivity.this);

				mListPopularity.setOnScrollListener(new OnScrollListener() {
					@Override
					public void onScroll(AbsListView arg0, int arg1, int arg2,
							int totalItemCount) {
						MyLog.d(LOG_TAG + "- popularity", arg1 + ", " + arg2
								+ ", " + totalItemCount);

						int lastVisible = arg0.getLastVisiblePosition();

						// check whether the list is still initializing
						if (lastVisible != -1) {
							if ((lastVisible >= totalItemCount - 1)) {
								mAdapterPopularity.startDownloading();
							}
						}
					}

					@Override
					public void onScrollStateChanged(AbsListView arg0, int arg1) {
					}
				});

				mListPopularity.setTextFilterEnabled(true);
				mListPopularity.setFilterText(mKeyword);
				mAdapterPopularity.startDownloading();

				return mListPopularity;
			}

		});

		specPopularity.setIndicator(getString(R.string.by_popularity),
				getResources().getDrawable(R.drawable.people));
		tabs.addTab(specPopularity);

		TabHost.TabSpec specDate = tabs.newTabSpec(TAB_BY_DATE);
		specDate.setContent(new TabHost.TabContentFactory() {
			@Override
      public View createTabContent(String tag) {
				mCollectionsDate = new ArrayList<OnlineCollection>();

				mAdapterDate = new CollectionFeedAdapter(mCollectionsDate,
						CollectionFeedAdapter.ORDER_BY_DATE, mKeyword);
				mListDate.setAdapter(mAdapterDate);

				mListDate.setOnItemClickListener(DownloadActivity.this);

				mListDate.setOnScrollListener(new OnScrollListener() {

					@Override
					public void onScroll(AbsListView arg0, int arg1, int arg2,
							int totalItemCount) {
						MyLog.d(LOG_TAG + "- date", arg1 + ", " + arg2 + ", "
								+ totalItemCount);
						// TODO Auto-generated method stub
						int lastVisible = arg0.getLastVisiblePosition();

						// check whether the list is still initializing
						if (lastVisible != -1) {
							if ((lastVisible >= totalItemCount - 1)) {
								mAdapterDate.startDownloading();
							}
						}
					}

					@Override
					public void onScrollStateChanged(AbsListView arg0, int arg1) {
						// TODO Auto-generated method stub

					}
				});

				mListDate.setTextFilterEnabled(true);
				mListDate.setFilterText(mKeyword);

				mAdapterDate.startDownloading();

				return mListDate;
			}
		});

		specDate.setIndicator(getString(R.string.by_date), getResources()
				.getDrawable(R.drawable.date));

		tabs.addTab(specDate);

		// set the popularity tab as the first one
		tabs.setCurrentTabByTag(TAB_BY_POPULARITY);
	}

	@Override
	public void onTabChanged(String tabId) {
		MyLog.d(LOG_TAG, "Keyword is " + mKeyword);

		if (tabId.equals(TAB_BY_POPULARITY)) {
			mAdapterPopularity.dataSetChangeOnActivation();
			if (mKeyword.equals("")) {
				mListPopularity.clearTextFilter();

			} else
				mListPopularity.setFilterText(mKeyword);
		} else if (tabId.equals(TAB_BY_DATE)) {
			mAdapterDate.dataSetChangeOnActivation();
			if (mKeyword.equals("")) {
				mListDate.clearTextFilter();
			} else
				mListDate.setFilterText(mKeyword);

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(BUNDLE_KEYWORD, mKeyword);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// get the active tab
		String currentTabTag = this.getTabHost().getCurrentTabTag();

		OnlineCollection collection;

		if (currentTabTag.equals(TAB_BY_POPULARITY))
			collection = mCollectionsPopularity.get(position);
		else
			collection = mCollectionsDate.get(position);

		// launch the intent for the online collection viewer
		Intent intent = new Intent(this, OnlineCollectionViewer.class);

		intent.putExtra(OnlineCollectionViewer.INTENT_GLOBALID, collection
				.getGlobalID());
		intent.putExtra(OnlineCollectionViewer.INTENT_AUTHOR_NAME, collection
				.getAuthorName());
		intent.putExtra(OnlineCollectionViewer.INTENT_DESCRIPTION, collection
				.getDescription());
		intent.putExtra(OnlineCollectionViewer.INTENT_DOWNLOAD_COUNT,
				collection.getDownloadCount());
		intent.putExtra(OnlineCollectionViewer.INTENT_DOWNLOAD_SIZE, collection
				.getDownloadSize());
		intent.putExtra(OnlineCollectionViewer.INTENT_RATING, collection
				.getRating());
		intent.putExtra(OnlineCollectionViewer.INTENT_TITLE, collection
				.getTitle());
		intent.putExtra(OnlineCollectionViewer.INTENT_UPLOAD_TIME, collection
				.getDateUploaded());
		intent.putExtra(OnlineCollectionViewer.INTENT_RATING_COUNT, collection
				.getRatingCount());

		this.startActivityForResult(intent, ACTIVITY_SHOW_COLLECTION);
	}

	/**
	 * 
	 * Custom adapter for {@link ListView}s that goes off and downloads extra
	 * collections when user scrolls to the bottom.
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	private class CollectionFeedAdapter extends BaseAdapter implements
			Runnable, Filterable {
		private ArrayList<OnlineCollection> mItemsShown;

		private static final int MESSAGE_NEW_COLLECTION = 0;
		private static final int MESSAGE_FINISHED = 1;
		private static final int MESSAGE_STARTED = 2;
		private static final int MESSAGE_CLEARED = 3;
		private static final int DOWNLOAD_FAILED = 4;

		private static final String MESSAGE_BUNDLE_GLOBALID = "globalID";
		private static final String MESSAGE_BUNDLE_TITLE = "title";
		private static final String MESSAGE_BUNDLE_DESCRIPTION = "description";
		private static final String MESSAGE_BUNDLE_AUTHOR_NAME = "authorName";
		private static final String MESSAGE_BUNDLE_RATING = "rating";
		private static final String MESSAGE_BUNDLE_DOWNLOAD_COUNT = "downloads";
		private static final String MESSAGE_BUNDLE_UPLOAD_TIME = "uploadTime";
		private static final String MESSAGE_BUNDLE_DOWNLOAD_SIZE = "downloadSize";
		private static final String MESSAGE_BUNDLE_THREAD_STRING = "thread";
		private static final String MESSAGE_BUNDLE_RATING_COUNT = "ratingCount";

		private static final String XML_ATTRIBUTE_GLOBALID = "globalID";
		private static final String XML_ATTRIBUTE_TITLE = "title";
		private static final String XML_ATTRIBUTE_DESCRIPTION = "description";
		private static final String XML_ATTRIBUTE_AUTHOR_NAME = "authorName";
		private static final String XML_ATTRIBUTE_RATING = "rating";
		private static final String XML_ATTRIBUTE_DOWNLOAD_COUNT = "downloads";
		private static final String XML_ATTRIBUTE_UPLOAD_TIME = "uploadTime";
		private static final String XML_ATTRIBUTE_DOWNLOAD_SIZE = "downloadSize";
		private static final String XML_ATTRIBUTE_RATING_COUNT = "ratingCount";

		private static final String HTTP_REQUEST_FROM = "from";
		private static final String HTTP_REQUEST_TO = "to";
		private static final String HTTP_REQUEST_ORDER_BY = "order_by";
		private static final String HTTP_REQUEST_KEYWORD = "keyword";

		public static final String ORDER_BY_DATE = "date";
		public static final String ORDER_BY_POPULARITY = "popularity";

		private XMLReader mReader;

		private boolean mThreadRunning = false;
		private volatile Thread mThread;

		private String orderBy;
		
		private boolean dataSetChangeDeferred = false;

		private boolean mGotMore = true;

		private int mAddedThisSession = 0;

		private Filter mFilter = new MyFilter();

		private static final int ATTEMPT_LIMIT = 10;

		private int mAttempt = 0;
		
		protected void dataSetChangeOnActivation()
		{
			if (dataSetChangeDeferred)
			{
				this.notifyDataSetChanged();
				dataSetChangeDeferred = false;
			}
				
		}
		
		private class MyFilter extends Filter {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				MyLog.d(LOG_TAG, "performFiltering() called - " + constraint);
				mGotMore = true;
				stopThread();

				// invalidate data set
				mItemsShown.clear();
				mHandler.sendEmptyMessage(MESSAGE_CLEARED);

				mKeyword = constraint.toString();

				// start new thread to download things
				startDownloading();

				Filter.FilterResults results = new Filter.FilterResults();

				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
			}

		}

		private Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int messageCode = msg.what;

				switch (messageCode) {
				case MESSAGE_NEW_COLLECTION:
					// read off the collection data from bundle and add it to
					// the collection

					Bundle collectionData = msg.getData();

					// ignore the data if it comes from some other threads which
					// have not been stopped properly for some reason
					if (mThread != null) {
						if (collectionData.getString(
								MESSAGE_BUNDLE_THREAD_STRING).equals(
								mThread.toString())) {
							OnlineCollection newCollection = new OnlineCollection(
									collectionData
											.getLong(MESSAGE_BUNDLE_GLOBALID),
									collectionData
											.getString(MESSAGE_BUNDLE_TITLE),
									collectionData
											.getString(MESSAGE_BUNDLE_DESCRIPTION),
									collectionData
											.getString(MESSAGE_BUNDLE_AUTHOR_NAME),
									collectionData
											.getInt(MESSAGE_BUNDLE_RATING),
									collectionData
											.getInt(MESSAGE_BUNDLE_DOWNLOAD_COUNT),
									collectionData
											.getFloat(MESSAGE_BUNDLE_DOWNLOAD_SIZE),
									collectionData
											.getLong(MESSAGE_BUNDLE_UPLOAD_TIME),
									collectionData
											.getInt(MESSAGE_BUNDLE_RATING_COUNT));
							mItemsShown.add(newCollection);
							mAddedThisSession++;
						}
					}

					break;
				case MESSAGE_FINISHED:

					// check if the given list is actually active as otherwise
					// notifyDataSetChanged makes the inactive list appear in
					// the background and everything looks ugly...
					if ((DownloadActivity.this.getTabHost().getCurrentTabTag()
							.equals(TAB_BY_POPULARITY) && orderBy
							.equals(CollectionFeedAdapter.ORDER_BY_POPULARITY))
							|| (DownloadActivity.this.getTabHost()
									.getCurrentTabTag().equals(TAB_BY_DATE) && orderBy
									.equals(CollectionFeedAdapter.ORDER_BY_DATE)))
						CollectionFeedAdapter.this.notifyDataSetChanged();
					else
						dataSetChangeDeferred = true;

					DownloadActivity.this
							.setProgressBarIndeterminateVisibility(false);

					MyLog.d(LOG_TAG, "Added " + mAddedThisSession);

					if (mAddedThisSession < COUNT_LOAD_INCREMENT)
						mGotMore = false;

					mThreadRunning = false;

					break;
				case MESSAGE_STARTED:
					DownloadActivity.this
							.setProgressBarIndeterminateVisibility(true);

					mAddedThisSession = 0;

					break;
				case MESSAGE_CLEARED:
					CollectionFeedAdapter.this.notifyDataSetInvalidated();
					break;
				case DOWNLOAD_FAILED:
					if (mThread != null) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								DownloadActivity.this);
						builder
								.setMessage(R.string.alert_network_error_message);
						builder.setTitle(R.string.alert_network_error_title);
						builder.setPositiveButton(R.string.retry,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										mAttempt = 0;
										CollectionFeedAdapter.this
												.startDownloading();
									}
								});

						builder.setNegativeButton(R.string.cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										DownloadActivity.this.finish();
									}
								});

						builder.show();

						mThreadRunning = false;

						break;
					}
				}
			}
		};

		public CollectionFeedAdapter(ArrayList<OnlineCollection> mItemsShown,
				String orderBy, String keyword) {
			super();
			this.mItemsShown = mItemsShown;
			this.orderBy = orderBy;

			try {
				// initiate factory and parser
				SAXParserFactory parsingFactory = SAXParserFactory
						.newInstance();
				SAXParser parser = parsingFactory.newSAXParser();

				// get the reader
				mReader = parser.getXMLReader();

				// create a new content handler and pass it onto the reader
				CollectionFeedHandler handler = new CollectionFeedHandler();
				mReader.setContentHandler(handler);
			} catch (Exception e) {
				MyLog.e(LOG_TAG,
						"Exception caught while initializing XML parser");
			}
		}

		public synchronized void stopThread() {
			if (mThread != null) {
				mThread.interrupt();
				mThread = null;
				mThreadRunning = false;
			}
		}

		@Override
		public void run() {
			// load the extra stuff
			if (mGotMore) {
				mHandler.sendEmptyMessage(MESSAGE_STARTED);

				if (this.getExtra())
					mHandler.sendEmptyMessage(MESSAGE_FINISHED);
			}
		}

		private synchronized void startDownloading() {
			MyLog.d(LOG_TAG, "startDownloading() called");

			if (!mThreadRunning) {
				mThread = new Thread(this);
				mThreadRunning = true;
				mThread.start();
			}
		}

		@Override
		public int getCount() {
			return mItemsShown.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		class ViewHolder {
			TextView topText;
			TextView botText;
			RatingBar ratingBar;
		}

		@Override
		public boolean isEmpty() {
			return this.getCount() == 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder viewHolder;

			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.onlinecollectionrow, null);

				// instantiate ViewHolder
				viewHolder = new ViewHolder();
				viewHolder.topText = (TextView) view.findViewById(R.id.toptext);

				viewHolder.botText = (TextView) view
						.findViewById(R.id.bottomtext);

				viewHolder.ratingBar = (RatingBar) view
						.findViewById(R.id.ratingbar);

				view.setTag(viewHolder);

			} else {
				view = convertView;
				viewHolder = (ViewHolder) convertView.getTag();
			}

			// get the collection
			OnlineCollection collection = mItemsShown.get(position);
			if (collection != null) {
				viewHolder.topText.setText(collection.getTitle());

				viewHolder.botText.setText(collection.getDescription());

				viewHolder.ratingBar.setRating((float) collection.getRating());
			}
			return view;
		}

		private boolean getExtra() {
			try {
				MultipartEntity form = new MultipartEntity();

				if (mKeyword != null)
					form
							.addPart(HTTP_REQUEST_KEYWORD, new StringBody(
									mKeyword));

				int currentCount = mItemsShown.size();

				MyLog.d(LOG_TAG, "Downloading from - " + currentCount);

				form.addPart(HTTP_REQUEST_FROM, new StringBody(String
						.valueOf(currentCount)));

				if (currentCount == 0)
					form.addPart(HTTP_REQUEST_TO, new StringBody(String
							.valueOf(DownloadActivity.COUNT_DOWNLOAD_FIRST)));
				else
					form.addPart(HTTP_REQUEST_TO, new StringBody(String
							.valueOf(currentCount
									+ DownloadActivity.COUNT_LOAD_INCREMENT)));

				form.addPart(HTTP_REQUEST_ORDER_BY, new StringBody(orderBy));

				// read the response of the web server
				InputStream iStream = ServerHelper.obtainInputStream(
						URL_COLLECTION_LIST, form);

				mReader.parse(new InputSource(iStream));

				mAttempt = 0;

				return true;
			} catch (Exception e) {
				MyLog.e(LOG_TAG, "Error in XML parsing - " + e.toString());

				e.printStackTrace();

				MyLog.e(LOG_TAG, "Restarting in 0.5 second");

				if (mAttempt < ATTEMPT_LIMIT) {
					mAttempt++;

					try {
						Thread.sleep(500);
						return this.getExtra();
					} catch (Exception e1) {
						e1.printStackTrace();
						return false;
					}
				} else {
					MyLog.d(LOG_TAG,
							"Attempt limit reached - showing dialog box");

					mHandler.sendEmptyMessage(DOWNLOAD_FAILED);
					mAttempt = 0;

					return false;
				}

			}
		}

		/**
		 * Inner class for XML parsing of the collections
		 * 
		 * @author Vytautas Vaitukaitis
		 * 
		 */
		class CollectionFeedHandler extends DefaultHandler {
			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
			}

			@Override
			public void endDocument() throws SAXException {
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
			}

			@Override
			public void startDocument() throws SAXException {
			}

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				if (localName.equals("collection")) {
					// get the attributes and send the message to the handler
					Bundle bundle = new Bundle();
					
					bundle.putLong(MESSAGE_BUNDLE_GLOBALID, Long
							.parseLong(attributes
									.getValue(XML_ATTRIBUTE_GLOBALID)));

					bundle.putString(MESSAGE_BUNDLE_TITLE, attributes
							.getValue(XML_ATTRIBUTE_TITLE));

					bundle.putString(MESSAGE_BUNDLE_DESCRIPTION, attributes
							.getValue(XML_ATTRIBUTE_DESCRIPTION));

					bundle.putString(MESSAGE_BUNDLE_AUTHOR_NAME, attributes
							.getValue(XML_ATTRIBUTE_AUTHOR_NAME));

					bundle.putInt(MESSAGE_BUNDLE_RATING,
							Integer.parseInt(attributes
									.getValue(XML_ATTRIBUTE_RATING)));

					bundle.putInt(MESSAGE_BUNDLE_DOWNLOAD_COUNT, Integer
							.parseInt(attributes
									.getValue(XML_ATTRIBUTE_DOWNLOAD_COUNT)));

					bundle.putFloat(MESSAGE_BUNDLE_DOWNLOAD_SIZE, Float
							.parseFloat(attributes
									.getValue(XML_ATTRIBUTE_DOWNLOAD_SIZE)));

					bundle.putLong(MESSAGE_BUNDLE_UPLOAD_TIME, Long
							.parseLong(attributes
									.getValue(XML_ATTRIBUTE_UPLOAD_TIME)));

					bundle.putString(MESSAGE_BUNDLE_THREAD_STRING, Thread
							.currentThread().toString());

					bundle.putInt(MESSAGE_BUNDLE_RATING_COUNT, Integer
							.parseInt(attributes
									.getValue(XML_ATTRIBUTE_RATING_COUNT)));

					Message msg = Message.obtain();
					msg.what = MESSAGE_NEW_COLLECTION;
					msg.setData(bundle);
					
					
					mHandler.sendMessage(msg);
				}
			}

		}

		@Override
		public Filter getFilter() {
			return mFilter;
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			// show the search bar
			showSearchDialog();

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// only need to add items if menu has not been populated yet
		if (menu.size() == 0) {
			MenuItem searchButton = menu.add(R.string.search);
			searchButton.setIcon(android.R.drawable.ic_menu_search);
			searchButton
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem arg0) {
							showSearchDialog();

							// the event doesn't have to be passed through, so
							// return true
							return true;
						}
					});
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Shows search dialog.
	 * 
	 */
	private void showSearchDialog() {
		final AlertDialog.Builder searchDialog = new AlertDialog.Builder(this);
		searchDialog.setTitle(R.string.search);

		final EditText searchField = new EditText(this);
		searchField.setText(mKeyword);
		searchField.setSingleLine();

		ImageButton iButton = new ImageButton(this);
		iButton.setImageResource(R.drawable.ic_btn_search);

		LinearLayout mainHolder = new LinearLayout(this);
		mainHolder.setOrientation(LinearLayout.HORIZONTAL);
		mainHolder.setGravity(Gravity.CENTER);

		mainHolder.addView(searchField, new ViewGroup.LayoutParams(200,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		mainHolder.addView(iButton, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		mainHolder.setPadding(5, 5, 5, 5);

		// set up the search button listener
		iButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String tabTag = DownloadActivity.this.getTabHost()
						.getCurrentTabTag();
				String searchString = searchField.getEditableText().toString();

				if (tabTag.equals(DownloadActivity.TAB_BY_DATE)) {
					mListDate.setFilterText(searchString);
				} else if (tabTag.equals(DownloadActivity.TAB_BY_POPULARITY)) {
					mListPopularity.setFilterText(searchString);
				}

				// hide the soft keyboard
				InputMethodManager imm = (InputMethodManager) DownloadActivity.this
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

				if (mAlertDialog != null)
					mAlertDialog.dismiss();
			}
		});

		searchDialog.setView(mainHolder);

		mAlertDialog = searchDialog.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mAdapterDate != null)
			mAdapterDate.stopThread();
		if (mAdapterPopularity != null)
			mAdapterPopularity.stopThread();
	}

}
