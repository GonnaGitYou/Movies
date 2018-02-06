package com.example.drken.flicfinder.presentation;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.example.drken.flicfinder.BuildConfig;
import com.example.drken.flicfinder.R;
import com.example.drken.flicfinder.data.FileHelper;
import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.utilities.ApplicationContextProvider;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * @author Dr. Ken
 */
public class GridViewCursorAdapterOffline extends CursorAdapter {

    public final String LOG_TAG = GridViewCursorAdapterOffline.class.getSimpleName();

    public GridViewCursorAdapterOffline(Context context, Cursor cursor, int flags) {
        super(context, cursor, 0);

    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.grid_layout, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final ImageView image = (ImageView)view.findViewById(R.id.imagePoster);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        image.setPadding(4, 4, 4, 4);

        String filename = cursor.getString(cursor.getColumnIndex(MoviesContract.MovieFavorite.FAVORITE_IMDB_ID));

        //Get file path of poster image
        final File filePath = context.getFileStreamPath(filename);
        Log.d(LOG_TAG,"Loading file...." + filePath);
        if(filePath.exists()) {
            FileHelper fh = new FileHelper();
            Bitmap poster = fh.loadBitmapFromInternalStorage(context, filePath);
            Log.d(LOG_TAG,"File exists.");
            image.setImageBitmap(poster);
        }

    }

}
