package uk.ac.cam.cl.dtg.android.language;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * 
 * {@link Activity} that shows the menu. Starts up when clicked on the Learn! launcher icon.
 * 
 * @author Vytautas
 *
 */
public class MenuActivity extends ListActivity
{
	private static final int ACTIVITY_LEARN = 0;
	private static final int ACTIVITY_CREATE = 1;
	private static final int ACTIVITY_SHARE = 2;
	private static final int ACTIVITY_SETTINGS = 3;
	private static final int ACTIVITY_HELP = 4;
	
	private static final int TITLE_TEXT_SIZE = 20;
	private static final int DESC_TEXT_SIZE = 17;
	
	private GoogleAnalyticsTracker mTracker;
	
    @Override  
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	
    	mTracker = GoogleAnalyticsTracker.getInstance();
    	mTracker.start("UA-11566223-1", 20, this);    	
    	mTracker.trackPageView("/MenuActivity");
    	   	
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	
    	// initialize the application
    	ApplicationInitializer appInit = new ApplicationInitializer(this);
    	if (appInit.initializeApp())
    	{
    		// and if the automatically check updates is set
    		
    		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    		
    		boolean autoUpdate = preferences.getBoolean(getString(R.string.preferences_code_update_collections), false);
    		
    		if (autoUpdate)
    		{
    			// no need to show toast if no updates are available
    			appInit.checkForUpdates(false);
    		}    		
    	}
    	
    	final String STRING_LEARN = getString(R.string.menuLearn);
    	final String STRING_CREATE = getString(R.string.menuCreate);
    	final String STRING_SHARE = getString(R.string.menuShare);
    	final String STRING_SETTINGS = getString(R.string.menuSettings);
    	final String STRING_HELP = getString(R.string.menuHelp);
    	
    	final String STRING_LEARN_DESC = getString(R.string.menuLearnDesc);
    	final String STRING_CREATE_DESC = getString(R.string.menuCreateDesc);
    	final String STRING_SHARE_DESC = getString(R.string.menuShareDesc);
    	final String STRING_SETTINGS_DESC = getString(R.string.menuSettingsDesc);
    	final String STRING_HELP_DESC = getString(R.string.menuHelpDesc);
    	
    	ArrayList<MenuItem> menuOptions = new ArrayList<MenuItem>();
      
    	Resources res = getResources();
    	
    	menuOptions.add(ACTIVITY_LEARN, 
    			new MenuItem(BitmapFactory.decodeResource(res, R.drawable.learn), STRING_LEARN, STRING_LEARN_DESC));
    	menuOptions.add(ACTIVITY_CREATE,
    			new MenuItem(BitmapFactory.decodeResource(res, R.drawable.create), STRING_CREATE, STRING_CREATE_DESC));
    	menuOptions.add(ACTIVITY_SHARE,
    			new MenuItem(BitmapFactory.decodeResource(res, R.drawable.share), STRING_SHARE, STRING_SHARE_DESC));
    	menuOptions.add(ACTIVITY_SETTINGS,
    			new MenuItem(BitmapFactory.decodeResource(res, R.drawable.settings), STRING_SETTINGS, STRING_SETTINGS_DESC));
    	menuOptions.add(ACTIVITY_HELP,
    			new MenuItem(BitmapFactory.decodeResource(res, R.drawable.help), STRING_HELP, STRING_HELP_DESC));
      
    	MenuAdapter menuAdapter = new MenuAdapter(this, R.layout.menurow, menuOptions);
    	setListAdapter(menuAdapter);
    	getListView().setTextFilterEnabled(false);
    	
    }
  
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id)
    {  
    	// intent for starting the applications
    	Intent intent;
      
    	switch (position)
        {
        case ACTIVITY_LEARN:
        	intent = new Intent(this, LearningActivity.class); 
        	startActivityForResult(intent, ACTIVITY_LEARN);
        	break;
        case ACTIVITY_CREATE:
        	intent = new Intent(this, CollectionBrowser.class);
        	
        	intent.putExtra(CollectionBrowser.INTENT_ACTION, CollectionBrowser.INTENT_ACTION_PICK_FOR_EDITING);
       	
        	startActivityForResult(intent, ACTIVITY_CREATE);
        	break;
        case ACTIVITY_SHARE:
        	intent = new Intent(this, SharingActivity.class);
        	startActivityForResult(intent, ACTIVITY_SHARE);
        	break;
        case ACTIVITY_SETTINGS:
        	intent = new Intent(this, SettingsActivity.class);
        	startActivityForResult(intent,ACTIVITY_SETTINGS);
        	break;        
        case ACTIVITY_HELP:
        	
        	// launch the help web page
        	intent = new Intent(Intent.ACTION_VIEW);
        	intent.setData(Uri.parse(ServerHelper.URL_HELP_PAGE));
        	startActivity(intent);

        	break;
        }
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
            	LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		mTracker.stop();
	}
    
    
}