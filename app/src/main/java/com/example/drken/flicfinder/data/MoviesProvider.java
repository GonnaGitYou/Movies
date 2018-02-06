package com.example.drken.flicfinder.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.drken.flicfinder.data.MoviesContract.MovieEntry;
import com.example.drken.flicfinder.data.MoviesContract.MovieFavorite;

/**
 * Acts as SQLite database intermediary.
 * @author Dr. Ken
 */
public class MoviesProvider extends ContentProvider {

    public final String LOG_TAG = MoviesProvider.class.getSimpleName();

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.
    private static final String CONTENT_AUTHORITY = "com.example.android.flicfinder";

    //Base pth refers to table used to store data
    private static final String BASE_PATH = "movies";
    private static final String FAVORITE_PATH = "favorites";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri CONTENT_URI = Uri.parse("content://" +
            CONTENT_AUTHORITY + "/" + BASE_PATH  );
    public static final Uri FAVORITES_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + FAVORITE_PATH);




    //Constants identifying various operations
    //Getting all movies
    private  static final int MOVIES = 1;
    //Getting individual movies
    private static final int MOVIES_ID = 2;
    //Getting favorites list
    private static final int FAVORITES = 3;
    //Getting individual favorites
    private static final int FAVORITE_ID = 4;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //public static final String CONTENT_FAVORITE_INDICATOR = "isfavorite";

    private SQLiteDatabase database;

    //SIB for UriMatcher
    static {
        uriMatcher.addURI(CONTENT_AUTHORITY, BASE_PATH, MOVIES);
        uriMatcher.addURI(CONTENT_AUTHORITY, BASE_PATH + "/#", MOVIES_ID);
        uriMatcher.addURI(CONTENT_AUTHORITY, FAVORITE_PATH, FAVORITES);
        uriMatcher.addURI(CONTENT_AUTHORITY, FAVORITE_PATH + "/#", FAVORITE_ID);
    }


    @Override
    public boolean onCreate() {
        //Create the SQLite database that will store our data
        DBOpenHelper dbOpenHelper = new DBOpenHelper(getContext());
        database = dbOpenHelper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(LOG_TAG,"query()");
        Cursor c = null;
        //Add a selection value if the uriMatcher determines that we are looking for a specific row
//        if(uriMatcher.match(uri) == MOVIES_ID){
//            selection = MovieEntry.MOVIE_ID + "=" + uri.getLastPathSegment();
//        }
Log.d(LOG_TAG, String.format("Uri is...%s", uri));
//        return database.query(MovieEntry.TABLE_MOVIES, MovieEntry.ALL_COLUMNS,
//                selection, null, null, null, MovieEntry.MOVIE_TITLE + " ASC");
        switch (uriMatcher.match(uri)){

            case MOVIES:
                Log.d(LOG_TAG,"Made it to the correct Uri match.");
                c = database.query(MovieEntry.TABLE_MOVIES, MovieEntry.ALL_COLUMNS,
                        selection, null, null, null, MovieEntry.MOVIE_TITLE + " ASC");
                break;

            case MOVIES_ID:
                selection = MovieEntry.MOVIE_ID + "=" + uri.getLastPathSegment();
                c = database.query(MovieEntry.TABLE_MOVIES, MovieEntry.ALL_COLUMNS,
                        selection, null, null, null, MovieEntry.MOVIE_TITLE + " ASC");
                break;

            case FAVORITES:
                c = database.query(MovieFavorite.TABLE_FAVORITES, MovieFavorite.ALL_COLUMNS,
                        selection, null, null, null, MovieFavorite.FAVORITE_TTTLE + " ASC");
                break;

            case FAVORITE_ID:
                Log.d(LOG_TAG,"I'm querying the favorites table.");
                selection = MovieFavorite.FAVORITE_ID + "=" + uri.getLastPathSegment();
                c = database.query(MovieFavorite.TABLE_FAVORITES, MovieFavorite.ALL_COLUMNS,
                        selection, null, null, null, MovieFavorite.FAVORITE_TTTLE + " ASC");
                break;

            default:
                break;
        }
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri retUri = null;
        long id;
        switch (uriMatcher.match(uri)){
            case MOVIES:
                id = database.insert(MovieEntry.TABLE_MOVIES, null, values);
                retUri = Uri.parse(BASE_PATH + "/" + id);
                break;

            case FAVORITES:
                id = database.insert(MovieFavorite.TABLE_FAVORITES, null, values);
                retUri = Uri.parse(FAVORITE_PATH + "/" + id);
                Log.d(LOG_TAG, "I'm inserting some data in favorites.  The Uri is " + retUri.toString());
                break;

            default:
                break;
        }
        return retUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int retId = 0;
        switch (uriMatcher.match(uri)){
            case MOVIES:
                retId = database.delete(MovieEntry.TABLE_MOVIES, selection, selectionArgs);
                break;

            case MOVIES_ID:
                selection = MovieEntry.MOVIE_ID + "=" + uri.getLastPathSegment();
                retId = database.delete(MovieEntry.TABLE_MOVIES, selection, selectionArgs);
                break;

            case FAVORITES:
                retId = database.delete(MovieFavorite.TABLE_FAVORITES, selection, selectionArgs);
                Log.d(LOG_TAG, "I'm deleting some data in favorites. ");
                break;

            case FAVORITE_ID:
                selection = MovieFavorite.FAVORITE_ID + "=" + uri.getLastPathSegment();
                retId = database.delete(MovieFavorite.TABLE_FAVORITES, selection, selectionArgs);
                break;

            default:
                break;
        }
        return retId;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int retId = 0;

        switch (uriMatcher.match(uri)){
            case MOVIES:
                retId = database.update(MovieEntry.TABLE_MOVIES, values, selection, selectionArgs);
                break;

            case MOVIES_ID:
                selection = MovieEntry.MOVIE_ID + "=" + uri.getLastPathSegment();
                retId = database.update(MovieEntry.TABLE_MOVIES, values, selection, selectionArgs);
                break;

            case FAVORITES:
                retId = database.update(MovieFavorite.TABLE_FAVORITES, values, selection, selectionArgs);
                break;

            case FAVORITE_ID:
                selection = MovieFavorite.FAVORITE_ID + "=" + uri.getLastPathSegment();
                retId = database.update(MovieFavorite.TABLE_FAVORITES, values, selection, selectionArgs);
                break;

            default:
                break;
        }
        return retId;
    }
}
