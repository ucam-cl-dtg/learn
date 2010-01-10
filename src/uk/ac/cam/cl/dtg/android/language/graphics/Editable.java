package uk.ac.cam.cl.dtg.android.language.graphics;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.CardEditor;
import uk.ac.cam.cl.dtg.android.language.MultipleChoiceAnswer;
import uk.ac.cam.cl.dtg.android.language.MyLog;
import uk.ac.cam.cl.dtg.android.language.R;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * 
 * Class that represents component which wraps up any other component and makes
 * user able to edit it (decorator pattern used here).
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class Editable extends Component
{
	private static final String LOG_TAG = "Editable";

	public static final int AUDIO_RECORDING_TAG = 1;
	public static final int VIDEO_RECORDING_TAG = 2;
	public static final int AUDIO_PICK_TAG = 3;
	public static final int VIDEO_PICK_TAG = 4;
	public static final int IMAGE_PICK_TAG = 5;
	public static final int IMAGE_CAPTURE_TAG = 6;
	public static final int TEXT_TYPE_IN_TAG = 7;
	public static final int TEXT_MCQ_TAG = 8;
	public static final int IMAGE_MCQ_TAG = 9;
	public static final int TYPE_IN_ANSWER_TAG = 10;
	public static final int IMAGE_REUSE_TAG = 11;
	public static final int VIDEO_REUSE_TAG = 12;
	public static final int AUDIO_REUSE_TAG = 13;

	private Component mComponent = null; // the component that will be saved
	private int mComponentID;

	public static final String EMPTY = "empty";
	public static final String VIDEO = XMLStrings.XML_TYPE_VIDEO;
	public static final String AUDIO = XMLStrings.XML_TYPE_AUDIO;
	public static final String IMAGE = XMLStrings.XML_TYPE_IMAGE;
	public static final String TEXT = XMLStrings.XML_TYPE_TEXT;
	public static final String ANSWER = XMLStrings.XML_TYPE_ANSWER;

	public static final int CONTEXT_MENU_EDIT_COMPONENT = 100;
	public static final int CONTEXT_MENU_DELETE_COMPONENT = 101;

	private final CardEditor mContext;
	private long mCollectionID;

	public Editable(CardEditor c, int id, long collectionID)
	{
		mComponentID = id;
		mCollectionID = collectionID;

		mContext = c;
	}

	public void render()
	{
		LinearLayout mLayout = new LinearLayout(mContext);

		mLayout.setGravity(Gravity.CENTER);

		// create a long-clickable button that allows you to build components
		Button addButton = new Button(mContext);
		addButton.setText(mContext.getString(R.string.click_to_edit));

		addButton.setOnLongClickListener(new View.OnLongClickListener()
		{

			@Override
			public boolean onLongClick(View v)
			{
				mContext.showComponentDialog(mComponentID);
				return false;
			}
		});

		mLayout.addView(
				addButton,
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		mView = mLayout;
	}

	@Override
	public void drawYourselfOnto(ViewGroup v, LayoutParams params)
	{
		// if the component has not been set, do the usual thing - just show the
		// screen + button
		if (mComponent == null)
			super.drawYourselfOnto(v, params);
		else
		{
			mComponent.render();
			mComponent.drawYourselfOnto(v, params);
			mComponent.getView().setOnLongClickListener(new View.OnLongClickListener()
			{

				@Override
				public boolean onLongClick(View v)
				{
					showEditDeleteDialog();
					return false;
				}
			});
		}
	}

	public void setVideo(long collectionID, long resourceID, long delay, boolean onActivityResult)
	{
		Video videoComponent = new Video(mContext, collectionID, resourceID, delay);
		// videoComponent.render();

		mComponent = videoComponent;

		if (onActivityResult)
		{
			redraw();
		}
	}

	public void setAudio(long collectionID, long resourceID, long delay, boolean playInTestMode,
			boolean onActivityResult)
	{
		Audio audioComponent = new Audio(mContext, collectionID, resourceID, delay, playInTestMode, false);
		// audioComponent.render();

		mComponent = audioComponent;

		if (onActivityResult)
		{
			redraw();
		}

	}

	public void setImage(long collectionID, long resourceID, int width, int height,
			boolean onActivityResult)
	{
		try
		{
			Image imageComponent = new Image(mContext, collectionID, resourceID, width, height);
			// imageComponent.render();

			mComponent = imageComponent;

			if (onActivityResult)
			{
				redraw();
			}

		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while drawing image - " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void setText(String value, boolean ttsable, boolean onActivityResult)
	{
		Text textComponent = new Text(mContext, value, ttsable);
		// textComponent.render();

		mComponent = textComponent;

		if (onActivityResult)
		{
			redraw();
		}

	}

	public void setTypeInAnswer(String correctValue, String question, boolean onActivityResult)
	{
		TypeInAnswer answerComponent = new TypeInAnswer(mContext, mContext, question, correctValue);
		// answerComponent.render();

		mComponent = answerComponent;

		if (onActivityResult)
		{
			redraw();
		}

	}

	public void setMCQAnswer(MultipleChoiceAnswer answer, boolean onActivityResult)
	{
		MultipleChoiceAnswerBox answerComponent = new MultipleChoiceAnswerBox(mContext, mContext, answer, mCollectionID);
		// answerComponent.render();

		mComponent = answerComponent;

		if (onActivityResult)
			redraw();
	}

	public void redraw()
	{
		Container container = mContext.getContainer();
		container.removeViewsFromFrame(mComponentID);
		ViewGroup myFrame = container.getFrame(mComponentID);
		this.drawYourselfOnto(myFrame, container.defaultParams);
	}

	@Override
	public void editComponent(int requestCode)
	{
		if (mComponent != null)
			mComponent.editComponent(requestCode);
	}

	private void deleteComponent()
	{

		mComponent.stop();
		mComponent.deleteResources();
		mComponent = null;
		this.redraw();
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			if (mComponent != null)
			{
				mComponent.toXML(serializer);
			}
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while describing a component in XML - "
					+ e.getMessage());
		}
	}

	@Override
	protected void stop()
	{
		if (mComponent != null)
			mComponent.stop();
	}

	public void showEditDeleteDialog()
	{
		if (mComponent != null)
		{
			mContext.mDialogShown = true;
			mContext.mDialogCode = CardEditor.DIALOG_EDIT_DELETE;
			mContext.setSenderID(mComponentID);

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			builder.setTitle(R.string.choose_action);

			// Image does not have an option to be edited, thus need 2 cases
			// here
			if (mComponent instanceof Image)
			{
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						switch (which)
						{
						case 0:
							// delete
							mContext.mDialogShown = false;
							mContext.mDialogCode = 0;

							deleteComponent();
						}

					}
				};

				builder.setItems(new String[]
				{ mContext.getString(R.string.delete_component) }, listener);

			} else
			{
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
				{

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						switch (which)
						{
						case 0:
							// edit
							mContext.mDialogShown = false;
							mContext.mDialogCode = 0;

							editComponent(mComponentID + 1000);
							break;
						case 1:
							// delete
							mContext.mDialogShown = false;
							mContext.mDialogCode = 0;

							deleteComponent();
						}

					}
				};

				builder.setItems(new String[]
				{ mContext.getString(R.string.edit_component),
						mContext.getString(R.string.delete_component) }, listener);

			}

			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{

				@Override
				public void onCancel(DialogInterface dialog)
				{
					mContext.mDialogShown = false;
					mContext.mDialogCode = 0;
				}
			});

			builder.show();

		}
	}

	@Override
	public void handleEditResult(Intent intent)
	{
		if (mComponent != null)
			mComponent.handleEditResult(intent);

	}

	@Override
	public void deleteResources()
	{
		if (mComponent != null)
			mComponent.deleteResources();
	}

	public boolean isEmpty()
	{
		return mComponent == null;
	}

	public Component getComponent()
	{
		return mComponent;
	}

	@Override
	public View getView()
	{
		if (mView != null)
			return mView;
		else if (mComponent != null)
			return mComponent.getView();
		else
			return null;
	}

}