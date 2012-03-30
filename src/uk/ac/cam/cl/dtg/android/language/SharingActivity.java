package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * Activity for sharing - responsible for showing the menu with Download, Upload
 * and Check for Updates. In version 1.0 it was responsible for logging in and
 * registration. In version 1.1, these were moved to the Upload activity as the
 * login is not necessary for downloading anymore.
 * 
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class SharingActivity extends ListActivity
{
	private final static int ACTIVITY_DOWNLOAD = 0;
	private final static int ACTIVITY_UPLOAD = 1;
	private final static int ACTIVITY_UPDATE = 2;

	private final static int TITLE_TEXT_SIZE = 20;
	private final static int DESC_TEXT_SIZE = 17;

	@Override
  public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
    	ApplicationInitializer initializer = new ApplicationInitializer(this);
    	if (initializer.initializeActivity())
		{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			showMenu();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent;

		switch (position)
		{
		case 0:
			intent = new Intent(this, DownloadActivity.class);
			startActivityForResult(intent, 1);
			break;
		case 1:
			intent = new Intent(this, UploadActivity.class);
			startActivityForResult(intent, 0);
			break;
		case 2:
			ApplicationInitializer appInit = new ApplicationInitializer(this);
			appInit.checkForUpdates(true);
			break;
		}

		super.onListItemClick(l, v, position, id);
	}

	
	private class MenuItem
	{
		private Bitmap icon;
		private String title, description;

		public MenuItem(Bitmap ic, String t, String desc)
		{
			icon = ic;
			title = t;
			description = desc;
		}

		public Bitmap getIcon()
		{
			return icon;
		}

		public String getTitle()
		{
			return title;
		}

		public String getDescription()
		{
			return description;
		}
	}

	private class MenuAdapter extends ArrayAdapter<MenuItem>
	{

		private ArrayList<MenuItem> items;

		public MenuAdapter(Context context, int textViewResourceId, ArrayList<MenuItem> items)
		{
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = convertView;
			if (v == null)
			{
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.menurow, null);
			}

			MenuItem o = items.get(position);
			if (o != null)
			{
				ImageView iconView = (ImageView) v.findViewById(R.id.icon);
				TextView topText = (TextView) v.findViewById(R.id.toptext);
				TextView botText = (TextView) v.findViewById(R.id.bottomtext);

				if (iconView != null)
				{
					iconView.setImageBitmap(o.getIcon());
				}

				if (topText != null)
				{
					topText.setTextSize(TITLE_TEXT_SIZE);
					topText.setText(o.getTitle());
				}

				if (botText != null)
				{
					botText.setTextSize(DESC_TEXT_SIZE);
					botText.setText(o.getDescription());
				}
			}
			return v;
		}
	}
	
	/**
	 * Method to show the menu to choose from Download, Upload and Updates.
	 * 
	 */
	private void showMenu()
	{
		final String STRING_UPLOAD = getString(R.string.upload);
		final String STRING_DOWNLOAD = getString(R.string.download);
		final String STRING_CHECK_FOR_UPDATES = getString(R.string.check_for_updates);

		final String STRING_UPLOAD_DESC = getString(R.string.uploadDesc);
		final String STRING_DOWNLOAD_DESC = getString(R.string.downloadDesc);
		final String STRING_UPDATES_DESC = getString(R.string.check_for_updates_desc);

		ArrayList<MenuItem> menuOptions = new ArrayList<MenuItem>();

		Resources res = getResources();

		menuOptions.add(ACTIVITY_DOWNLOAD, new MenuItem(BitmapFactory.decodeResource(res,
				R.drawable.download), STRING_DOWNLOAD, STRING_DOWNLOAD_DESC));
		menuOptions.add(ACTIVITY_UPLOAD, new MenuItem(BitmapFactory.decodeResource(res,
				R.drawable.upload), STRING_UPLOAD, STRING_UPLOAD_DESC));
		menuOptions.add(ACTIVITY_UPDATE, new MenuItem(BitmapFactory.decodeResource(res,
				R.drawable.update), STRING_CHECK_FOR_UPDATES, STRING_UPDATES_DESC));

		MenuAdapter menuAdapter = new MenuAdapter(this, R.layout.menurow, menuOptions);
		setListAdapter(menuAdapter);
		getListView().setTextFilterEnabled(true);
	}

}
