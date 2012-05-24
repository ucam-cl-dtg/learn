package uk.ac.cam.cl.dtg.android.language.graphics;

import java.io.FileInputStream;
import java.io.InputStream;

import org.xmlpull.v1.XmlSerializer;

import uk.ac.cam.cl.dtg.android.language.ApplicationInitializer;
import uk.ac.cam.cl.dtg.android.language.CardDBAdapter;
import uk.ac.cam.cl.dtg.android.language.L;
import uk.ac.cam.cl.dtg.android.language.ResourceHelper;
import uk.ac.cam.cl.dtg.android.language.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.android.language.XMLStrings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * 
 * Method that represents Image component. Also contains a few static methods to
 * deal with image loading.
 * 
 * @author Vytautas Vaitukaitis
 * 
 */
public class Image extends Component
{
	private final static String LOG_TAG = "Image";

	private Activity mContext;
	private long mCollectionID;
	private long mResourceID;

	public Image(Activity context, long collectionID, long resourceID, int width, int height)
	{
		mContext = context;
		mResourceID = resourceID;
		mCollectionID = collectionID;
		mWidth = width;
		mHeight = height;
	}

	@Override
  public void render()
	{
	  try {
	    CardDBAdapter db = new CardDBAdapter();
	    db.open(mCollectionID);
	    String suffix;
	    try {
	      suffix = db.getResource(mResourceID).getSuffix();
	    } finally {
	      db.close();
	    }

	    mView = produceImageView(mContext, Uri.parse("file://"
	        + ApplicationInitializer.COLLECTIONS_FOLDER + mCollectionID + "/" + mResourceID
	        + "." + suffix));

	    if (mView == null)
	      L.e(LOG_TAG, "Produced ImageView is null");
	  } catch (ResourceNotFoundException e){
	    L.e(LOG_TAG, e.getMessage());
	  }
	}

	public static ImageView produceImageView(Context context, Uri source)
	{
		return produceImageView(context, source, false);
	}

	public static ImageView produceImageView(Context context, Uri source, boolean small)
	{
		try
		{
			L.d("Image", "Displaying image at " + source);

			// try decoding bitmap
			ImageView iv = new ImageView(context);

			Bitmap bitmap = null;

			if (small)
				bitmap = produceBitmap(context, source, true);
			else
				bitmap = produceBitmap(context, source, false);

			iv.setImageBitmap(bitmap);

			return iv;

		} catch (Exception e)
		{
			L.e(LOG_TAG, "Exception caught while rendering image - " + e.getMessage());
			return null;
		}

	}

	public static Bitmap produceBitmap(Context context, Uri source)
	{
		return produceBitmap(context, source, false);
	}

	public static Bitmap produceBitmap(Context context, Uri source, boolean small)
	{
		try
		{
			// get the image dimensions and decide what compression should be
			// used
			InputStream bitmapInputStream = null;
			if (source.getScheme().equals("file"))
			{
				bitmapInputStream = new FileInputStream(source.getPath());
			} else
			{
				bitmapInputStream = context.getContentResolver().openInputStream(source);
			}

			BitmapFactory.Options optionsSize = new BitmapFactory.Options();

			L.d(LOG_TAG, "Free memory before decoding bounds - "
					+ Runtime.getRuntime().freeMemory());

			optionsSize.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(bitmapInputStream, null, optionsSize);
			int imWidth = optionsSize.outWidth;
			int imHeight = optionsSize.outHeight;

			L.d(LOG_TAG, "Free memory after decoding bounds - "
					+ Runtime.getRuntime().freeMemory());

			L.d(LOG_TAG, "Image size is " + imWidth + "x" + imHeight);

			int compression = 1;
			if (!small)
			{
				if (imWidth > 200 || imHeight > 200)
				{
					if (imWidth > imHeight)
						compression = imWidth / 200;
					else
						compression = imHeight / 200;
				}
			} else
			{
				if (imWidth > 100 || imHeight > 100)
				{
					if (imWidth > imHeight)
						compression = imWidth / 100;
					else
						compression = imHeight / 100;
				}

			}

			L.d(LOG_TAG, "Compression for image is " + compression);

			// reset the input stream and create the image bitmap under memory
			// restrictions
			if (source.getScheme().equals("file"))
			{
				bitmapInputStream = new FileInputStream(source.getPath());
			} else
			{
				bitmapInputStream = context.getContentResolver().openInputStream(source);
			}
			BitmapFactory.Options opts = new BitmapFactory.Options();

			opts.inSampleSize = compression;
			Bitmap bitmap = BitmapFactory.decodeStream(bitmapInputStream, null, opts);

			L.d(LOG_TAG, "Free memory after decoding image - "
					+ Runtime.getRuntime().freeMemory());

			return bitmap;
		} catch (Throwable e)
		{
			L.e(LOG_TAG, "Exception caught while decoding bitmap - " + e.getMessage());
			return null;
		}
	}

	@Override
	public void drawYourselfOnto(ViewGroup v, LayoutParams params)
	{
		// override the parameters - draw just as big as they wanted it to be
		ViewGroup.LayoutParams newParams;

		if (mWidth != 0 && mHeight != 0)
			newParams = new ViewGroup.LayoutParams(mWidth, mHeight);
		else
			newParams = params;

		super.drawYourselfOnto(v, newParams);
	}

	@Override
	public void toXML(XmlSerializer serializer)
	{
		try
		{
			serializer.attribute("", XMLStrings.XML_TYPE, XMLStrings.XML_TYPE_IMAGE);
			serializer.attribute("", XMLStrings.XML_MEDIA_RESOURCE_ID, String.valueOf(mResourceID));
			serializer.attribute("", XMLStrings.XML_IMAGE_WIDTH, String.valueOf(mWidth));
			serializer.attribute("", XMLStrings.XML_IMAGE_HEIGHT, String.valueOf(mHeight));
		} catch (Exception e)
		{
			L.e(LOG_TAG, "Exception caught while serializing image - " + e.getMessage());
		}
	}

	@Override
	protected void stop()
	{
	}

	@Override
	public void editComponent(int requestCode)
	{
	}

	@Override
	public void handleEditResult(Intent intent)
	{
	}

	@Override
	public void deleteResources()
	{
		ResourceHelper resHelper = new ResourceHelper(mContext);

		resHelper.reduceReferenceCount(mCollectionID, mResourceID);

	}
}
