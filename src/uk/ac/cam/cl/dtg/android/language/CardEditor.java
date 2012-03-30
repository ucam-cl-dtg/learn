package uk.ac.cam.cl.dtg.android.language;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.graphics.Component;
import uk.ac.cam.cl.dtg.android.language.graphics.Container;
import uk.ac.cam.cl.dtg.android.language.graphics.Editable;
import uk.ac.cam.cl.dtg.android.language.graphics.MultipleChoiceAnswerBox;
import uk.ac.cam.cl.dtg.android.language.graphics.SingleComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.ThreeComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TwoComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TwoEqualComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TypeInAnswer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

/**
 * 
 * Class for card editing activity
 * 
 * @author Vytautas
 * 
 */
public class CardEditor extends Activity implements AnswerListener
{
	private static final String LOG_TAG = "CardEditor";
	
	private int mContainerType;
	private Editable[] mEditables;
	private Container mContainer = null;

	private long mCollectionID;

	private int mIntentSenderID;
	private String mPicturePath;

	private Card mCard;

	protected static final String BUNDLE_XML_TAG = "description";
	protected static final String BUNDLE_ACTION = "currentAction";
	protected static final String BUNDLE_CONTAINER_TYPE = "containerType";
	protected static final String BUNDLE_CARD_ID = "cardID";
	protected static final String BUNDLE_INTENT_SENDER_ID = "senderID";
	protected static final String BUNDLE_PICTURE_PATH = "picture_path";
	protected static final String BUNDLE_COLLECTION_ID = "collectionID";
	protected static final String BUNDLE_DIALOG_SHOWN = "dialogShown";
	protected static final String BUNDLE_DIALOG_CODE = "dialogCode";

	protected static final String INTENT_XML_TAG = "description";
	protected static final String INTENT_ACTION = "action";
	protected static final String INTENT_CARD_ID = "cardID";
	protected static final String INTENT_COLLECTION_ID = "collectionID";

	private static final int ACTIVITY_CHOOSE_CONTAINER = 100;

	public static final int ACTION_NEW = 0;
	public static final int ACTION_EDIT = 1;

	private int mCurrentAction;

	private int mCurrentState = STATE_NORMAL;
	private static final int STATE_STARTING_ACTIVITY = 1;
	private static final int STATE_NORMAL = 0;
	
	public boolean mDialogShown = false;
	public int mDialogCode = 0;
	
	private static final int DIALOG_OVERALL = 1;
	private static final int DIALOG_AUDIO = 2;
	private static final int DIALOG_VIDEO = 3;
	private static final int DIALOG_IMAGE = 4;
	private static final int DIALOG_ANSWER = 5;
	
	private static final int DIALOG_HELP = 6;
	public static final int DIALOG_EDIT_DELETE = 7;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		MyLog.d(LOG_TAG, "onCreate() called");
		
		super.onCreate(savedInstanceState);

    	ApplicationInitializer initializer = new ApplicationInitializer(this);
    	if (initializer.initializeActivity())
    	{
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			
			// essential for video rendering - done here so that the screen does not flicker during rendering
			getWindow().setFormat(PixelFormat.TRANSLUCENT);
						
			setContentView(R.layout.emptylinear);
			
			// if instance is saved - recreate the editable from the bundle
			if (savedInstanceState != null)
			{
				MyLog.d(LOG_TAG, "savedInstanceState is not null - analysing it");
	
				// get the current action from bundle
				mCurrentAction = savedInstanceState.getInt(BUNDLE_ACTION);
				mPicturePath = savedInstanceState.getString(BUNDLE_PICTURE_PATH);
				mIntentSenderID = savedInstanceState.getInt(BUNDLE_INTENT_SENDER_ID);
				mCollectionID = savedInstanceState.getLong(BUNDLE_COLLECTION_ID);
				mContainerType = savedInstanceState.getInt(BUNDLE_CONTAINER_TYPE);
				mDialogShown = savedInstanceState.getBoolean(BUNDLE_DIALOG_SHOWN);
				
				long id = savedInstanceState.getLong(BUNDLE_CARD_ID, -1);
				
				if (id != -1)
				{
					CardDBAdapter db = new CardDBAdapter();
					db.open(mCollectionID);
					try {
					  MyLog.d(LOG_TAG, "Getting the card with id - " + id);

					  mCard = db.getCardById((long) id);
					} finally {
					  db.close();
					}
					if (mCard != null)
					{
						initializeFromXML(mCard.getXmlDescription());
					} else
					{
						MyLog.e(LOG_TAG, "Card for editing does not exist - shutting down activity");
		
						this.setResult(RESULT_CANCELED);
						this.finish();
					}
				}
				else
				{
					prepareEmptyEditor(mContainerType);
				}
				
				if (mDialogShown)
				{
					mDialogCode = savedInstanceState.getInt(BUNDLE_DIALOG_CODE);
					
					switch (mDialogCode)
					{
						case DIALOG_OVERALL:
							showComponentDialog(mIntentSenderID);
							break;
						case DIALOG_VIDEO:
							showVideoDialog();
							break;
						case DIALOG_AUDIO:
							showAudioDialog();
							break;
						case DIALOG_IMAGE:
							showImageDialog();
							break;
						case DIALOG_ANSWER:
							showMCQDialog();
							break;
						case DIALOG_HELP:
							showHelpDialog();
							break;
						case DIALOG_EDIT_DELETE:							
							mEditables[mIntentSenderID].showEditDeleteDialog();
							
							break;
					}
				}
	
			}
	
			// otherwise - analyse the intent
			else
			{
				MyLog.d(LOG_TAG, "savedInstanceState is null - analysing intent");
	
				Intent intent = getIntent();
	
				// impose the right container for the new view
				mCurrentAction = intent.getIntExtra(INTENT_ACTION, 0);
				mCollectionID = intent.getLongExtra(INTENT_COLLECTION_ID, 0);
	
				if (mCurrentAction == ACTION_NEW)
				{
					intent = new Intent(this, ContainerSelectActivity.class);
					mCurrentState = STATE_STARTING_ACTIVITY;
					startActivityForResult(intent, ACTIVITY_CHOOSE_CONTAINER);
				} else if (mCurrentAction == ACTION_EDIT)
				{
					// get the xml description of the card from the database
					long id = intent.getLongExtra(INTENT_CARD_ID, 0);
	
					CardDBAdapter db = new CardDBAdapter();
					db.open(mCollectionID);
					try {
					  mCard = db.getCardById((long) id);
					} finally {
					  db.close();
					}
					if (mCard != null)
					{
						initializeFromXML(mCard.getXmlDescription());
					} else
					{
						MyLog.e(LOG_TAG, "Card for editing does not exist - shutting down activity");
	
						this.setResult(RESULT_CANCELED);
						this.finish();
					}
				}
	
			}
    	}
		
		MyLog.d(LOG_TAG, "onCreate() finished");
	}

	/**
	 * 
	 * Serializes the card that is being edited into the XML string
	 * 
	 * @return the XML description of the card
	 */
	private String toXML()
	{
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try
		{
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);

			serializer.startTag("", XMLStrings.XML_CARD);
			serializer.attribute("", XMLStrings.XML_CONTAINER_TYPE, String.valueOf(mContainerType));
			// pass the serializer to all of the elements to collect element XML
			// descriptions
			for (int i = 0; i < mEditables.length; i++)
			{
				serializer.startTag("", XMLStrings.XML_ELEMENT);
				serializer.attribute("", XMLStrings.XML_COMPONENT_ID, String.valueOf(i));
				mEditables[i].toXML(serializer);
				serializer.endTag("", XMLStrings.XML_ELEMENT);
			}

			serializer.endTag("", XMLStrings.XML_CARD);
			serializer.endDocument();

			String returnable = writer.toString();

			MyLog.d(LOG_TAG, "Finished parsing to XML. The string is: " + returnable);

			return returnable;
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Exception caught while serializing the card to XML");
			return "";
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		mDialogShown = false;
		mDialogCode = 0;
		
		if (resultCode == RESULT_CANCELED)
		{
			// if we were about to choose the container - no point in
			// continuing, finish
			if (requestCode == ACTIVITY_CHOOSE_CONTAINER)
				this.finish();
			else
				// replay the card
				this.redraw();
			
		} else
		{
			ResourceHelper fshelper;
			long resourceID;
			Uri source;
			boolean deleteResult;
			
			switch (requestCode)
			{
			case Editable.TEXT_TYPE_IN_TAG:
				mEditables[mIntentSenderID].setText(data.getStringExtra(TextEditor.INTENT_TEXT), data.getBooleanExtra(TextEditor.INTENT_TTSABLE, false),
						true);
				
				finishActivity(requestCode);
				break;
			case Editable.VIDEO_RECORDING_TAG:
				
				// copy the video to the safe place
				fshelper = new ResourceHelper(this);
				
				source = data.getData();
				
				resourceID = fshelper.addResource(source, mCollectionID, true);
				
				deleteResult = fshelper.deleteUri(source);
				
				MyLog.d(LOG_TAG, "Original resource deleted - " + deleteResult);

				mEditables[mIntentSenderID].setVideo(mCollectionID, resourceID, 0, true);
				finishActivity(requestCode);
				break;
			case Editable.AUDIO_RECORDING_TAG:
				fshelper = new ResourceHelper(this);
			
				source = data.getData();
				
				resourceID = fshelper.addResource(source, mCollectionID, true);
				
				deleteResult = fshelper.deleteUri(source);
				
				MyLog.d(LOG_TAG, "Resource deleted - " + deleteResult);
				
				mEditables[mIntentSenderID].setAudio(mCollectionID, resourceID, 0, true, true);
				finishActivity(requestCode);
				break;
			case Editable.AUDIO_PICK_TAG:
				fshelper = new ResourceHelper(this);
				
				source = data.getData();
				
				resourceID = fshelper.addResource(source, mCollectionID, false);
			
				mEditables[mIntentSenderID].setAudio(mCollectionID, resourceID, 0, true, true);
				finishActivity(requestCode);
				break;
			case Editable.VIDEO_PICK_TAG:
				
				// copy the video to a safe place
				fshelper = new ResourceHelper(this);

				resourceID = fshelper.addResource(data.getData(), mCollectionID, false);
				
				mEditables[mIntentSenderID].setVideo(mCollectionID, resourceID, data.getIntExtra(
						"delay", 0), true);
				finishActivity(requestCode);
				break;
			case Editable.IMAGE_PICK_TAG:
				try
				{
					fshelper = new ResourceHelper(this);
					resourceID = fshelper.addResource(data.getData(), mCollectionID, false);
					
					mEditables[mIntentSenderID].setImage(mCollectionID, resourceID, data.getIntExtra(
							"width", 0), data.getIntExtra("height", 0), true);
					finishActivity(requestCode);
				}
				catch (Exception e)
				{
					this.showErrorDialog(getString(R.string.image_could_not_be_loaded));
				}
				break;
			case Editable.IMAGE_CAPTURE_TAG:
				
				try
				{
					// globalize the resource
					fshelper = new ResourceHelper(this);
					
					// this is due to Hero and Android Camera app mismatch....
					try
					{
						source = data.getData();
					}
					catch (Exception e)
					{
						source = Uri.parse(mPicturePath);
					}				
									
					resourceID = fshelper.addResource(source, mCollectionID, true);
					deleteResult = fshelper.deleteUri(source);
					
					mEditables[mIntentSenderID].setImage(mCollectionID, resourceID, 0, 0, true);
					finishActivity(requestCode);
				}
				catch (Exception e)
				{
					// inform the user that the image could not be set
					this.showErrorDialog(getString(R.string.image_could_not_be_loaded));
				}
				break;
			case Editable.TEXT_MCQ_TAG:

				MyLog.d(LOG_TAG, "Received the result from the text MC answer");

				MultipleChoiceAnswer answer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_TEXT);
				answer.addOption(data.getStringExtra(TextMCQEditor.INTENT_OPTION_1));
				answer.addOption(data.getStringExtra(TextMCQEditor.INTENT_OPTION_2));
				answer.addOption(data.getStringExtra(TextMCQEditor.INTENT_OPTION_3));
				answer.addOption(data.getStringExtra(TextMCQEditor.INTENT_OPTION_4));

				answer.setCorrect(data.getIntExtra(TextMCQEditor.INTENT_CORRECT, 0));

				mEditables[mIntentSenderID].setMCQAnswer(answer, true);

				break;
			case Editable.IMAGE_MCQ_TAG:

				MultipleChoiceAnswer imageAnswer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE);

				long[] resourceIDs = data.getLongArrayExtra(ImageMCQEditor.INTENT_RESOURCE_IDS);
				int correctAnswer = data.getIntExtra(ImageMCQEditor.INTENT_CORRECT_ANSWER, 0);

				for (long resID : resourceIDs)
					imageAnswer.addOption(String.valueOf(resID));

				imageAnswer.setCorrect(correctAnswer);
				mEditables[mIntentSenderID].setMCQAnswer(imageAnswer, true);

				break;
			case Editable.TYPE_IN_ANSWER_TAG:
				mEditables[mIntentSenderID].setTypeInAnswer(data.getStringExtra(TypeInAnswerEditor.INTENT_CORRECT_ANSWER), data.getStringExtra(TypeInAnswerEditor.INTENT_QUESTION), true);
				break;
			case ACTIVITY_CHOOSE_CONTAINER:
				int containerType = data.getIntExtra(
						ContainerSelectActivity.INTENT_CONTAINER_TYPE_ID, 0);
				prepareEmptyEditor(containerType);
				break;
			
			case Editable.IMAGE_REUSE_TAG:
				
				resourceID = ResourceHelper.resolveIntoResourceID(this, data.getData());
				
				mEditables[mIntentSenderID].setImage(mCollectionID, resourceID, 0, 0, true);
				break;
			case Editable.VIDEO_REUSE_TAG:
				
				resourceID = ResourceHelper.resolveIntoResourceID(this, data.getData());
				
				mEditables[mIntentSenderID].setVideo(mCollectionID, resourceID, data.getIntExtra(
						"delay", 0), true);

				
				break;
			case Editable.AUDIO_REUSE_TAG:
				
				resourceID = ResourceHelper.resolveIntoResourceID(this, data.getData());
				
				mEditables[mIntentSenderID].setAudio(mCollectionID, resourceID, 0, true, true);
				
				break;		
			default:
				// intent was sent by some of the components

				int componentID = requestCode - 1000;
				if (componentID >= 0) 
					mEditables[componentID].handleEditResult(data);

				// in case something has changed - redraw the components in the
				// container
				this.redraw();

				break;

			}

		}

		mCurrentState = STATE_NORMAL;
	}

	public void setSenderID(int id)
	{
		mIntentSenderID = id;
	}
	
	private void showErrorDialog(String dialogText)
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.error);
		alertDialogBuilder.setMessage(dialogText);
		alertDialogBuilder.setPositiveButton(R.string.ok, null);
		alertDialogBuilder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem helpItem = menu.add(R.string.help);
		helpItem.setIcon(android.R.drawable.ic_menu_help);
		helpItem.setOnMenuItemClickListener(new OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem arg0)
			{
				showHelpDialog();
				return true;
			}});
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		MyLog.d(LOG_TAG, "onSaveInstanceState() called");

		if (mCurrentState != STATE_STARTING_ACTIVITY)
		{
			saveChanges();
			this.stopComponents();
		}

		outState.putInt(BUNDLE_ACTION, mCurrentAction);
		outState.putInt(BUNDLE_INTENT_SENDER_ID, mIntentSenderID);
		outState.putLong(BUNDLE_COLLECTION_ID, mCollectionID);

		if (mCard != null)
			outState.putLong(BUNDLE_CARD_ID, mCard.getId());
		else
			outState.putLong(BUNDLE_CARD_ID, -1);
		
		outState.putInt(BUNDLE_CONTAINER_TYPE, mContainerType);

		outState.putString(BUNDLE_PICTURE_PATH, mPicturePath);
		
		outState.putBoolean(BUNDLE_DIALOG_SHOWN, mDialogShown);
		outState.putInt(BUNDLE_DIALOG_CODE, mDialogCode);

		this.stopComponents();

		super.onSaveInstanceState(outState);

		MyLog.d(LOG_TAG, "onSaveInstanceState() finished");
	}

	private void redraw()
	{
		if (mContainer != null)
		{
			mContainer.removeAllViews();

			View newView = mContainer.drawContainer();
			setContentView(newView);
		}
	}

	/**
	 * 
	 * Depending on the current action of the card editor, either creates a new
	 * card or updates the record of the existing card in the DB.
	 * 
	 */
	private void saveChanges()
	{
		MyLog.d(LOG_TAG, "saveChanges() called");
		
		boolean allEmpty = true;
		
		if (mEditables != null)
		{	
			for (Editable e : mEditables)
			{
				if (!e.isEmpty())
					allEmpty = false;
			}
			
			if (!allEmpty)
			{
				String xmlDesc = toXML();
				int type = getCardType();
				
				MyLog.d(LOG_TAG, "Card is of " + type + " type");
				
				CardDBAdapter db = new CardDBAdapter();
				db.open(mCollectionID);
				try {
				  if (mCurrentAction == ACTION_NEW)
				  {
				    long id = db.insertCard("Untitled", 0, type, xmlDesc, 0, "");
				    mCurrentAction = ACTION_EDIT;
				    mCard = db.getCardById(id);
				  } else if (mCurrentAction == ACTION_EDIT)
				  {
				    if (mCard != null)
				      db.updateContent(mCard.getId(), xmlDesc, type);
				  }
				} finally {
				  db.close();
				}
				
				this.setResult(RESULT_OK);
			}
			else
				this.setResult(RESULT_CANCELED);
		}
		else
			this.setResult(RESULT_CANCELED);
		
		
	}
	
	private int getCardType()
	{
		boolean hasAnswer = false;
		
		for (Editable e : mEditables)
		{
			if (e != null)
			{
				Component c = e.getComponent();
				if ((c instanceof TypeInAnswer) || (c instanceof MultipleChoiceAnswerBox))
				{
					hasAnswer = true;
				}
			}
		}
		
		if (hasAnswer)
			return Card.TYPE_LEARNING_AND_TESTING;
		else
			return Card.TYPE_LEARNING_ONLY;
	}

	public void setPicturePath(String s)
	{
		mPicturePath = s;
	}
	
	
	/**
	 * 
	 * Fills the Editable element from the given XML attributes - main XML parsing callback method.
	 * 
	 * @param attributes the attributes to be parsed from XML
	 */
	private void fillElement(Attributes attributes)
	{
		int componentID = Integer.parseInt(attributes.getValue(XMLStrings.XML_COMPONENT_ID));

		String type = attributes.getValue(XMLStrings.XML_TYPE);

		if (type == null)
			type = Editable.EMPTY;

		MyLog.d(LOG_TAG, "Filling element: id - " + componentID + ", type - " + type);

		mEditables[componentID] = new Editable(this, componentID, mCollectionID);

		if (type.equals(Editable.VIDEO))
		{
			String delay = attributes.getValue(XMLStrings.XML_MEDIA_DELAY);
			
			if (delay == null)
				delay = "0";

			
			
			mEditables[componentID].setVideo(mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)),
					Long.parseLong(delay), false);
		} else if (type.equals(Editable.AUDIO))
		{
			String delay = attributes.getValue(XMLStrings.XML_MEDIA_DELAY);
			
			if (delay == null)
				delay = "0";
			
			String playInTestMode = attributes.getValue(XMLStrings.XML_AUDIO_PLAY_IN_TEST_MODE);
			
			if (playInTestMode == null)
				playInTestMode = "true";
			
			mEditables[componentID].setAudio(mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)),
					Long.parseLong(delay), Boolean.parseBoolean(playInTestMode), false);
		} else if (type.equals(Editable.IMAGE))
		{
			String width = attributes.getValue(XMLStrings.XML_IMAGE_WIDTH);
			if (width == null)
				width = "0";
			
			String height = attributes.getValue(XMLStrings.XML_IMAGE_HEIGHT);
			if (height == null)
				height = "0";
					
			
			mEditables[componentID].setImage(mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)),
					Integer.parseInt(width),
					Integer.parseInt(height), false);

		} else if (type.equals(Editable.TEXT))
		{
			boolean ttsable = Boolean.parseBoolean(attributes.getValue(XMLStrings.XML_TEXT_TTSABLE));
			
			mEditables[componentID].setText(attributes.getValue(XMLStrings.XML_TEXT_VALUE), ttsable, false);
		} else if (type.equals(Editable.ANSWER))
		{
			String answerType = attributes.getValue(XMLStrings.XML_ANSWER_TYPE);

			if (answerType.equals(XMLStrings.XML_ANSWER_TYPE_TYPEIN))
			{
				String question;
				
				try
				{
					question = attributes.getValue(XMLStrings.XML_ANSWER_QUESTION);
				}
				catch (Exception e)
				{
					question = "";
				}
				
				mEditables[componentID].setTypeInAnswer(attributes.getValue(XMLStrings.XML_ANSWER_CORRECT_ANSWER), question,
						false);
			}
			else if (answerType.equals(XMLStrings.XML_ANSWER_TYPE_MC_TEXT))
			{
				MultipleChoiceAnswer answer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_TEXT);

				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_1));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_2));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_3));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_4));

				answer.setCorrect(Integer.parseInt(attributes.getValue(XMLStrings.XML_CORRECT_OPTION)));

				mEditables[componentID].setMCQAnswer(answer, false);

			} else if (answerType.equals(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE))
			{
				MultipleChoiceAnswer answer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE);

				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_1));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_2));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_3));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_4));

				answer.setCorrect(Integer.parseInt(attributes.getValue(XMLStrings.XML_CORRECT_OPTION)));

				mEditables[componentID].setMCQAnswer(answer, false);
			}
		}

		mEditables[componentID].render();
		mContainer.fillComponent(componentID, mEditables[componentID]);
	}

	private void finishedParsingXML()
	{
		for (int i = 0; i < mEditables.length; i++)
		{
			// fill the ones that were not previously initialised
			if (mEditables[i] == null)
			{
				mEditables[i] = new Editable(this, i, mCollectionID);
				mEditables[i].render();
				mContainer.fillComponent(i, mEditables[i]);
			}
		}

		if (mContainer != null)
		{
			View view = mContainer.drawContainer();
			setContentView(view);
		}

	}

	/**
	 * 
	 * Method called to get the container that has been initialized most
	 * recently
	 * 
	 * @return the container that has been initialized most recently
	 */

	public Container getContainer()
	{
		return mContainer;
	}

	/**
	 * 
	 * Method called to instantiate the container with a given type.
	 * 
	 * @param containerType
	 *            Required container type, one of the Container.ONE_COMPONENT,
	 *            ...
	 */

	private void instantiateContainer(int containerType)
	{
		MyLog.d(LOG_TAG, "instantiateContainer() called");

		mContainerType = containerType;

		switch (containerType)
		{
		case Container.CONTAINER_ONECOMPONENT:
			mContainer = new SingleComponentContainer(this);
			break;
		case Container.CONTAINER_TWOCOMPONENT:
			mContainer = new TwoComponentContainer(this);
			break;
		case Container.CONTAINER_TWOCOMPONENTEQUAL:
			mContainer = new TwoEqualComponentContainer(this);
			break;
		case Container.CONTAINER_THREECOMPONENT:
			mContainer = new ThreeComponentContainer(this);
			break;
		}

		mEditables = new Editable[Container.CONTAINER_ELEMENT_COUNT[mContainerType]];

	}

	/**
	 * 
	 * Method called to initialize CardEditor from XML description of the card.
	 * 
	 * @param xmlDescription
	 *            XML description of the card.
	 */

	private void initializeFromXML(String xmlDescription)
	{
		try
		{
			MyLog.d(LOG_TAG, xmlDescription);
			
			
			// let the parser fill in the ones
			// initiate factory and parser
			SAXParserFactory parsingFactory = SAXParserFactory.newInstance();
			SAXParser parser = parsingFactory.newSAXParser();

			// get the reader
			XMLReader reader = parser.getXMLReader();

			// create a new content handler and pass it onto the reader
			XMLEditorHandler xmlHandler = new XMLEditorHandler();
			reader.setContentHandler(xmlHandler);

			StringReader stringReader = new StringReader(xmlDescription);

			InputSource iSource = new InputSource(stringReader);

			reader.parse(iSource);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Error while parsing XML - " + e.getMessage()
					+ ". Printing stack trace:");

			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Prepares the empty editor window - fills in all the components with
	 * Editable's.
	 * 
	 * @param containerType
	 *            The type of container to prepare.
	 */

	private void prepareEmptyEditor(int containerType)
	{
		MyLog.d(LOG_TAG, "prepareEmptyEditor() called with the container type "
				+ String.valueOf(containerType));

		instantiateContainer(containerType);

		for (int i = 0; i < mEditables.length; i++)
		{
			// initialize the editable and fill the corresponding component of
			// the container
			mEditables[i] = new Editable(this, i, mCollectionID);
			mEditables[i].render();
			mContainer.fillComponent(i, mEditables[i]);
		}

		if (mContainer != null)
		{
			View view = mContainer.drawContainer();

			setContentView(view);
		}
	}

	/**
	 * Method called to stop components - essentially propagates the
	 * stopComponents() call to the container.
	 * 
	 */
	private void stopComponents()
	{
		if (mContainer != null)
		{
			mContainer.stopComponents();
			MyLog.d(LOG_TAG, "Stopping components");
		}
	}

	/**
	 * A method called to save the changes and stop components.
	 */
	@Override
	protected void onPause()
	{
		MyLog.d(LOG_TAG, "onPause() called");
		
		saveChanges();
		
		this.stopComponents();
		
		super.onPause();
		
		MyLog.d(LOG_TAG, "onPause() finished");
	}

	@Override
	protected void onDestroy()
	{
		MyLog.d(LOG_TAG, "onDestroy() called");
		super.onDestroy();		
		MyLog.d(LOG_TAG, "onDestroy() finished");
	}

	/**
	 * Inner class for XML handling
	 * 
	 * @author Vytautas
	 * 
	 */
	class XMLEditorHandler extends DefaultHandler
	{
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{}

		@Override
		public void endDocument() throws SAXException
		{
			finishedParsingXML();
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{}

		@Override
		public void startDocument() throws SAXException
		{}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException
		{
			// CARD
			if (localName.equals("card"))
			{
				instantiateContainer(Integer.parseInt(attributes.getValue("containerType")));
			} else if (localName.equals("element"))
			{
				fillElement(attributes);
			}
		}
	}

	@Override
	public void answerCorrect()
	{
		Toast.makeText(this, R.string.answer_correct, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void answerIncorrect()
	{
		Toast.makeText(this,  R.string.answer_incorrect, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Shows dialog for choosing what sort of component is to be added.
	 * 
	 * @param componentID component ID for which the component type is to be chosen
	 */
	public void showComponentDialog(int componentID)
	{
		this.setSenderID(componentID);
		
		mDialogShown = true;
		mDialogCode = DIALOG_OVERALL;
		
		// set up the dialog that lets you choose what to add onto that container
		AlertDialog.Builder alertDialogBuilder = 
			new AlertDialog.Builder(this);
	
		String [] types = {this.getString(R.string.text), this.getString(R.string.audio), this.getString(R.string.video), this.getString(R.string.image), this.getString(R.string.type_in_answer), this.getString(R.string.multiple_choice_answer)};				
		
		alertDialogBuilder.setTitle(R.string.choose_component_type);
		
		alertDialogBuilder.setItems(types,
				new DialogInterface.OnClickListener() {
					@Override
          public void onClick(DialogInterface dialog, int typeID)
					{
						mDialogShown = false;
						mDialogCode = 0;
						
						// send up the corresponding intent from the perspective of the CardEditor and be ready to catch the result
						switch (typeID)
						{
						case 0:
							// type in text
							Intent intent = new Intent(CardEditor.this, TextEditor.class);
							startActivityForResult(intent, Editable.TEXT_TYPE_IN_TAG);
							break;
						
						case 1:
							showAudioDialog();
							break;
						case 2:
							// set up the video intent							
							showVideoDialog();
							
							break;
						case 3:
							// set up the image dialog
							
							showImageDialog();							
							
							break;
						
						case 4:
							intent = new Intent(CardEditor.this, TypeInAnswerEditor.class);
							startActivityForResult(intent, Editable.TYPE_IN_ANSWER_TAG);


							break;
						case 5:
							// set up the MCQ dialog
							showMCQDialog();
							

							break;
						}
						
						
					}
				});
		alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});
		
		alertDialogBuilder.show();

	}
	
	/**
	 * Shows a dialog for choosing audio source - picking a file vs recording.
	 */
	public void showAudioDialog()
	{
		mDialogCode = DIALOG_AUDIO;
		mDialogShown = true;
		
		AlertDialog.Builder audioDialogBuilder = 
			new AlertDialog.Builder(this);
		
		String [] actions = {this.getString(R.string.pick_a_file), this.getString(R.string.record)};
		
		audioDialogBuilder.setTitle(R.string.choose_audio_source);
		
		audioDialogBuilder.setItems(actions, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent;
				
				mDialogShown = false;
				mDialogCode = 0;

				switch (which)
				{
				case 0:
					intent = new Intent(Intent.ACTION_PICK, 
							  android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(intent, Editable.AUDIO_PICK_TAG);
					break;
				case 1:
					intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
					startActivityForResult(intent, Editable.AUDIO_RECORDING_TAG);
					break;
				}
			}
		});
		
		audioDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});

		
		audioDialogBuilder.show();

	}
	
	/**
	 * Shows dialog for choosing video source - recording vs picking a file.
	 */
	public void showVideoDialog()
	{
		mDialogCode = DIALOG_VIDEO;
		mDialogShown = true;

		
		AlertDialog.Builder videoDialogBuilder = 
			new AlertDialog.Builder(this);
		
		String [] actionsVideo = {this.getString(R.string.pick_a_file), this.getString(R.string.record)};
		
		videoDialogBuilder.setTitle(R.string.choose_video_source);
		
		videoDialogBuilder.setItems(actionsVideo, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent;
				
				mDialogShown = false;
				mDialogCode = 0;
				
				switch (which)
				{
				case 0:
					intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("video/*");
					startActivityForResult(intent, Editable.VIDEO_PICK_TAG);
					break;
				case 1:
					intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
					startActivityForResult(intent, Editable.VIDEO_RECORDING_TAG);				
					break;
				}
			}
		});
		
		videoDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});

		
		videoDialogBuilder.show();

	}
	
	/**
	 * Shows a dialog for choosing where an image is to come from - existing file vs camera.
	 */
	public void showImageDialog()
	{
		mDialogCode = DIALOG_IMAGE;
		mDialogShown = true;

		
		AlertDialog.Builder imageDialogBuilder = 
			new AlertDialog.Builder(this);
		
		String [] actionsImage = {getString(R.string.pick_a_file), getString(R.string.take_a_picture)};
		
		imageDialogBuilder.setTitle(R.string.choose_image_source);
		
		imageDialogBuilder.setItems(actionsImage, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mDialogShown = false;
				mDialogCode = 0;
				
				Intent intent;
				
				// TODO Auto-generated method stub
				switch (which)
				{
				case 0:
					intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("image/*");
				
					startActivityForResult(intent, Editable.IMAGE_PICK_TAG);
					
					break;
				case 1:
					intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					
					Uri imageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "test" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
					intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureUri);
					
					setPicturePath(imageCaptureUri.toString());
					
					startActivityForResult(intent, Editable.IMAGE_CAPTURE_TAG);				
					break;
				}
			}
		});
		
		imageDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});

		
		imageDialogBuilder.show();

	}
	
	/**
	 * Shows the dialog for choosing multiple-choice answer type: image vs text.
	 */
	public void showMCQDialog()
	{
		mDialogCode = DIALOG_ANSWER;
		mDialogShown = true;

		
		AlertDialog.Builder mcqDialogBuilder = 
			new AlertDialog.Builder(this);
		
		String [] actionsMCQ = {getString(R.string.image_based_answer), getString(R.string.text_based_answer)};
		
		mcqDialogBuilder.setTitle(R.string.choose_answer_type);
		
		mcqDialogBuilder.setItems(actionsMCQ, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent;
				
				mDialogShown = false;
				mDialogCode = 0;

				
				switch (which)
				{
				case 0:
					intent = new Intent(CardEditor.this, ImageMCQEditor.class);
					
					intent.putExtra(ImageMCQEditor.INTENT_COLLECTION_ID, mCollectionID);
					
					startActivityForResult(intent, Editable.IMAGE_MCQ_TAG);
					break;
				case 1:
					intent = new Intent(CardEditor.this, TextMCQEditor.class);
					
					startActivityForResult(intent, Editable.TEXT_MCQ_TAG);											
																	
					break;
				}
			}
		});
		
		mcqDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});

		
		mcqDialogBuilder.show();

	}
	
	/**
	 * Shows a dialog that gives you basic information about how to use this editor.
	 */
	public void showHelpDialog()
	{
		mDialogShown = true;
		mDialogCode = DIALOG_HELP;
		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(CardEditor.this);
		builder.setTitle(R.string.help);
		builder.setMessage(R.string.card_editor_help_string);
		
		builder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				mDialogShown = false;
				mDialogCode = 0;
			}
		});

		
		builder.show();
	}
}
