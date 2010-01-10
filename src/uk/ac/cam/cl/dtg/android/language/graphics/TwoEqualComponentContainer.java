package uk.ac.cam.cl.dtg.android.language.graphics;

import uk.ac.cam.cl.dtg.android.language.MyLog;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 
 * Class that represents two equal component layout.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class TwoEqualComponentContainer extends Container
{
	private final static String LOG_TAG = "TwoEqualComponentContainer";

	public TwoEqualComponentContainer(Activity c)
	{
		super(c);
		mComponents = new Component[2];
		mFrames = new ViewGroup[2];
	}

	@Override
	public View drawContainer()
	{
		int orientation = mContext.getResources().getConfiguration().orientation;

		mView.setGravity(Gravity.CENTER);

		FrameLayout firstComponent = new FrameLayout(mContext), secondComponent = new FrameLayout(mContext);

		ViewGroup.LayoutParams firstCompParams, secondCompParams;

		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			mView.setOrientation(LinearLayout.VERTICAL);

			// set out the layout parameters for the first and second frames
			firstCompParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 1);
			secondCompParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0, 1);
			
			firstComponent.setPadding(0, 0, 0, PADDING);
			secondComponent.setPadding(0, 0, 0, PADDING);
		} else
		{
			mView.setOrientation(LinearLayout.HORIZONTAL);

			firstCompParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT, 1);
			secondCompParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT, 1);
			
			firstComponent.setPadding(0, 0, PADDING / 2, PADDING);
			secondComponent.setPadding(PADDING / 2, 0, 0, PADDING);
		}

		// add the frames onto the main container
		mView.addView(firstComponent, firstCompParams);
		mView.addView(secondComponent, secondCompParams);

		mFrames[0] = firstComponent;
		mFrames[1] = secondComponent;

		defaultParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

		// try to obtain components and draw them onto the frames
		Component c = obtainComponent(0);
		try
		{
			c.drawYourselfOnto(firstComponent, defaultParams);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while drawing component 0 - " + e.getMessage());
		}

		c = obtainComponent(1);
		try
		{
			c.drawYourselfOnto(secondComponent, defaultParams);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while drawing component 1 - " + e.getMessage());
		}

		return mView;

	}

}
