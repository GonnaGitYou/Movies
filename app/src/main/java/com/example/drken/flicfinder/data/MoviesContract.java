package com.example.drken.flicfinder.data;

import android.app.FragmentManager;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contains class for movie object and database URI info for accessing objects in DB.
 * @author Dr. Ken
 */
public class MoviesContract {
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.
    public static final String CONTENT_AUTHORITY = "com.example.android.flicfinder";

    //Base pth refers to table used to store data
    public static final String BASE_PATH = "movies";
    public static final String FAVORITE_PATH = "favorites";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + BASE_PATH);
    public static final Uri FAVORITES_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + FAVORITE_PATH);


    /* Inner class that defines the table contents of the movies table */
    public static final class MovieEntry implements BaseColumns {
        //Constants for tables and columns
        public static final String TABLE_MOVIES = "movies";
        public static final String MOVIE_ID = "_id";
        public static final String API_MOVIE_ID = "apiID";
        public static final String MOVIE_TITLE = "movieTitle";
        public static final String MOVIE_SYNOPSIS = "movieSynopsis";
        public static final String MOVIE_POSTER = "moviePoster";
        public static final String MOVIE_RELEASE_DATE = "movieReleaseDate";
        public static final String MOVIE_RATING = "movieRating";
        public static final String MOVIE_IS_FAVORITE = "movieIsFavorite";

        public static final String[] ALL_COLUMNS = {MOVIE_ID, API_MOVIE_ID, MOVIE_TITLE, MOVIE_SYNOPSIS, MOVIE_POSTER, MOVIE_RELEASE_DATE, MOVIE_RATING, MOVIE_IS_FAVORITE};

    }

    /* Inner class that defines the table contents of the favorites table */
    public static final class MovieFavorite implements BaseColumns {
        //Constants for tables and columns
        public static final String TABLE_FAVORITES = "favorites";
        public static final String FAVORITE_ID = "_id";
        public static final String FAVORITE_IMDB_ID = "apiID";
        public static final String FAVORITE_TTTLE = "favoriteTitle";
        public static final String FAVORTIE_SYNOPSIS = "favoriteSynopsis";
        public static final String FAVORITE_POSTER = "favoritePoster";
        public static final String FAVORITE_RELEASE_DATE = "favoriteReleaseDate";
        public static final String FAVORITE_RATING = "favoriteRating";

        public static final String[] ALL_COLUMNS = {FAVORITE_ID, FAVORITE_IMDB_ID, FAVORITE_TTTLE, FAVORTIE_SYNOPSIS,
                                        FAVORITE_POSTER, FAVORITE_RELEASE_DATE, FAVORITE_RATING};
    }
}
