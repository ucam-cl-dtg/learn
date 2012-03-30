package uk.ac.cam.cl.dtg.android.language;

import java.io.File;

import uk.ac.cam.cl.dtg.android.language.graphics.Image;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * 
 * {@link Activity} for creating and editing multiple-choice image answers.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class ImageMCQEditor extends Activity implements OnLongClickListener,
		OnCheckedChangeListener
{
	private final static String LOG_TAG = "ImageMCQEditor";

	private final static int IMAGE_CAPTURE_TAG = 0, IMAGE_PICK_TAG = 1;

	private final static String BUNDLE_INTENT_SENDER_ID = "intentSenderID",
			BUNDLE_PICTURE_PATH = "picturePath", BUNDLE_RESOURCE_IDS = "resourceIDs",
			BUNDLE_CORRECT_ANSWER = "correctAnswer", BUNDLE_COLLECTION_ID = "collectionID";

	public final static String INTENT_RESOURCE_IDS = "resourceIDs",
			INTENT_CORRECT_ANSWER = "correctAnswer", INTENT_COLLECTION_ID = "collectionID";

	public final static int IMAGE_COUNT = 4;

	// arrays for storing all the UI elements
	private FrameLayout[] mHolders;
	private Button[] mButtons;
	private ImageView[] mImageViews;
	private RadioGroup[] mRadioGroups;
	private RadioButton[] mRadioButtons;
	private long[] mResourceIDs;

	// single UI elements
	private Button mOkButton;

	private int mIntentSenderID;
	private String mPicturePath;
	private int mCorrectAnswer = -1;

	private long mCollectionID;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		setContentView(R.layout.imagemcqeditor);

		mHolders = new FrameLayout[IMAGE_COUNT];
		mImageViews = new ImageView[IMAGE_COUNT];
		mButtons = new Button[IMAGE_COUNT];
		mRadioGroups = new RadioGroup[IMAGE_COUNT];
		mRadioButtons = new RadioButton[IMAGE_COUNT];

		mHolders[0] = (FrameLayout) findViewById(R.id.imageplace1);
		mHolders[1] = (FrameLayout) findViewById(R.id.imageplace2);
		mHolders[2] = (FrameLayout) findViewById(R.id.imageplace3);
		mHolders[3] = (FrameLayout) findViewById(R.id.imageplace4);

		mRadioGroups[0] = (RadioGroup) findViewById(R.id.radiogroup1);
		mRadioGroups[1] = (RadioGroup) findViewById(R.id.radiogroup2);
		mRadioGroups[2] = (RadioGroup) findViewById(R.id.radiogroup3);
		mRadioGroups[3] = (RadioGroup) findViewById(R.id.radiogroup4);

		mRadioButtons[0] = (RadioButton) findViewById(R.id.radiobutton1);
		mRadioButtons[1] = (RadioButton) findViewById(R.id.radiobutton2);
		mRadioButtons[2] = (RadioButton) findViewById(R.id.radiobutton3);
		mRadioButtons[3] = (RadioButton) findViewById(R.id.radiobutton4);

		// initialize the radio groups
		for (int i = 0; i < IMAGE_COUNT; i++)
		{
			mRadioGroups[i].setOnCheckedChangeListener(this);
		}

		if (savedInstanceState == null)
		{
			// handle the intent
			Intent intent = this.getIntent();

			mResourceIDs = intent.getLongArrayExtra(INTENT_RESOURCE_IDS);
			if (mResourceIDs == null)
			{
				mResourceIDs = new long[IMAGE_COUNT];

				for (int i = 0; i < IMAGE_COUNT; i++)
					mResourceIDs[i] = -1;
			}

			mCorrectAnswer = intent.getIntExtra(INTENT_CORRECT_ANSWER, -1);
			mCollectionID = intent.getLongExtra(INTENT_COLLECTION_ID, -1);

			MyLog.d(LOG_TAG, "collection id from intent is " + mCollectionID);

			instantiateImagesAndButtons();
		} else
		{
			mIntentSenderID = savedInstanceState.getInt(BUNDLE_INTENT_SENDER_ID);
			mPicturePath = savedInstanceState.getString(BUNDLE_PICTURE_PATH);
			mResourceIDs = savedInstanceState.getLongArray(BUNDLE_RESOURCE_IDS);
			mCorrectAnswer = savedInstanceState.getInt(BUNDLE_CORRECT_ANSWER, -1);
			mCollectionID = savedInstanceState.getLong(BUNDLE_COLLECTION_ID, -1);

			instantiateImagesAndButtons();

		}

		// initialize OK button
		mOkButton = (Button) findViewById(R.id.okbutton);
		mOkButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				returnResult();
			}
		});

	}

	/**
	 * 
	 * Method to check and return result to the {@link CardEditor}
	 */
	private void returnResult()
	{
		// check whether everything's correct and pack it all up back to intent
		// and send off
		boolean imageEmpty = false;

		for (long resID : mResourceIDs)
		{
			if (resID == -1)
			{
				imageEmpty = true;
				break;
			}
		}
		if (imageEmpty)
		{
			// show the dialog that some of the images are empty
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
			alertBuilder.setTitle(R.string.alert);
			alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
			alertBuilder.setMessage(getString(R.string.some_of_images_empty));

			alertBuilder.setPositiveButton(R.string.ok, null);
			alertBuilder.show();

		} else
		{
			if (mCorrectAnswer != -1)
			{
				Intent resultIntent = new Intent();

				resultIntent.putExtra(INTENT_RESOURCE_IDS, mResourceIDs);
				resultIntent.putExtra(INTENT_CORRECT_ANSWER, mCorrectAnswer);

				this.setResult(RESULT_OK, resultIntent);
				this.finish();

			} else
			{
				// show the dialog that none of the answers was selected as
				// correct
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setTitle(R.string.alert);
				alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
				alertBuilder.setMessage(getString(R.string.none_of_the_answers_selected_correct));
				alertBuilder.setPositiveButton(R.string.ok, null);

				alertBuilder.show();

			}
		}

	}

	/**
	 * Method to make button for adding image.
	 * 
	 * @param id ID of the option to build button for
	 */
	private void makeButton(int id)
	{
		mButtons[id] = new Button(this);
		mButtons[id].setText("Press me to add a picture");
		mButtons[id].setOnLongClickListener(this);
		mHolders[id].removeAllViews();
		mHolders[id].addView(
				mButtons[id],
				new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

	}

	/**
	 * Method to make image for one of the options.
	 * 
	 * @param id ID of the option to create image for.
	 * @throws ResourceNotFoundException 
	 */
	private void makeImage(int id) throws ResourceNotFoundException
	{
		ImageView iView;

		CardDBAdapter db = new CardDBAdapter();
		db.open(mCollectionID);
		String suffix;
		try {
		  suffix = db.getResource(mResourceIDs[id]).getSuffix();
		} finally {
		  db.close();
		}

		String path = "file://" + ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID + "/"
				+ mResourceIDs[id] + "." + suffix;

		MyLog.d(LOG_TAG, "Path string is " + path);

		Uri source = Uri.parse(path);

		iView = Image.produceImageView(this, source);
		mHolders[id].removeAllViews();
		mHolders[id].addView(
				iView,
				new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		mImageViews[id] = iView;

		iView.setOnLongClickListener(this);
	}
	
	/**
	 * Method for instantiating all images and buttons.
	 * 
	 */
	private void instantiateImagesAndButtons()
	{
		for (int i = 0; i < IMAGE_COUNT; i++)
		{
			if (mResourceIDs[i] >= 0)
			{
				// instantiate the image view and add it to the holder
				try {
          makeImage(i);
        } catch (ResourceNotFoundException e) {
          MyLog.e(LOG_TAG, e.getMessage());
          // fall back to just making a button
          makeButton(i);
        }
			} else
			{
				// just make a button
				makeButton(i);
			}
		}
		checkCorrect();

	}
	
	/**
	 * Method to check the checkbox for the correct answer.
	 * 
	 */
	private void checkCorrect()
	{
		// check the appropriate button
		if (mCorrectAnswer != -1)
			mRadioButtons[mCorrectAnswer].setChecked(true);

	}

	@Override
	public boolean onLongClick(View view)
	{
		boolean handled = false;

		// check the buttons first
		for (int i = 0; i < IMAGE_COUNT; i++)
		{
			if (mButtons[i] == view)
			{
				startImageDialog(i);
				handled = true;
			}
		}

		// then check the image views
		for (int i = 0; i < IMAGE_COUNT; i++)
		{
			if (mImageViews[i] == view)
			{
				makeButton(i);
				mImageViews[i] = null;

				ResourceHelper resHelper = new ResourceHelper(this);
				resHelper.reduceReferenceCount(mCollectionID, mResourceIDs[i]);

				mResourceIDs[i] = -1;
				handled = true;
			}
		}
		return handled;
	}

	/**
	 * Method to show image dialog.
	 * 
	 * @param id option for which the dialog is shown
	 */
	private void startImageDialog(int id)
	{
		mIntentSenderID = id;

		AlertDialog.Builder imageDialogBuilder = new AlertDialog.Builder(this);

		String[] actionsImage =
		{ getString(R.string.pick_a_file), getString(R.string.take_a_picture) };

		imageDialogBuilder.setItems(actionsImage, new DialogInterface.OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent intent;

				// TODO Auto-generated method stub
				switch (which)
				{
				case 0:
					intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("image/*");
					startActivityForResult(intent, IMAGE_PICK_TAG);
					break;
				case 1:
					intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

					Uri imageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "temp"
							+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
					intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureUri);

					mPicturePath = imageCaptureUri.toString();

					startActivityForResult(intent, IMAGE_CAPTURE_TAG);
					break;
				}
			}
		});

		imageDialogBuilder.show();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		if (resultCode == RESULT_OK)
		{
			int id = mIntentSenderID;
			boolean addToResolver = false;
			long resourceID = -1;

			Uri source = null;

			switch (requestCode)
			{
			case IMAGE_PICK_TAG:
				source = data.getData();

				break;
			case IMAGE_CAPTURE_TAG:
				try
				{
					source = data.getData();
				}
				catch (Exception e)
				{
					source = Uri.parse(mPicturePath);
				}
				addToResolver = true;
				break;
			}

			ResourceHelper fsHelper = new ResourceHelper(this);
			resourceID = fsHelper.addResource(source, mCollectionID, addToResolver);

			if (requestCode == IMAGE_CAPTURE_TAG)
				fsHelper.deleteUri(source);

			mResourceIDs[id] = resourceID;
			try {
        makeImage(id);
      } catch (ResourceNotFoundException e) {
        MyLog.e(LOG_TAG, "Could not make image for id ("+id+") :" + e.getMessage());
      }
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(BUNDLE_INTENT_SENDER_ID, mIntentSenderID);
		outState.putString(BUNDLE_PICTURE_PATH, mPicturePath);

		outState.putLongArray(BUNDLE_RESOURCE_IDS, mResourceIDs);
		outState.putInt(BUNDLE_CORRECT_ANSWER, mCorrectAnswer);
		outState.putLong(BUNDLE_COLLECTION_ID, mCollectionID);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId)
	{
		for (int i = 0; i < IMAGE_COUNT; i++)
		{
			if (mRadioGroups[i] == group)
				mCorrectAnswer = i;
			else
			{
				// uncheck the others
				mRadioGroups[i].setOnCheckedChangeListener(null);
				mRadioGroups[i].clearCheck();
				mRadioGroups[i].setOnCheckedChangeListener(this);
			}
		}

	}
}
