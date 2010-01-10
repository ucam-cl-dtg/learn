package uk.ac.cam.cl.dtg.android.language;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.cam.cl.dtg.android.language.graphics.Audio;
import uk.ac.cam.cl.dtg.android.language.graphics.Component;
import uk.ac.cam.cl.dtg.android.language.graphics.Container;
import uk.ac.cam.cl.dtg.android.language.graphics.Image;
import uk.ac.cam.cl.dtg.android.language.graphics.MultipleChoiceAnswerBox;
import uk.ac.cam.cl.dtg.android.language.graphics.SingleComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.Text;
import uk.ac.cam.cl.dtg.android.language.graphics.ThreeComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TwoComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TwoEqualComponentContainer;
import uk.ac.cam.cl.dtg.android.language.graphics.TypeInAnswer;
import uk.ac.cam.cl.dtg.android.language.graphics.Video;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ViewFlipper;

/**
 * 
 * Class for rendering cards in {@link LearningActivity}
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class CardRenderer
{
	private static final String LOG_TAG = "CardRenderer";

	private static final int DURATION_IN_ANIMATION = 500;
	public static final int DURATION_OUT_ANIMATION = 500;
	private static final int DURATION_DELAY_IN_ANIMATION = 500;
	public static final int DURATION_TOTAL_LAG = (DURATION_DELAY_IN_ANIMATION + DURATION_IN_ANIMATION) + 500;

	private LearningActivity mContext;
	private Container mContainer;

	private long mCollectionID;

	private boolean mTesting;

	private ViewFlipper mAnimator;

	private Animation inAnimation, outAnimation;

	public CardRenderer(LearningActivity context, long collectionID)
	{
		mContext = context;
		mCollectionID = collectionID;

		reinitialize();
	}

	/**
	 * Method to reinitialize the {@link CardRenderer}
	 */
	protected void reinitialize()
	{
		mAnimator = new ViewFlipper(mContext);

		inAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_right);
		inAnimation.setDuration(DURATION_IN_ANIMATION);
		inAnimation.setStartOffset(DURATION_DELAY_IN_ANIMATION);

		outAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slide_out_left);
		outAnimation.setDuration(DURATION_OUT_ANIMATION);
		outAnimation.setStartOffset(0);

		mAnimator.setInAnimation(inAnimation);
		mAnimator.setOutAnimation(outAnimation);

		mContext.setContentView(mAnimator);
	}

	/**
	 * 
	 * Method to render a single card - parses XML and prepares card through
	 * callback methods.
	 * 
	 * @param cardXML
	 *            XML description of the card
	 * @param testing
	 *            true if the card should be shown in testing mode, false if in
	 *            learning mode
	 * @param showAnimation
	 *            true if the slide in animation should be shown, false if not
	 * 
	 */
	public void renderCard(String cardXML, boolean testing, boolean showAnimation)
	{
		try
		{
			if (showAnimation)
			{
				mAnimator.setInAnimation(inAnimation);
				mAnimator.setOutAnimation(outAnimation);
			} else
			{
				mAnimator.setInAnimation(null);
				mAnimator.setOutAnimation(null);
			}

			mTesting = testing;

			// initiate factory and parser
			SAXParserFactory parsingFactory = SAXParserFactory.newInstance();
			SAXParser parser = parsingFactory.newSAXParser();

			// get the reader
			XMLReader reader = parser.getXMLReader();

			// create a new content handler and pass it onto the reader
			XMLCardHandler handler = new XMLCardHandler();
			reader.setContentHandler(handler);

			StringReader stringReader = new StringReader(cardXML);

			InputSource iSource = new InputSource(stringReader);

			reader.parse(iSource);
		} catch (Exception e)
		{
			MyLog.e(LOG_TAG, "Error in XML parsing - " + e.toString());
		}
	}

	/**
	 * 
	 * Method to initialize suitable container for a card - called from
	 * {@link #XMLCardHandler}
	 * 
	 * @param containerType
	 */
	public void initializeCard(int containerType)
	{
		// get hold of the corresponding view - be ready to receive drawText(),
		// drawImage(), playAudio() and other calls...
		switch (containerType)
		{
		case Container.CONTAINER_ONECOMPONENT:
			mContainer = new SingleComponentContainer(mContext);
			break;
		case Container.CONTAINER_TWOCOMPONENT:
			mContainer = new TwoComponentContainer(mContext);
			break;
		case Container.CONTAINER_TWOCOMPONENTEQUAL:
			mContainer = new TwoEqualComponentContainer(mContext);
			break;
		case Container.CONTAINER_THREECOMPONENT:
			mContainer = new ThreeComponentContainer(mContext);
			break;
		}
		MyLog.d(LOG_TAG, "Card rendering was initialized");
	}

	/**
	 * 
	 * Callback method called to fill in a component from the XML parser.
	 * 
	 * @param attributes
	 *            attributes of the element
	 */
	private void drawElement(Attributes attributes)
	{
		String elementType = attributes.getValue(XMLStrings.XML_TYPE);

		if (elementType == null)
		{
			MyLog.d(LOG_TAG,
					"drawElement() - element type is null, something might have went wrong...");
			elementType = "";
		}

		String compString = attributes.getValue(XMLStrings.XML_COMPONENT_ID);

		int targetComponent;

		if (compString != null)
			targetComponent = Integer.parseInt(compString);
		else
			targetComponent = 0;

		Component c;

		if (elementType.equals(XMLStrings.XML_TYPE_VIDEO))
		{
			// create new video component and add it to the container
			long delay;
			try
			{
				delay = Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_DELAY));
			} catch (Exception e)
			{
				delay = 0;
			}

			c = new Video(mContext, mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)), delay);

			c.render();

			mContainer.fillComponent(targetComponent, c);
		} else if (elementType.equals(XMLStrings.XML_TYPE_AUDIO))
		{
			long delay;
			try
			{
				delay = Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_DELAY));
			} catch (Exception e)
			{
				delay = 0;
			}

			String playInTestMode = attributes.getValue(XMLStrings.XML_AUDIO_PLAY_IN_TEST_MODE);

			if (playInTestMode == null)
				playInTestMode = "true";

			// set up the audio
			c = new Audio(mContext, mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)), delay, Boolean.parseBoolean(playInTestMode), mTesting);
			c.render();

			mContainer.fillComponent(targetComponent, c);
		} else if (elementType.equals(XMLStrings.XML_TYPE_IMAGE))
		{
			try
			{
				String width = attributes.getValue(XMLStrings.XML_IMAGE_WIDTH);
				if (width == null)
					width = "0";

				String height = attributes.getValue(XMLStrings.XML_IMAGE_HEIGHT);
				if (height == null)
					height = "0";

				c = new Image(mContext, mCollectionID, Long.parseLong(attributes.getValue(XMLStrings.XML_MEDIA_RESOURCE_ID)), Integer.parseInt(width), Integer.parseInt(height));
				c.render();
				mContainer.fillComponent(targetComponent, c);

			} catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
		} else if (elementType.equals(XMLStrings.XML_TYPE_TEXT))
		{
			boolean ttsable = Boolean.parseBoolean(attributes.getValue(XMLStrings.XML_TEXT_TTSABLE));

			c = new Text(mContext, attributes.getValue(XMLStrings.XML_TEXT_VALUE), ttsable);

			c.render();

			mContainer.fillComponent(targetComponent, c);
		} else if (elementType.equals(XMLStrings.XML_TYPE_ANSWER))
		{
			if (attributes.getValue(XMLStrings.XML_ANSWER_TYPE).equals(
					XMLStrings.XML_ANSWER_TYPE_TYPEIN))
			{
				String question;

				try
				{
					question = attributes.getValue(XMLStrings.XML_ANSWER_QUESTION);
				} catch (Exception e)
				{
					question = "";
				}

				TypeInAnswer answer = new TypeInAnswer(mContext, mContext, question, attributes.getValue(XMLStrings.XML_ANSWER_CORRECT_ANSWER));
				answer.setTesting(mTesting);
				answer.render();

				mContainer.fillComponent(targetComponent, answer);

			} else if (attributes.getValue(XMLStrings.XML_ANSWER_TYPE).equals(
					XMLStrings.XML_ANSWER_TYPE_MC_TEXT))
			{
				MultipleChoiceAnswer answer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_TEXT);

				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_1));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_2));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_3));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_4));

				answer.setCorrect(Integer.parseInt(attributes.getValue(XMLStrings.XML_CORRECT_OPTION)));

				MultipleChoiceAnswerBox box = new MultipleChoiceAnswerBox(mContext, mContext, answer, mCollectionID);

				box.setTesting(mTesting);

				box.render();

				mContainer.fillComponent(targetComponent, box);
			} else if (attributes.getValue(XMLStrings.XML_ANSWER_TYPE).equals(
					XMLStrings.XML_ANSWER_TYPE_MC_IMAGE))
			{
				MultipleChoiceAnswer answer = new MultipleChoiceAnswer(XMLStrings.XML_ANSWER_TYPE_MC_IMAGE);

				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_1));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_2));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_3));
				answer.addOption(attributes.getValue(XMLStrings.XML_OPTION_4));

				answer.setCorrect(Integer.parseInt(attributes.getValue(XMLStrings.XML_CORRECT_OPTION)));

				MultipleChoiceAnswerBox box = new MultipleChoiceAnswerBox(mContext, mContext, answer, mCollectionID);

				box.setTesting(mTesting);

				box.render();

				mContainer.fillComponent(targetComponent, box);

			}
		}

		MyLog.d(LOG_TAG, "Filled component " + targetComponent + " with " + elementType);
	}

	/**
	 * Method to slide out the last card. It is a good idea to delay any action
	 * after that for {@link #DURATION_OUT_ANIMATION}.
	 */
	protected void slideOut()
	{
		// add a blank view after that and show it
		mAnimator.addView(new View(mContext));
		mAnimator.showNext();
	}

	/**
	 * Method called from XML parser when the card parsing is finished. Sorts
	 * out the views and shows the card.
	 * 
	 */
	private void finishedCard()
	{
		MyLog.d(LOG_TAG, "finishedCard() called");
		MyLog.d(LOG_TAG, "Child count before is - " + mAnimator.getChildCount());

		while (mAnimator.getChildCount() > 1)
		{
			mAnimator.removeViewAt(0);
		}

		MyLog.d(LOG_TAG, "Child count after is - " + mAnimator.getChildCount());

		// start playing audio stuff, video, etc, etc..
		View newView = mContainer.drawContainer();

		mAnimator.addView(newView);

		MyLog.d(LOG_TAG, "Starting hiding the keyboard");

		// hide the soft keyboard
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mAnimator.getWindowToken(), 0);

		MyLog.d(LOG_TAG, "Finished hiding keyboard");

		mAnimator.showNext();
	}

	public void stop()
	{

		if (mContainer != null)
			mContainer.stopComponents();

	}

	/**
	 * Inner class for XML handling
	 * 
	 * @author Vytautas Vaitukaitis
	 * 
	 */
	class XMLCardHandler extends DefaultHandler
	{
		private boolean mCardStarted = false;

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{}

		@Override
		public void endDocument() throws SAXException
		{}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			if (localName.equals(XMLStrings.XML_CARD))
			{
				finishedCard();
			}
		}

		@Override
		public void startDocument() throws SAXException
		{}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException
		{
			// CARD
			if (localName.equals(XMLStrings.XML_CARD))
			{

				if (mCardStarted)
				{
					MyLog.e(LOG_TAG, "Card parsing already started - check for duplicate <card> tags");
					throw new SAXException("Card parsing already started - check for duplicate <card> tags");
				} else
				{
					try
					{
						int containerID = 0;
						try
						{
							containerID = Integer.parseInt(attributes.getValue(XMLStrings.XML_CONTAINER_TYPE));
						} catch (Exception e)
						{
							MyLog.e(LOG_TAG, "Exception caught while obtaining container type - "
									+ e.getMessage());
							MyLog.d(
									LOG_TAG,
									"Setting container to the three component container - so that we are sure we will fit everything that's on the card in");
							containerID = Container.CONTAINER_THREECOMPONENT;
						}

						// start up the card parsing
						initializeCard(containerID);
					} catch (Exception e)
					{
						MyLog.e(LOG_TAG, "Exception caught while initializing card - "
								+ e.getMessage());
					}
				}

			} else if (localName.equals(XMLStrings.XML_ELEMENT))
			{
				drawElement(attributes);
			} else
			{
				MyLog.d(LOG_TAG, "Unrecognized XML tag...");
			}

		}

	}
}