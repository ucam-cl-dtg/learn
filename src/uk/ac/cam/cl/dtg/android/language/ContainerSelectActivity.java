package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import uk.ac.cam.cl.dtg.android.language.graphics.Container;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * Activity for selecting a suitable container for a newly created card.
 * 
 * <b>Note:</b> probably needs to be turned into activity with simple
 * {@link LinearLayout} as overheads of having {@link ListActivity} might be
 * too big.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ContainerSelectActivity extends ListActivity
{
	/**
	 * {@link Intent} string code for the container type returned 
	 */
	public final static String INTENT_CONTAINER_TYPE_ID = "containerType";

	private final int TITLE_TEXT_SIZE = 18;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		final int ICON_ONECOMPONENT = R.drawable.onecomponent, ICON_TWOCOMPONENT = R.drawable.twocomponenttwotoone, ICON_TWOEQUALCOMPONENT = R.drawable.twocomponent, ICON_THREECOMPONENT = R.drawable.threecomponent;

		final String TITLE_ONECOMPONENT = getString(R.string.oneComponent), TITLE_TWOCOMPONENT = getString(R.string.twoComponents), TITLE_TWOEQUALCOMPONENT = getString(R.string.twoEqualComponents), TITLE_THREECOMPONENT = getString(R.string.threeComponents);

		// draw the menu for choosing the layout

		ArrayList<MenuItem> menuOptions = new ArrayList<MenuItem>();

		Resources res = getResources();

		menuOptions.add(
				Container.CONTAINER_ONECOMPONENT,
				new MenuItem(BitmapFactory.decodeResource(res, ICON_ONECOMPONENT), TITLE_ONECOMPONENT));
		menuOptions.add(
				Container.CONTAINER_TWOCOMPONENT,
				new MenuItem(BitmapFactory.decodeResource(res, ICON_TWOCOMPONENT), TITLE_TWOCOMPONENT));
		menuOptions.add(
				Container.CONTAINER_TWOCOMPONENTEQUAL,
				new MenuItem(BitmapFactory.decodeResource(res, ICON_TWOEQUALCOMPONENT), TITLE_TWOEQUALCOMPONENT));
		menuOptions.add(
				Container.CONTAINER_THREECOMPONENT,
				new MenuItem(BitmapFactory.decodeResource(res, ICON_THREECOMPONENT), TITLE_THREECOMPONENT));

		MenuAdapter menuAdapter = new MenuAdapter(this, R.layout.containerrow, menuOptions);
		setListAdapter(menuAdapter);
		getListView().setTextFilterEnabled(true);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		Intent intent = new Intent();
		intent.putExtra(INTENT_CONTAINER_TYPE_ID, position);

		this.setResult(RESULT_OK, intent);
		this.finish();
	}

	private class MenuItem
	{
		private Bitmap icon;
		private String title;

		public MenuItem(Bitmap ic, String t)
		{
			icon = ic;
			title = t;
		}

		public Bitmap getIcon()
		{
			return icon;
		}

		public String getTitle()
		{
			return title;
		}
	}
	
	/**
	 * 
	 * Custom adapter for the list view.
	 * 
	 * @author Vytautas Vaitukaitis
	 *
	 */
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
				v = vi.inflate(R.layout.containerrow, null);
			}

			MenuItem o = items.get(position);
			if (o != null)
			{
				ImageView iconView = (ImageView) v.findViewById(R.id.containericon);
				TextView topText = (TextView) v.findViewById(R.id.containertitle);

				if (iconView != null)
				{
					iconView.setImageBitmap(o.getIcon());
				} else
					MyLog.d("CreationActivity", "Container icon is null...");

				if (topText != null)
				{
					topText.setTextSize(TITLE_TEXT_SIZE);
					topText.setText(o.getTitle());
				} else
					MyLog.d("CreationActivity", "Container top text is null...");

			} else
				MyLog.d("CreationActivity", "Menu object is null...");
			return v;
		}
	}

}
