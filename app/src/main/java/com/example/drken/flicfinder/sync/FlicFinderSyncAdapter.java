package com.example.drken.flicfinder.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.drken.flicfinder.BuildConfig;
import com.example.drken.flicfinder.MoviesFragment;
import com.example.drken.flicfinder.R;
import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.data.MoviesContract.MovieEntry;
import com.example.drken.flicfinder.state.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The custom sync adapter will sync data queries to the movie API at http://api.themoviedb.org.
 * Created by Dr. Ken on 3/7/2016.
 */
public class FlicFinderSyncAdapter extends AbstractThreadedSyncAdapter {

    //API Related Constants
    private static final int MAX_PAGES_TO_QUERY = 7;
    private static boolean API_AVAILABLE = true;

    //Data retrieval modes
    public static final int MOVIES_NO_SORT = 0;
    public static final int MOVIES_POPULAR_SORT = 1;
    public static final int MOVIES_RATINGS_SORT = 2;
    public static final int MOVIES_FAVORITE_SORT = 3;
    public static int SORT_MODE = MOVIES_NO_SORT;

    //Array to hold favorites
    public static String[] myFavorites = null;

    public final String LOG_TAG = FlicFinderSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;


    public FlicFinderSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        //Get instance of content resolver from the incoming Context
        mContentResolver = context.getContentResolver();

    }

    //Constructor maintains compatibility with Android 3.0 and later
    public FlicFinderSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        //Get instance of content resolver from the incoming Context
        mContentResolver = context.getContentResolver();

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        //Clear existing data records
        mContentResolver.delete(MoviesContract.CONTENT_URI,
                null,
                null);
        Log.d(LOG_TAG, "I'm in FlicFinderSyncAdapter and mode is " + SORT_MODE);
        //Fetch new data - How depends on sort mode
        if (SORT_MODE == MOVIES_FAVORITE_SORT) {
            if (myFavorites != null) {
                if (getFavoriteMovieList()) {
                    //Report  data changed to refresh view
                    if (MoviesFragment.mFavoriteLoader != null)
                        MoviesFragment.mFavoriteLoader.onContentChanged();
                } else {
                    Log.d(LOG_TAG, "Could not retrieve data from server");

                }
            } else {
                Log.d(LOG_TAG, "You have no favorites!");
            }
        } else {
            Log.d(LOG_TAG, "Checking page values...." + API_AVAILABLE);
            Log.d(LOG_TAG, "Pages values...current....max....total" + SessionManager.getQueryCurrentPage() + "/" + MAX_PAGES_TO_QUERY + "/" + SessionManager.getQueryTotalPages());
            while (SessionManager.getQueryCurrentPage() < MAX_PAGES_TO_QUERY && SessionManager.getQueryCurrentPage() <= SessionManager.getQueryTotalPages() && API_AVAILABLE == true) {
                if (getMovieList()) {
                    SessionManager.incrementCurrentPage();
                } else {
                    //Cancel the sync request and bail out of the loop
                    ContentResolver.cancelSync(account, authority);
                    API_AVAILABLE = false;
                }
            }
        }
        //Give the API another chance next time
        API_AVAILABLE = true;

        //Reset page session data
        SessionManager.resetCurrentPage();
        SessionManager.resetQueryTotalPages();

        //Report  data changed to refresh view
        if (MoviesFragment.mCursorLoader != null) MoviesFragment.mCursorLoader.onContentChanged();

        return;
    }

    /**
     * Fetches movie information.
     *
     * @return Boolean to indicate whether we got data back from the API.
     */
    private boolean getMovieList() {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        boolean RECEIVED_DATA = false;

        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;

        try {
            // Construct the URL for the MoviesDB query
            // Possible parameters are avaiable at http://docs.themoviedb.apiary.io/#reference/configuration
            //Example:  https://api.themoviedb.org/3/movie/550?api_key=65f9aa0115aaad281ad43d1586xxxxxx
            //or...http://api.themoviedb.org/3/discover/movie?certification=R&sort_by=vote_average.desc&api_key=65f9aa0115aaad281ad43d1586xxxxxx

            //To build an image URL, you will need 3 pieces of data. The base_url, size and file_path. Simply combine them all and you will have a fully qualified URL. Here’s an example URL:
            //http://image.tmdb.org/t/p/w75/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
            ///movie/id/images


            final String MOVIES_BASE_URL =
                    "https://api.themoviedb.org/3/discover/movie?";

            final String SORT_PARAM = "sort_by";
            final String SORT_POPULARITY = "popularity.desc";
            final String SORT_VOTE = "vote_average.desc";
            final String RATING_PARAM = "certification"; //Not being used currently
            final String PAGE_PARAM = "page";
            final String APPID_PARAM = "api_key";

            Uri builtUri;

            //Change the URL based on sort flag NOTE - All modes sorted alphabetically
            switch (SORT_MODE) {
                case MOVIES_NO_SORT:
                    builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendQueryParameter(PAGE_PARAM, Integer.toString(SessionManager.getQueryCurrentPage()))
                            .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                            .build();
                    break;
                case MOVIES_POPULAR_SORT:
                    builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendQueryParameter(PAGE_PARAM, Integer.toString(SessionManager.getQueryCurrentPage()))
                            .appendQueryParameter(SORT_PARAM, SORT_POPULARITY)
                            .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                            .build();
                    break;
                case MOVIES_RATINGS_SORT:
                    builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendQueryParameter(PAGE_PARAM, Integer.toString(SessionManager.getQueryCurrentPage()))
                            .appendQueryParameter(SORT_PARAM, SORT_VOTE)
                            .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                            .build();
                    break;
                default:
                    builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                            .appendQueryParameter(PAGE_PARAM, Integer.toString(SessionManager.getQueryCurrentPage()))
                            .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                            .build();
                    break;
            }

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "URL built = " + builtUri.toString());

            // Create the request to MovieDB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return RECEIVED_DATA;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return RECEIVED_DATA;
            }
            moviesJsonStr = buffer.toString();
            RECEIVED_DATA = getMovieDataFromJson(moviesJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the movie data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return RECEIVED_DATA;
    }

    /**
     * Fetches movie information for a specific movie id.  Favorite movie ids are stored in a different table than "movies".
     *
     * @return Boolean to indicate whether we got data back from the API.
     */
    private boolean getFavoriteMovieList() {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        boolean RECEIVED_DATA = false;

        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;
        Log.d(LOG_TAG, "myFavorites length is " + myFavorites.length);
        for (int i = 0; i < myFavorites.length; i++) {
            try {
                // Construct the URL for the MoviesDB query
                // Possible parameters are avaiable at http://docs.themoviedb.apiary.io/#reference/configuration
                //Example:  https://api.themoviedb.org/3/movie/550?api_key=65f9aa0115aaad281ad43d1586xxxxxx
                //or...http://api.themoviedb.org/3/discover/movie?certification=R&sort_by=vote_average.desc&api_key=65f9aa0115aaad281ad43d1586xxxxxx

                //To build an image URL, you will need 3 pieces of data. The base_url, size and file_path. Simply combine them all and you will have a fully qualified URL. Here’s an example URL:
                //http://image.tmdb.org/t/p/w75/8uO0gUM8aNqYLs1OsTBQiXu0fEv.jpg
                ///movie/id/images

                //URLs for individual movie fetches by id are constructed like this https://api.themoviedb.org/3/movie/333186?api_key=65f9aa0115aaad281ad43d1586xxxxxx

                final String MOVIES_BASE_URL =
                        "https://api.themoviedb.org/3/movie/" + myFavorites[i] + "?";

                Log.d(LOG_TAG, "URLs from Favorites->" + MOVIES_BASE_URL);

                final String APPID_PARAM = "api_key";

                Uri builtUri;

                builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                        .build();


                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "URL built = " + builtUri.toString());

                // Create the request to MovieDB, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return RECEIVED_DATA;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return RECEIVED_DATA;
                }
                moviesJsonStr = buffer.toString();
                RECEIVED_DATA = getFavoriteDataFromJson(moviesJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
                // to parse it.
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                    Log.i(LOG_TAG, "HTTP connection closed.");
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }//End Try...Catch Block
        }//End For Loop
        return RECEIVED_DATA;
    }

    private String[] getTrailers(int _movieID) {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        boolean RECEIVED_DATA = false;

        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;

        ArrayList<String> trailers = new ArrayList<>();

        try {
            // Construct the URL for the MoviesDB query

            //URLs for trailer fetches by movie id are constructed like this https://api.themoviedb.org/3/movie/<id>/videos?api_key=65f9aa0115aaad281ad43d1586xxxxxx
            //where <id> = movie id from Movie DB

            final String MOVIES_BASE_URL =
                    "https://api.themoviedb.org/3/movie/" + _movieID + "/videos?";

            Log.d(LOG_TAG, "URLs from Favorites->" + MOVIES_BASE_URL);

            final String APPID_PARAM = "api_key";

            Uri builtUri;

            builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                    .build();


            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "URL built = " + builtUri.toString());

            // Create the request to MovieDB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            moviesJsonStr = buffer.toString();
            trailers = getTrailersFromJson(moviesJsonStr);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the movie data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
                Log.i(LOG_TAG, "HTTP connection closed.");
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }//End Try...Catch Block

        String[] aryTrailers = new String[trailers.size()];
        aryTrailers = trailers.toArray(aryTrailers);
        Log.d(LOG_TAG,aryTrailers.toString());
        return aryTrailers;
    }

    private ArrayList<String> getTrailersFromJson(String moviesJsonStr)
            throws JSONException {

        ArrayList<String> trailers = new ArrayList<>();

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        final String MDB_RESULTS = "results";
        final String MDB_KEY = "key";
        final String MDB_SITE = "site";

        try {
            JSONObject movieJson = new JSONObject(moviesJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(MDB_RESULTS);


            for (int i = 0, size = movieArray.length(); i < size; i++) {
                JSONObject objectInArray = movieArray.getJSONObject(i);
                String movieTrailerID = objectInArray.getString(MDB_KEY);
                String movieSite = objectInArray.getString(MDB_SITE);
                //We are only going to process YouTube trailers
                if (movieSite.equals("YouTube")) trailers.add(movieTrailerID);
            }



        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return trailers;
    }

    /**
     * Take the String representing the movies in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * @param moviesJsonStr is the JSON packet obtained from the API.
     * @return Boolean indicating whether or not we were able to parse the JSON data successfully.
     * @throws JSONException
     */
    private boolean getMovieDataFromJson(String moviesJsonStr)
            throws JSONException {

        boolean PROCESSED_DATA = false;

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        final String MDB_MOVIES = "results";
        final String MDB_PAGES = "total_pages";
        final String MDB_API_ID = "id";
        final String MDB_TITLE = "original_title";
        final String MDB_SYNOPSIS = "overview";
        final String MDB_PATH_IMAGE = "poster_path";
        final String MDB_RELEASE_DATE = "release_date";
        final String MDB_USER_RATING = "vote_average";

        try {
            JSONObject movieJson = new JSONObject(moviesJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(MDB_MOVIES);

            // Insert the new movie information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(movieArray.length());

            //Find out how many pages are available for this query.  NOTE - for now we are only pulling down 7.
            SessionManager.setQueryTotalPages(Integer.parseInt(movieJson.getString(MDB_PAGES)));
            Log.d(LOG_TAG, "Total pages -->" + movieJson.getString(MDB_PAGES));

            //Iterate thru movie list and extract info
            for (int i = 0, size = movieArray.length(); i < size; i++) {
                JSONObject objectInArray = movieArray.getJSONObject(i);
                String movieAPIID = objectInArray.getString(MDB_API_ID);
                String movieTitle = objectInArray.getString(MDB_TITLE);
                String movieSynopsis = objectInArray.getString(MDB_SYNOPSIS);
                String moviePoster = objectInArray.getString(MDB_PATH_IMAGE);
                String movieReleased = objectInArray.getString(MDB_RELEASE_DATE);
                String movieRating = objectInArray.getString(MDB_USER_RATING);


                //Perform a bulk load//////////////////////////////////////////////////////////
                ContentValues contentValues = new ContentValues();

                // Then add the data, along with the corresponding name of the data type,
                // so the content provider knows what kind of value is being inserted.
                contentValues.put(MovieEntry.API_MOVIE_ID, movieAPIID);
                contentValues.put(MovieEntry.MOVIE_TITLE, movieTitle);
                contentValues.put(MovieEntry.MOVIE_SYNOPSIS, movieSynopsis);
                contentValues.put(MovieEntry.MOVIE_POSTER, moviePoster);
                contentValues.put(MovieEntry.MOVIE_RELEASE_DATE, movieReleased);
                contentValues.put(MovieEntry.MOVIE_RATING, movieRating);


                cVVector.add(contentValues);


                //End bulk load////////////////////////////////////////////////////////////////
            }

            //Start bulk insert/////////////////////////////////////////////////////////////////
            int inserted = 0;
            // add to database
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);

                mContentResolver.bulkInsert(MoviesContract.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "Sync Page " + SessionManager.getQueryCurrentPage() + " Complete. " + cVVector.size() + " Inserted");

            PROCESSED_DATA = true;

            //End bulk insert/////////////////////////////////////////////////////////////////////

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return PROCESSED_DATA;
    }

    private boolean getFavoriteDataFromJson(String moviesJsonStr)
            throws JSONException {

        boolean PROCESSED_DATA = false;

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        final String MDB_MOVIES = "results";
        final String MDB_PAGES = "total_pages";
        final String MDB_API_ID = "id";
        final String MDB_TITLE = "original_title";
        final String MDB_SYNOPSIS = "overview";
        final String MDB_PATH_IMAGE = "poster_path";
        final String MDB_RELEASE_DATE = "release_date";
        final String MDB_USER_RATING = "vote_average";

        try {
            JSONObject movieJson = new JSONObject(moviesJsonStr);

            // Insert the new movie information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(1);

            //We only have to deal with one page of data here.
            SessionManager.resetQueryTotalPages();

            String movieAPIID = movieJson.getString(MDB_API_ID);
            String movieTitle = movieJson.getString(MDB_TITLE);
            String movieSynopsis = movieJson.getString(MDB_SYNOPSIS);
            String moviePoster = movieJson.getString(MDB_PATH_IMAGE);
            String movieReleased = movieJson.getString(MDB_RELEASE_DATE);
            String movieRating = movieJson.getString(MDB_USER_RATING);


            //Perform a bulk load//////////////////////////////////////////////////////////
            ContentValues contentValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            contentValues.put(MovieEntry.API_MOVIE_ID, movieAPIID);
            contentValues.put(MovieEntry.MOVIE_TITLE, movieTitle);
            contentValues.put(MovieEntry.MOVIE_SYNOPSIS, movieSynopsis);
            contentValues.put(MovieEntry.MOVIE_POSTER, moviePoster);
            contentValues.put(MovieEntry.MOVIE_RELEASE_DATE, movieReleased);
            contentValues.put(MovieEntry.MOVIE_RATING, movieRating);


            cVVector.add(contentValues);


            //End bulk load////////////////////////////////////////////////////////////////


            //Start bulk insert/////////////////////////////////////////////////////////////////
            int inserted = 0;
            // add to database
            if (cVVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);

                mContentResolver.bulkInsert(MoviesContract.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "Sync Page " + SessionManager.getQueryCurrentPage() + " Complete. " + cVVector.size() + " Inserted");

            PROCESSED_DATA = true;

            //End bulk insert/////////////////////////////////////////////////////////////////////

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return PROCESSED_DATA;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // we can enable inexact timers in our periodic sync
//            SyncRequest request = new SyncRequest.Builder().
//                    syncPeriodic(syncInterval, flexTime).
//                    setSyncAdapter(account, authority).
//                    setExtras(new Bundle()).build();
//            ContentResolver.requestSync(request);
//        } else {
        ContentResolver.addPeriodicSync(account,
                authority, new Bundle(), syncInterval);
//        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        FlicFinderSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        //Store account in global static variable
        //SYNC_ACCOUNT = newAccount;
        SessionManager.setSyncAccount(newAccount);
        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
