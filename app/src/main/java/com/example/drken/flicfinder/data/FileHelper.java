package com.example.drken.flicfinder.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.example.drken.flicfinder.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Dr. Ken
 */
public class FileHelper {

    public final String LOG_TAG = FileHelper.class.getSimpleName();

    public FileHelper() {

    }

    /**
     * This method will download an image resource using Picasso and save it to internal storage.
     *
     * @param uri      Uri of bitmap resource.
     * @param filename Name that you want to use for file.
     * @param ctx      Context of application.
     */
    public void downloadAndSave(final Uri uri, final String filename, final Context ctx) {

        Target imageTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                //Save image for offline retrieval
                commitBitmapToMemory(filename, bitmap, ctx);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        Picasso.with(ctx).load(uri).placeholder(R.drawable.noposter).into(imageTarget);
    }

    /**
     * This method will delete a file from internal storage.
     *
     * @param ctx      Context of application.
     * @param filename Name of file to be deleted.
     */
    public void deleteFile(Context ctx, String filename) {
        ctx.deleteFile(filename);
    }

    public Bitmap loadBitmapFromInternalStorage(Context ctx, File file) {
        Bitmap imageStored = null;
        try {

            //File filePath = ctx.getFileStreamPath(filename);
            FileInputStream fi = new FileInputStream(file);
            imageStored = BitmapFactory.decodeStream(fi);

        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error accessing file on internal storage");
            Log.e("Error msg: ", ex.getMessage());
        }
        return imageStored;
    }


    /**
     * This method will be used to store the image downloaded by Picasso to internal storage for offline use.
     *
     * @param filename The name of the file will be the API ID for the movie.
     * @param bitmap   The bitmap retrieved by Picasso.
     * @param cntx     The application context.
     */
    private void commitBitmapToMemory(String filename, Bitmap bitmap, Context cntx) {
        FileOutputStream fos;
        // Write bitmap
        try {
            fos = cntx.openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            Log.d(LOG_TAG, "Successfully saved file: " + filename + ".jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Error saving file: " + filename + ".jpg");
        }

    }
}
