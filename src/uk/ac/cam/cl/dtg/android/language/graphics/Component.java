package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * Class that represents any component of a card.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public abstract class Component
{
	protected View mView;
	protected int mWidth = 0, mHeight = 0;

	public Component()
	{

	}

	/**
	 * Method to get the view of the component.
	 * 
	 * @return View on which the component has drawn.
	 */

	public View getView()
	{
		return mView;
	}

	/**
	 * Draws the component view onto the ViewGroup. Has to be overriden if
	 * non-standard Container layout parameters are needed.
	 * 
	 * @param v
	 *            View to draw onto
	 * @param params
	 *            Drawing parameters
	 */
	public void drawYourselfOnto(ViewGroup v, ViewGroup.LayoutParams params)
	{
		v.addView(mView, params);
	}

	/**
	 * Method for all the under-the-hood rendering of the given component.
	 */
	public abstract void render();

	/**
	 * Method called when the component is required to stop its action.
	 */
	protected abstract void stop();

	/**
	 * Method that serializes the correct attributes for the given component.
	 * 
	 * @param serializer
	 *            XML serializer to be used
	 */
	public abstract void toXML(XmlSerializer serializer);

	/**
	 * Copies resources of the particular component to the folder of the
	 * collection with the ID given. Changes the source atributes as well. Must
	 * be called before serializing component.
	 * 
	 * @param collectionID
	 *            Collection ID to which the card is being saved (resources will
	 *            be copied there)
	 * @param resourceID
	 *            Resource ID for the resource filename
	 */

	public abstract void editComponent(int requestCode);

	/**
	 * 
	 * Method to handle with the {@link Intent} returned from the
	 * {@link Activity} that deals with editing of the component.
	 * 
	 * @param intent
	 *            returned intent
	 */
	public abstract void handleEditResult(Intent intent);

	/**
	 * Method to delete the resources associated with the component.
	 * 
	 */
	public abstract void deleteResources();
}
