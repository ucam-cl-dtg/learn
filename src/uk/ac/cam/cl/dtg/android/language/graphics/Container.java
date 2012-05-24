package uk.ac.cam.cl.dtg.android.language.graphics;

import uk.ac.cam.cl.dtg.android.language.L;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Class that represents any container that holds a number of components.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public abstract class Container
{
	public static final int CONTAINER_ONECOMPONENT = 0, CONTAINER_TWOCOMPONENT = 1,
			CONTAINER_TWOCOMPONENTEQUAL = 2, CONTAINER_THREECOMPONENT = 3;
	
	public static final int[] CONTAINER_ELEMENT_COUNT =
	{ 1, 2, 2, 3 };

	private static final String LOG_TAG = "Container";
	
	protected LinearLayout mView;
	
	protected static int PADDING = 5;
	
	protected ViewGroup[] mFrames;
	protected Activity mContext;
	protected Component[] mComponents;
	protected ViewGroup.LayoutParams defaultParams;

	public Container(Activity c)
	{
		mContext = c;
		
		mView = new LinearLayout(c);
		
		// each frame is responsible for adding some padding underneath itself
		mView.setPadding(PADDING, PADDING, PADDING, 0);
		mView.setBaselineAligned(false);		
	}

	/**
	 * 
	 * Method to fill the given place with a given component.
	 * 
	 * @param id
	 *            ID of the component to be filled
	 * @param comp
	 *            component object that has to be put in there
	 */
	public void fillComponent(int id, Component comp)
	{
		try
		{
			mComponents[id] = comp;
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Exception while adding component onto the array - " + e.getMessage());
		}
	}

	/**
	 * Method that renders the given container and returns the view.
	 * 
	 * @return a view that all the components have been drawn on
	 */
	public abstract View drawContainer();

	protected boolean isViewNotNull(View v)
	{
		return v != null;
	}

	protected Component obtainComponent(int id)
	{
		try
		{
			return mComponents[id];
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Unable to obtain the component that has id " + id
					+ " from the components array list - returning null");
			return null;
		}
	}

	public void removeViewsFromFrame(int id)
	{
		if (mFrames[id] != null)
			mFrames[id].removeAllViews();
	}

	public void removeAllViews()
	{
		mView.removeAllViews();
		for (ViewGroup v : mFrames)
			v.removeAllViews();
	}

	public ViewGroup getFrame(int id)
	{
		return mFrames[id];
	}

	public void stopComponents()
	{
		for (Component c : mComponents)
		{
			if (c != null)
				c.stop();
		}
	}
}
