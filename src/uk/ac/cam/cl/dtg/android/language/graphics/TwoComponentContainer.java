package uk.ac.cam.cl.dtg.android.language.graphics;

import uk.ac.cam.cl.dtg.android.language.L;
import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 
 * Class representing two component (2:1) container.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TwoComponentContainer extends Container
{
	private final static String LOG_TAG = "TwoComponentContainer";

	public TwoComponentContainer(Activity c)
	{
		super(c);
		mComponents = new Component[2];
		mFrames = new ViewGroup[2];
	}

	@Override
	public View drawContainer()
	{
		L.i(LOG_TAG, "Starting drawing a two component container");

		mView.setOrientation(LinearLayout.VERTICAL);
		mView.setGravity(Gravity.CENTER);
		
		// set the drawing parameters - fill the parent so that it could center
		// it up within the frame
		defaultParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

		// create a frame for the first component and add it onto the main
		// container
		FrameLayout firstComponent = new FrameLayout(mContext);
		firstComponent.setPadding(0, 0, 0, PADDING);
		
		mView.addView(firstComponent,
				new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 2));

		Component c = obtainComponent(0);
		try
		{
			c.drawYourselfOnto(firstComponent, defaultParams);
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Exception caught while drawing component 0 - " + e.getMessage());
		}

		// layout the 2nd component
		FrameLayout secondComponent = new FrameLayout(mContext);
		secondComponent.setPadding(0, 0, 0, PADDING);
		mView.addView(secondComponent,
				new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 1));

		c = obtainComponent(1);
		try
		{
			c.drawYourselfOnto(secondComponent, defaultParams);
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Exception caught while drawing component 1 - " + e.getMessage());
		}

		mFrames[0] = firstComponent;
		mFrames[1] = secondComponent;

		L.i(LOG_TAG, "Finished drawing a two component container");

		return mView;
	}

}
