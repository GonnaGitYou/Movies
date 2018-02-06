package com.example.drken.flicfinder.presentation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.example.drken.flicfinder.BuildConfig;
import com.example.drken.flicfinder.R;
import com.example.drken.flicfinder.data.FileHelper;
import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.state.SessionManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Dr. Ken on 3/16/2016.
 */
public class GridViewCursorAdapter extends CursorAdapter {

    public final String LOG_TAG = GridViewCursorAdapter.class.getSimpleName();


    public GridViewCursorAdapter(Context context, Cursor cursor, int flags) {
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

        //For now the 154dp image seems to be the best fit
        final String IMAGE_PATH = "http://image.tmdb.org/t/p/w154";
        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w75";
        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w185";
        final String APPID_PARAM = "api_key";

        final String path = cursor.getString(cursor.getColumnIndex(MoviesContract.MovieEntry.MOVIE_POSTER));

        final Context ct = context;

        Uri builtUri = Uri.parse(IMAGE_PATH.concat(path)).buildUpon()
                .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                .build();
        Log.d("fillView", "Uri is " + builtUri.toString());

        final ImageView image = (ImageView) view.findViewById(R.id.imagePoster);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        image.setPadding(4, 4, 4, 4);


            if (!path.equals("null") && !path.equals("/null")) {
                Picasso.with(context).load(builtUri).into(image);

            } else {
                //If we don't get a poster back let's throw up a placeholder
                Picasso.with(context).load(builtUri).placeholder(R.drawable.noposter).into(image);

            }

    }


}
