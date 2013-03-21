package com.koushikdutta.urlimageviewhelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;

/**
 * I have used UrlImageviewHelper for caching images from the internet, but I needed to cache thumbnails of 
 * local images as well, for a multi-sourced image gallery. I added this class to handle the ones I wanted 
 * to make and cache thumbnails of.  To differentiate between local files meant for this class as opposed
 * to FileUrlDownloader, images meant to be Thumbnailed should be sent as a path instead of a URL. This way
 * a mixed list of web image urls, small local image urls, and paths to large local images that need to be 
 * thumbnailed can be in the same set.
 * 
 * @author Walt Moorhouse
 */
public class FilePathThumbnailer implements UrlDownloader {

	@Override
	public void download(final Context context, final String url, String filename, final UrlDownloaderCallback callback, final Runnable completion) {
		final AsyncTask<Void, Void, Void> downloader = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                try {
                	// Can be pulled from values/dimens.xml
            		int thumbSize = 750; //context.getResources().getDimensionPixelSize(R.dimen.thumb_size);
            		
            	    final BitmapFactory.Options options = new BitmapFactory.Options();
            	    options.inJustDecodeBounds = true;
            	    BitmapFactory.decodeFile(url, options);
            	    options.inSampleSize = 1;
            	    if (options.outHeight > thumbSize || options.outWidth > thumbSize) {
            	        final int heightRatio = Math.round((float) options.outHeight / (float) thumbSize);
            	        final int widthRatio = Math.round((float) options.outWidth / (float) thumbSize);
            	        options.inSampleSize = heightRatio > widthRatio ? heightRatio : widthRatio;
            	    }
            	    options.inJustDecodeBounds = false;
            		ByteArrayOutputStream stream = new ByteArrayOutputStream();
            		cropCenter(BitmapFactory.decodeFile(url, options), true).compress(Bitmap.CompressFormat.JPEG, 100, stream);
            		byte[] data = stream.toByteArray();
                    callback.onDownloadComplete(FilePathThumbnailer.this, new ByteArrayInputStream(data), null);
                    return null;
                }
                catch (final Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final Void result) {
                completion.run();
            }
        };

        UrlImageViewHelper.executeTask(downloader);
	}
	
	// --- from Android Gallery source code
	public static Bitmap cropCenter(Bitmap bitmap, boolean recycle) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap;
        int size = Math.min(width, height);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2, (size - height) / 2);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }
    // ----

	@Override
	public boolean allowCache() {
		return true;
	}

	@Override
	public boolean canDownloadUrl(String url) {
		// To distinguish between local files I want thumbnailed, and those I do not, I send
		// the ones to thumbnail as a path instead of the url, so it will not begin with "file:/". 
		return !url.contains(":/");
	}

}
