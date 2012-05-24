package uk.ac.cam.cl.dtg.android.language.graphics;

import uk.ac.cam.cl.dtg.android.language.L;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 
 * Class that represents 3 component container.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ThreeComponentContainer extends Container {
	private final static String LOG_TAG = "ThreeComponentContainer";

	public ThreeComponentContainer(Activity c) {
		super(c);
		mComponents = new Component[3];
		mFrames = new ViewGroup[3];
	}

	@Override
	public View drawContainer() {
		// set up the main layout
		mView.setOrientation(LinearLayout.VERTICAL);

		// get screen orientation first
		int orientation = mContext.getResources().getConfiguration().orientation;

		FrameLayout firstComponent = new FrameLayout(mContext), secondComponent = new FrameLayout(
				mContext), thirdComponent = new FrameLayout(mContext);
		
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			// layout the components as if they were in portrait
			mView.addView(firstComponent, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 0, 1));

			// layout the 2nd component
			mView.addView(secondComponent, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 0, 1));

			// layout the 3nd component
			mView.addView(thirdComponent, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 0, 1));
			
			firstComponent.setPadding(0, 0, 0, PADDING);
			secondComponent.setPadding(0, 0, 0, PADDING);
			thirdComponent.setPadding(0, 0, 0, PADDING);

		} else {
			// set up the horizontal view for the first two frames
			LinearLayout horizontalView = new LinearLayout(mContext);

			horizontalView.addView(firstComponent, new LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.FILL_PARENT, 1));

			// layout the 2nd component
			horizontalView.addView(secondComponent, new LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.FILL_PARENT, 1));

			// add the horizontal view
			mView.addView(horizontalView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					0, 2));

			// layout the 3nd component
			mView.addView(thirdComponent, new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 0, 1));
			
			firstComponent.setPadding(0, 0, PADDING / 2, PADDING);
			secondComponent.setPadding(PADDING / 2, 0, 0, PADDING);
			thirdComponent.setPadding(0, 0, 0, PADDING);

		}

		mFrames[0] = firstComponent;
		mFrames[1] = secondComponent;
		mFrames[2] = thirdComponent;

		// set up the default frame filling parameters
		defaultParams = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);

		// fill the first frame layout
		Component c = obtainComponent(0);
		try {
			c.drawYourselfOnto(firstComponent, defaultParams);
		} catch (Exception e) {
			L
					.e(LOG_TAG,
							"Cannot draw the component 0 onto the container - leaving the frame blank");
		}

		// fill the second frame layout
		c = obtainComponent(1);
		try {
			c.drawYourselfOnto(secondComponent, defaultParams);
		} catch (Exception e) {
			L
					.e(LOG_TAG,
							"Cannot draw the component 1 onto the container - leaving the frame blank");
		}
		// fill the third one
		c = obtainComponent(2);
		try {
			c.drawYourselfOnto(thirdComponent, defaultParams);
		} catch (Exception e) {
			L
					.e(LOG_TAG,
							"Cannot draw the component 2 onto the container - leaving the frame blank");
		}

		return mView;
	}
}
