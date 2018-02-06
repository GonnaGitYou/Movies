package com.example.drken.flicfinder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.example.drken.flicfinder.data.MoviesContract.MovieEntry;
import static com.example.drken.flicfinder.data.MoviesContract.MovieFavorite;

/**
 * Class used to build SQLite database.
 * @author Dr. Ken
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    public final String LOG_TAG = DBOpenHelper.class.getSimpleName();

    //Constants for Database name and version
    private static final String DATABASE_NAME = "movies.db";
    private static final int DATABASE_VERSION = 1;

    //SQL to create table
    private static final String TABLE_CREATE ="CREATE TABLE "+ MovieEntry.TABLE_MOVIES + " (" +
                                                MovieEntry.MOVIE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                MovieEntry.API_MOVIE_ID + " TEXT, " +
                                                MovieEntry.MOVIE_TITLE + " TEXT, " +
                                                MovieEntry.MOVIE_SYNOPSIS + " TEXT, " +
                                                MovieEntry.MOVIE_POSTER + " TEXT, " +
                                                MovieEntry.MOVIE_RELEASE_DATE + " TEXT, " +
                                                MovieEntry.MOVIE_RATING + " TEXT, " +
                                                MovieEntry.MOVIE_IS_FAVORITE + " TEXT DEFAULT 'NO'" +
                                                ")";

    private static final String FAVORITE_TABLE_CREATE = "CREATE TABLE "+ MovieFavorite.TABLE_FAVORITES + " (" +
            MovieFavorite.FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            MovieFavorite.FAVORITE_IMDB_ID + " TEXT, " +
            MovieFavorite.FAVORITE_TTTLE + " TEXT, " +
            MovieFavorite.FAVORTIE_SYNOPSIS + " TEXT, " +
            MovieFavorite.FAVORITE_POSTER + " TEXT, " +
            MovieFavorite.FAVORITE_RELEASE_DATE + " TEXT, " +
            MovieFavorite.FAVORITE_RATING + " TEXT " +
            ")";

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(FAVORITE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_MOVIES);
        db.execSQL("DROP TABLE IF EXISTS " + MovieFavorite.TABLE_FAVORITES);
        onCreate(db);
    }
}
