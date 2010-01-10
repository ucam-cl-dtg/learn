package uk.ac.cam.cl.dtg.android.language.graphics;

import uk.ac.cam.cl.dtg.android.language.MyLog;
import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * Class that represents single component container.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class SingleComponentContainer extends Container
{

	private static final String LOG_TAG = "SingleComponentContainer";

	public SingleComponentContainer(Activity c)
	{
		super(c);
		mComponents = new Component[1];
		mFrames = new ViewGroup[1];
	}

	@Override
	public View drawContainer()
	{
		MyLog.i(LOG_TAG, "Starting to draw a single component container");

		// set the empty linear container as a content view
		
		mView.setGravity(Gravity.CENTER);
		
		// this is the same view, so set all paddings
		mView.setPadding(PADDING, PADDING, PADDING, PADDING);
		
		mFrames[0] = mView;

		// obtain the component view
		Component c = obtainComponent(0);

		// specify the layout parameters
		defaultParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

		// tell the component 0 to draw itself onto the main View
		try
		{
			c.drawYourselfOnto(mView, defaultParams);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while drawing element on component 0 - "
					+ e.getMessage());
		}

		MyLog.i(LOG_TAG, "Finished drawing a single component container");

		return mView;
	}

}
