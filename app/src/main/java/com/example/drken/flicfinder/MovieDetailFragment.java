package com.example.drken.flicfinder;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.drken.flicfinder.data.APIContract;
import com.example.drken.flicfinder.data.FileHelper;
import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.data.MoviesContract.MovieEntry;
import com.example.drken.flicfinder.data.MoviesContract.MovieFavorite;
import com.example.drken.flicfinder.state.SessionManager;
import com.example.drken.flicfinder.utilities.ApplicationContextProvider;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MovieDetailFragment.OnDetailFragmentInteractionListener} interface
 * to handle interaction events.
 * This fragment is used to fetch and display detailed information for each movie selected from the gridview in the MoviesFragment.
 *
 * @author Dr. Ken
 */
public class MovieDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = MovieDetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";
    private Uri mUri; //Will hold Uri passed in to obtain individual record detail

    //Use this loader to fetch movie details
    private static CursorLoader mCursorLoader;
    private static final int DETAIL_LOADER = 0;

    //Use this loader to check favorites list
    private static CursorLoader mFavoriteLoader;
    private static final int FAVORITE_LOADER = 1;

    //Use these variable to load current record info
    private static String RECORD_ID = null;
    private static String RECORD_TITLE = null;
    private static String RECORD_SYNOPSIS = null;
    private static String RECORD_POSTER = null;
    private static String RECORD_RELEASE_DATE = null;
    private static String RECORD_RATING = null;

    private static boolean RECORD_IS_FAVORITE = false;

    private OnDetailFragmentInteractionListener mListener;

    //Views for layout
    private ImageView mPoster;
    private TextView mTitle;
    private TextView mReleaseDate;
    private TextView mRating;
    private TextView mSynopsis;
    private ImageButton mFavorite;
    private TextView mFavoriteLabel;
    private ListView mTrailerList;
    private LinearLayout mLinearLayoutFirst;

    //Movies Table in movies.db///////////////////////////////////////////
    private static final String[] DETAIL_COLUMNS = {
            MovieEntry.TABLE_MOVIES + "." + MovieEntry.MOVIE_ID,
            MovieEntry.API_MOVIE_ID,
            MovieEntry.MOVIE_TITLE,
            MovieEntry.MOVIE_SYNOPSIS,
            MovieEntry.MOVIE_POSTER,
            MovieEntry.MOVIE_RELEASE_DATE,
            MovieEntry.MOVIE_RATING,
            MovieEntry.MOVIE_IS_FAVORITE


    };

    public static final int COL_MOVIE_ID = 0;
    public static final int COL_API_ID = 1;
    public static final int COL_MOVIE_TITLE = 2;
    public static final int COL_SYNOPSIS = 3;
    public static final int COL_POSTER = 4;
    public static final int COL_RELEASE_DATE = 5;
    public static final int COL_RATING = 6;
    public static final int COL_FAVORITE = 7;
    //End Movies Table constants//////////////////////////////////////////

    //Favorites Table in movies.db////////////////////////////////////////
    private static final String[] FAVORITE_COLUMNS = {
            MovieFavorite.TABLE_FAVORITES + "." + MovieFavorite.FAVORITE_ID,
            MovieFavorite.FAVORITE_IMDB_ID,
            MovieFavorite.FAVORITE_TTTLE,
            MovieFavorite.FAVORTIE_SYNOPSIS,
            MovieFavorite.FAVORITE_POSTER,
            MovieFavorite.FAVORITE_RELEASE_DATE,
            MovieFavorite.FAVORITE_RATING
    };
    public static final int COL_FAVORITE_ID = 0;
    public static final int COL_FAVORITE_IMDB_ID = 1;
    public static final int COL_FAVORITE_TITLE = 2;
    public static final int COL_FAVORITE_SYNOPSIS = 3;
    public static final int COL_FAVORITE_POSTER = 4;
    public static final int COL_FAVORITE_RELEASE_DATE = 5;
    public static final int COL_FAVORITE_RATING = 6;
    //End Favorites Table constants///////////////////////////////////////

    View mRootview;

    //Let's use a special font for the detail view title
    Typeface mMovieFont;

    //Favorite Movie Flags
    private static boolean IS_FAVORITE = false;

    //Array to hold video ids
    private String[] videos;

    //Share-related for passing trailer link to share provider
    private static final String TRAILER_SHARE_HASHTAG = " #FlicFinder";
    private ShareActionProvider mShareActionProvider;
    private String mTrailer = null;
    private MenuItem mMenuItem;

    public MovieDetailFragment() {
        // Required empty public constructor
    }

    private Intent createShareTrailerIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mTrailer);
        //Uncomment for Twitter account
        //shareIntent.putExtra(Intent.EXTRA_TEXT, mTrailer + TRAILER_SHARE_HASHTAG);

        return shareIntent;
    }
    //TODO:  Verify need for this function to share info
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
       super.onCreateOptionsMenu(menu, inflater);

    if(SessionManager.isNetworkConnected()) {
        // Retrieve the share menu item

        mMenuItem = menu.findItem(R.id.menuShareTrailer);
        mMenuItem.setVisible(false);  //We won't show the share menu item unless there is a trailer to share.

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(mMenuItem);
    }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        mTrailer = null;
        //mMenuItem.setVisible(false);
        Log.d(LOG_TAG, "onPause()");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //If we are using two panes we will need the sort menu items to remain in the action bar.
        if(getActivity().findViewById(R.id.detail_container) == null && SessionManager.isNetworkConnected()) {

            menu.findItem(R.id.menuSortRating).setVisible(false);
            menu.findItem(R.id.menuSortPopular).setVisible(false);
            menu.findItem(R.id.menuSortNone).setVisible(false);
            menu.findItem(R.id.menuSortFavorites).setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Respond to the action bar's Up/Home button
        switch (item.getItemId()) {
            case android.R.id.home:
                //Use home button to back up one screen
                //--remove the home button before we leave
                ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.popBackStack();
                return true;
           // case R.id.menuShareTrailer:
                //Use Share Action Provider to share the trailer
            default:

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mRootview = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        mPoster = (ImageView) mRootview.findViewById(R.id.imageviewPoster);
        mTitle = (TextView) mRootview.findViewById(R.id.textviewTitleMovie);
        mReleaseDate = (TextView) mRootview.findViewById(R.id.textviewReleaseDate);
        mRating = (TextView) mRootview.findViewById(R.id.textviewRating);
        mSynopsis = (TextView) mRootview.findViewById(R.id.textviewSynopsis);
        mFavorite = (ImageButton) mRootview.findViewById(R.id.btnFavorite);
        mFavoriteLabel = (TextView)mRootview.findViewById(R.id.labelFavorite);
        mLinearLayoutFirst = (LinearLayout) mRootview.findViewById(R.id.detailLinearLayoutFirst);

        if(SessionManager.isNetworkConnected()) {
            //Set up any view handlers here
            //Set up handler for favorite declaration
            mFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (IS_FAVORITE) {
                        Log.d(LOG_TAG, "off");
                        IS_FAVORITE = false;
                        mFavorite.setColorFilter(Color.argb(150, 127, 127, 127));
                        mFavoriteLabel.setText(R.string.favoriteLabel);
                        //remove from database
                        ContentValues cv = new ContentValues();
                        String selection = MovieFavorite.FAVORITE_IMDB_ID + "=" + RECORD_ID;
                        getContext().getContentResolver().delete(MoviesContract.FAVORITES_URI, selection, null);


                        //Delete the poster from this file from internal storage.
                        Log.d(LOG_TAG, "Deleting file: " + RECORD_ID + ".jpg");
                        FileHelper fh = new FileHelper();
                        fh.deleteFile(getContext(), RECORD_ID);


                    } else {
                        Log.d(LOG_TAG, "on");
                        IS_FAVORITE = true;
                        mFavorite.setColorFilter(Color.argb(150, 255, 196, 0));
                        mFavoriteLabel.setText(R.string.favoriteLabelRemove);
                        //add to database
                        ContentValues cv = new ContentValues();
                        cv.put(MovieFavorite.FAVORITE_IMDB_ID, RECORD_ID);
                        cv.put(MovieFavorite.FAVORITE_TTTLE, RECORD_TITLE);
                        cv.put(MovieFavorite.FAVORTIE_SYNOPSIS, RECORD_SYNOPSIS);
                        cv.put(MovieFavorite.FAVORITE_POSTER, RECORD_POSTER);
                        cv.put(MovieFavorite.FAVORITE_RELEASE_DATE, RECORD_RELEASE_DATE);
                        cv.put(MovieFavorite.FAVORITE_RATING, RECORD_RATING);
                        Log.d(LOG_TAG, "Saving following record to favorites " + RECORD_ID);
                        getContext().getContentResolver().insert(MoviesContract.FAVORITES_URI, cv);

                        //Save poster image for offline use
                        Log.d(LOG_TAG, "Saving..." + RECORD_POSTER);
                        FileHelper fh = new FileHelper();
                        fh.downloadAndSave(Uri.parse(RECORD_POSTER), RECORD_ID, getContext());

                    }
                }
            });
        }else{
            mFavoriteLabel.setVisibility(View.INVISIBLE);
            mFavorite.setColorFilter(Color.argb(150, 255, 196, 0));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(MovieDetailFragment.DETAIL_URI);
        }

        //Get our special movie font and apply it to the text views
        mMovieFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Frijole-Regular.ttf");
        mTitle.setTypeface(mMovieFont);

        // Inflate the layout for this fragment
        return mRootview;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
        //Here we add the back button to the action bar so we can return to the previous gridview when finished with detail
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDetailFragmentInteractionListener) {
            mListener = (OnDetailFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDetailFragmentInteractionListener");
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case DETAIL_LOADER:
                if (null != mUri) {
                    // Now create and return a CursorLoader that will take care of
                    // creating a Cursor for the data being displayed.
                    mCursorLoader = new CursorLoader(
                            getActivity(),
                            mUri,
                            DETAIL_COLUMNS,
                            null,
                            null,
                            null
                    );
                    return mCursorLoader;
                }
                break;
            case FAVORITE_LOADER:
                Log.d(LOG_TAG, "Created Favorites Loader....");
                if (null != mUri) {
                    mFavoriteLoader = new CursorLoader(
                            getActivity(),
                            MoviesContract.FAVORITES_URI,
                            FAVORITE_COLUMNS,
                            null,
                            null,
                            null

                    );
                    return mFavoriteLoader;
                }
                break;
            default:
                break;

        }

        return null;
    }


    public void onLoadFinished(Loader loader, Cursor data) {
        switch (loader.getId()) {
            case DETAIL_LOADER:
                if (data != null && data.moveToFirst()) {

                    // Read data from cursor and update views
                    String title = data.getString(COL_MOVIE_TITLE);
                    mTitle.setText(title);
                    RECORD_TITLE = title;
                    String releaseDate = data.getString(COL_RELEASE_DATE);
                    mReleaseDate.setText("RELEASE DATE: " + releaseDate);
                    RECORD_RELEASE_DATE = releaseDate;
                    String rating = data.getString(COL_RATING);
                    mRating.setText("Voted " + rating + " out of 10");
                    RECORD_RATING = rating;
                    String synopsis = data.getString(COL_SYNOPSIS);
                    mSynopsis.setText(synopsis);
                    RECORD_SYNOPSIS = synopsis;
                    RECORD_ID = data.getString(COL_API_ID);
                    Log.d(LOG_TAG, "Creating detail page...RecordID and Title are " + RECORD_ID + ":" + RECORD_TITLE);

                    //Get trailers and reviews
                    if(RECORD_ID != null && SessionManager.isNetworkConnected()) {
                        //Get Trailers
                        FetchTrailers fetchTrailers = new FetchTrailers();
                        fetchTrailers.execute(RECORD_ID);

                        //Get Reviews
                        // Construct the URL for the MoviesDB query

                        //URLs for trailer fetches by movie id are constructed like this https://api.themoviedb.org/3/movie/<id>/reviews?api_key=65f9aa0115aaad281ad43d1586xxxxxx
                        //where <id> = movie id from Movie DB

                        final String MOVIES_BASE_URL =
                                "https://api.themoviedb.org/3/movie/" + RECORD_ID + "/reviews?";

                        final String APPID_PARAM = "api_key";


                        Uri builtUri;

                        builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                                .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                                .build();

                        try{
                            URL url = new URL(builtUri.toString());
                            FetchReviews fetchReviews = new FetchReviews();
                            fetchReviews.execute(url);
                        }catch(Exception e){
                            Log.e(LOG_TAG, "URL Exception", e);
                        }


                    }

                    //Scale poster image
                    mPoster.setScaleType(ImageView.ScaleType.FIT_XY);
                    mPoster.setPadding(4, 4, 4, 4);

                    if(SessionManager.isNetworkConnected()) {
                        //Get poster image from Picasso
                        final String IMAGE_PATH = "http://image.tmdb.org/t/p/w154";
                        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w75";
                        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w185";
                        final String APPID_PARAM = "api_key";

                        String path = data.getString(COL_POSTER);

                        Uri builtUri = Uri.parse(IMAGE_PATH.concat(path)).buildUpon()
                                .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                                .build();

                        RECORD_POSTER = builtUri.toString();

                        Picasso.with(getActivity()).load(builtUri).placeholder(R.drawable.noposter).into(mPoster);
                    }else{
                        String filename = RECORD_ID;

                        //Get file path of poster image
                        final File filePath = getContext().getFileStreamPath(filename);
                        Log.d(LOG_TAG, "Loading file...." + filePath);
                        if (filePath.exists()) {
                            FileHelper fh = new FileHelper();
                            Bitmap poster = fh.loadBitmapFromInternalStorage(getContext(), filePath);
                            Log.d(LOG_TAG, "File exists.");
                            mPoster.setImageBitmap(poster);
                        }
                    }

                    //Once we have populated the detail with the information from the movies table we can kick off a
                    //second loader to determine whether this title is in our favorites table.
                    getLoaderManager().initLoader(FAVORITE_LOADER, null, this);
                } else {
                    Log.d(LOG_TAG, "Data is null!");
                    //Sometimes we get errors back from the database and need some filler for the view.
                   mPoster.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.noposter));
                   mTitle.setText(R.string.connection_error_msg);
                }
                break;
            case FAVORITE_LOADER:
                Log.d(LOG_TAG, "I'm in onLoadfinished for favorites. ");
                boolean isEOF = false;
                if (data != null && data.moveToFirst()) {
                    while (!isEOF) {
                        String imdb = data.getString(COL_FAVORITE_IMDB_ID);
                        Log.d(LOG_TAG, "Here's a favorite ID-> " + imdb);
                        Log.d(LOG_TAG, "Record_ID equals ->" + RECORD_ID);
                        if (imdb.equals(RECORD_ID)) {
                            RECORD_IS_FAVORITE = true;
                            Log.d(LOG_TAG, "Is record a favorite?" + RECORD_IS_FAVORITE);

                            IS_FAVORITE = true;
                            mFavorite.setColorFilter(Color.argb(150, 255, 196, 0));
                            mFavoriteLabel.setText(R.string.favoriteLabelRemove);
                            isEOF = true;
                        } else {
                            IS_FAVORITE = false;
                            mFavorite.setColorFilter(Color.argb(150, 127, 127, 127));
                            mFavoriteLabel.setText(R.string.favoriteLabel);
                            if (data.isLast()) {
                                isEOF = true;
                            } else {
                                data.moveToNext();
                            }


                        }
                    }

                } else {
                    Log.d(LOG_TAG, "No data in favorites table!");
                }
                break;
            default:
                break;

        }

    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case DETAIL_LOADER:
                //Do something
                break;
            case FAVORITE_LOADER:
                //Do something
                break;
            default:
                break;

        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDetailFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void processFavorite(View view) {
        Log.d(LOG_TAG, "You marked a favorite!");
    }

    /**
     * This class will be used to retrieve any trailers that may come with the movie. AsyncTask<Params, Progress, Result>
     * We will pull down an array of trailer keys from the Movie DB, create an image button for each trailer, and launch
     * an intent with a URI to the YouTube location of the trailer when the button is pressed.
     */
    private class FetchTrailers extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

            return getTrailers(params[0]);
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);

            if(s.length == 0){
                if(mMenuItem != null) mMenuItem.setVisible(false);
            }else {
                if(mMenuItem != null) mMenuItem.setVisible(true);
            }


            for(String str: s){
                Log.d(LOG_TAG, "Your keys are ->" + str);

            }

            View.OnClickListener trailerClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String selected_item = (String) v.getTag();
                    //Tell the main activity that a trailer has been selected so we don't pop the fragment
                    //from the backstack while viewing the video.
                    onButtonPressed(Uri.parse(selected_item));

                    //Launch an implicit intent to handle the trailer viewing
                    Intent getTrailer = new Intent(Intent.ACTION_VIEW);
                    getTrailer.setData(Uri.parse(selected_item));
                    startActivity(getTrailer);
                }
            };

            //Add the Trailers divider line
            View view = new View(getContext());
            view.setMinimumHeight(1);
            view.setPadding(0, 30, 0, 0);
            view.setBackgroundColor(Color.WHITE);
            mLinearLayoutFirst.addView(view);

            TextView txtTrailers = new TextView(getContext());
            txtTrailers.setText("TRAILERS");
            txtTrailers.setTypeface(mMovieFont);
            txtTrailers.setShadowLayer(15f, 0f, 0f, Color.BLACK);
            txtTrailers.setTextSize(30f);
            txtTrailers.setGravity(Gravity.CENTER);
            txtTrailers.setTextColor(Color.WHITE);
            mLinearLayoutFirst.addView(txtTrailers);



            ImageButton[] imageButtons = new ImageButton[s.length];

            int i = 0;
            for(String str: s){
                String baseURL = "https://www.youtube.com/v/" + str;
                //TODO: Verify that this share code is working
                //Put this first video aside for the share intent
                if(mTrailer == null) mTrailer = baseURL;

                // If onCreateOptionsMenu has already happened, we need to update the share intent now.
                if (mShareActionProvider != null  && mTrailer != null) {
                    mShareActionProvider.setShareIntent(createShareTrailerIntent());

                }

                Uri builtUri;

                builtUri = Uri.parse(baseURL);

                imageButtons[i] = new ImageButton(getContext());
                imageButtons[i].setMinimumHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
                imageButtons[i].setMinimumWidth(LinearLayout.LayoutParams.WRAP_CONTENT);

                imageButtons[i].setPadding(0, 15, 0, 15);
                imageButtons[i].setImageResource(R.drawable.reel_icon);
                imageButtons[i].setOnClickListener(trailerClickListener);
                imageButtons[i].setBackgroundColor(Color.TRANSPARENT);
                imageButtons[i].setTag(baseURL);
                imageButtons[i].setId(i);
                mLinearLayoutFirst.addView(imageButtons[i]);
                i += 1;
            }

            //Build the Reviews Section
            //Add the Reviews divider line
            View viewII = new View(getContext());
            viewII.setMinimumHeight(1);
            viewII.setBackgroundColor(Color.WHITE);
            mLinearLayoutFirst.addView(viewII);

            TextView txtReviews = new TextView(getContext());
            txtReviews.setText("REVIEWS");
            txtReviews.setTypeface(mMovieFont);
            txtReviews.setTextSize(30f);
            txtReviews.setShadowLayer(15f, 0f, 0f, Color.BLACK);
            txtReviews.setTextColor(Color.WHITE);
            txtReviews.setGravity(Gravity.CENTER);
            mLinearLayoutFirst.addView(txtReviews);

        }

        private String[] getTrailers(String _movieID) {
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
    }

    private class FetchReviews extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {

            return getReviews(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);


            try{
                getReviewsFromJson(s);
            }catch (JSONException e){
                Log.e(LOG_TAG, "JSON Parsing Error.", e);
            }

        }

        private String getReviews(URL reviewurl) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            boolean RECEIVED_DATA = false;

            // Will contain the raw JSON response as a string.
            String reviewsJsonStr = null;


            try {


                // Create the request to MovieDB, and open the connection
                urlConnection = (HttpURLConnection) reviewurl.openConnection();
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
                reviewsJsonStr = buffer.toString();
                //trailers = getReviewsFromJson(moviesJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attempting
                // to parse it.

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


            return reviewsJsonStr;
        }

        private void getReviewsFromJson(String moviesJsonStr)
                throws JSONException {


            // These are the names of the JSON objects that need to be extracted.
            final String MDB_RESULTS = "results";
            final String MDB_AUTHOR = "author";
            final String MDB_CONTENT = "content";
            final String MDB_PAGES = "total_pages";
            final String MDB_PAGE = "page";

            try {
                JSONObject movieJson = new JSONObject(moviesJsonStr);
                int totalPages = movieJson.getInt(MDB_PAGES);
                int page = movieJson.getInt(MDB_PAGE);
                JSONArray movieArray = movieJson.getJSONArray(MDB_RESULTS);


                for (int i = 0, size = movieArray.length(); i < size; i++) {
                    JSONObject objectInArray = movieArray.getJSONObject(i);
                    String reviewAuthor = objectInArray.getString(MDB_AUTHOR);
                    String reviewContent = objectInArray.getString(MDB_CONTENT);

                    //Post reviews
                    TextView textView = new TextView(getContext());
                    textView.setText("AUTHOR: " + reviewAuthor);
                    textView.setPadding(0, 10, 0, 0);
                    textView.setShadowLayer(15f, 0f, 0f, Color.BLACK);
                    textView.setTypeface(mMovieFont);
                    textView.setTextSize(20f);
                    textView.setTextColor(Color.WHITE);
                    mLinearLayoutFirst.addView(textView);

                    TextView textViewContent = new TextView(getContext());
                    textViewContent.setText(reviewContent);
                    textViewContent.setPadding(0, 10, 0, 0);
                    textViewContent.setShadowLayer(15f,0f,0f,Color.BLACK);
                    textViewContent.setTextSize(20f);
                    textViewContent.setTextColor(Color.WHITE);
                    mLinearLayoutFirst.addView(textViewContent);

                    if(page < totalPages){
                        for(int p = page + 1; p <= totalPages; p++){

                            // Construct the URL for the MoviesDB query
                            //URLs for trailer fetches by movie id are constructed like this https://api.themoviedb.org/3/movie/<id>/reviews?api_key=65f9aa0115aaad281ad43d1586xxxxxx
                            //where <id> = movie id from Movie DB

                            final String MOVIES_BASE_URL =
                                    "https://api.themoviedb.org/3/movie/" + RECORD_ID + "/reviews?";

                            final String APPID_PARAM = "api_key";
                            final String PAGE_PARAM = "page";

                            Uri builtUri;

                            builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                                    .appendQueryParameter(PAGE_PARAM, Integer.toString(p))
                                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                                    .build();

                            try{
                                URL url = new URL(builtUri.toString());
                                FetchReviews fetchReviews = new FetchReviews();
                                fetchReviews.execute(url);
                            }catch(Exception e){
                                Log.e(LOG_TAG, "URL Exception", e);
                            }

                        }
                    }
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

        }
    }
}
