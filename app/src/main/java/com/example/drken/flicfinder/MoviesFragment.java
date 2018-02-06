package com.example.drken.flicfinder;


import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.data.MoviesContract.MovieFavorite;
import com.example.drken.flicfinder.data.MoviesProvider;
import com.example.drken.flicfinder.presentation.GridViewCursorAdapter;
import com.example.drken.flicfinder.presentation.GridViewCursorAdapterOffline;
import com.example.drken.flicfinder.state.SessionManager;
import com.example.drken.flicfinder.sync.FlicFinderSyncAdapter;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MoviesFragment.OnMovieSelectedListener} interface
 * to handle interaction events.
 * This fragment is used to fetch and display movie titles.
 * @author Dr. Ken
 */
public class MoviesFragment extends android.app.Fragment implements android.app.LoaderManager.LoaderCallbacks<Cursor>,FragmentManager.OnBackStackChangedListener {

    public final String LOG_TAG = MoviesFragment.class.getSimpleName();

    private GridViewCursorAdapterOffline mGridCursorAdapterOffline;
    private GridViewCursorAdapter mGridCursorAdapter;

    public static CursorLoader mCursorLoader;
    public static CursorLoader mFavoriteLoader;
    public static CursorLoader mOfflineFavoriteLoader;

    GridView mGridView;

    private static final int MOVIE_LOADER_ID = 0;
    private static final int FAVORITE_LOADER_ID = 1;
    private static final int FAVORITE_OFFLINE_LOADER_ID = 2; //Use this loader when we don't have a network connection.

    private static boolean IS_QUERY_CHANGE = false;
    private static boolean PROCESSING_FAVORITES_QUERY = true;

    OnMovieSelectedListener mListener;

    //Action Bar Titles

    public static final String NO_SORT_TITLE = "Flic Finder Movies";
    public static final String POPULAR_TITLE = "Most Popular Movies";
    public static final String RATING_TITLE = "Highest Rated Movies";
    public static final String FAVORITE_TITLE = "Favorite Movies";
    public static final String OFFLINE = "OFFLINE - Favorite Movies";
    public static String CURRENT_SORT = NO_SORT_TITLE;

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

    public MoviesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onBackStackChanged() {

    }

    // Container Activity must implement this interface
    public interface OnMovieSelectedListener {
        public void onMovieSelected(Uri movieUri);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        //When used in a fragment init cursor loader in this callback
        if(SessionManager.isNetworkConnected()) {
            getLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);
            super.onActivityCreated(savedInstanceState);
            if (savedInstanceState != null)
                CURRENT_SORT = savedInstanceState.getString("CurrentSort", NO_SORT_TITLE);

        }else{
            getLoaderManager().initLoader(FAVORITE_OFFLINE_LOADER_ID, null, this);
            super.onActivityCreated(savedInstanceState);
            CURRENT_SORT = OFFLINE;
        }
        Log.d(LOG_TAG, "onActivityCreated()");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(CURRENT_SORT);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("CurrentSort", CURRENT_SORT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(LOG_TAG,"onCreateView()");
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_movies, container, false);
        if(SessionManager.isNetworkConnected()) {
            //Load the view with data from the DB
            mGridCursorAdapter = new GridViewCursorAdapter(getActivity(), null, 0);
            mGridView = (GridView) rootView.findViewById(R.id.gridview);
            mGridView.setAdapter(mGridCursorAdapter);

            mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // CursorAdapter returns a cursor at the correct position for getItem(), or null
                    // if it cannot seek to that position.
                    Cursor c = (Cursor) parent.getAdapter().getItem(position);
                    if (c != null) {
                        int entryID = c.getInt(c.getColumnIndex(MoviesContract.MovieEntry.MOVIE_ID));
                        // Append the clicked item's row ID with the content provider Uri
                        Uri movieUri = ContentUris.withAppendedId(MoviesContract.CONTENT_URI, id);
                        Log.d(LOG_TAG, "Passing this Uri to Main--->" + movieUri.toString());
                        ((OnMovieSelectedListener) getActivity()).onMovieSelected(movieUri);
                    }
                }
            });
        }else{
            //Load the view with data from the DB
            mGridCursorAdapterOffline = new GridViewCursorAdapterOffline(getActivity(), null, 0);
            mGridView = (GridView) rootView.findViewById(R.id.gridview);
            mGridView.setAdapter(mGridCursorAdapterOffline);

            mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // CursorAdapter returns a cursor at the correct position for getItem(), or null
                    // if it cannot seek to that position.
                    Cursor c = (Cursor) parent.getAdapter().getItem(position);
                    if (c != null) {
                        int entryID = c.getInt(c.getColumnIndex(MovieFavorite.FAVORITE_ID));
                        // Append the clicked item's row ID with the content provider Uri
                        Uri favoriteUri = ContentUris.withAppendedId(MoviesContract.FAVORITES_URI, id);
                        Log.d(LOG_TAG, "Passing this Favorite Uri to Main--->" + favoriteUri.toString());
                        ((OnMovieSelectedListener) getActivity()).onMovieSelected(favoriteUri);
                    }
                }
            });
        }

        return rootView;
    }



    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "onCreateLoader()");

        CursorLoader crs = null;
         switch (id) {
            case MOVIE_LOADER_ID:
                mCursorLoader = new android.content.CursorLoader(getActivity(), MoviesProvider.CONTENT_URI, null, null, null, null);
                crs = mCursorLoader;
                break;
            case FAVORITE_LOADER_ID:
                Log.d(LOG_TAG, "Created Favorites Loader....");
                mFavoriteLoader = new android.content.CursorLoader(getActivity(), MoviesContract.FAVORITES_URI, FAVORITE_COLUMNS, null, null, null );
                crs = mFavoriteLoader;
                break;
             case FAVORITE_OFFLINE_LOADER_ID:
                 Log.d(LOG_TAG, "Created Offline Favorites Loader....");
                 mOfflineFavoriteLoader = new android.content.CursorLoader(getActivity(), MoviesContract.FAVORITES_URI, FAVORITE_COLUMNS, null, null, null );
                 crs = mOfflineFavoriteLoader;
                 break;

            default:
                break;

        }
        return crs;

    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()){
            case MOVIE_LOADER_ID:
                if(mGridCursorAdapter!=null && data!=null){

                    mGridCursorAdapter.swapCursor(data);
                    //Refresh the view now that we have new data.
                    mGridCursorAdapter.notifyDataSetChanged();
                    //Don't move the gridview scroll position back to 0 unless we are performing a new query.
                    if (IS_QUERY_CHANGE) {
                        mGridView.smoothScrollToPosition(0);
                        IS_QUERY_CHANGE = false;
                    }


                }else{
                    Log.d(LOG_TAG, "onLoadFinished() and adapter is null");
                }

                Log.d(LOG_TAG, "onLoadFinished()");
                break;
            case FAVORITE_LOADER_ID:
                if(SessionManager.isNetworkConnected()) {
                    ArrayList<String> aryFavorites = new ArrayList<>();
                    boolean eof = false;
                    if (data != null && data.moveToFirst()) {
                        while (!eof) {
                            String str = data.getString(COL_FAVORITE_IMDB_ID);
                            aryFavorites.add(str);
                            if (data.isLast()) {
                                eof = true;
                            } else {
                                data.moveToNext();
                            }


                        }

                        FlicFinderSyncAdapter.myFavorites = aryFavorites.toArray(new String[aryFavorites.size()]);
                        PROCESSING_FAVORITES_QUERY = false;
                    }

                    //Request a new sync from the sync adapter now that the sort order has been changed
                    Bundle settings = new Bundle();
                    settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                    settings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                    CURRENT_SORT = FAVORITE_TITLE;
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(FAVORITE_TITLE);
                    getLoaderManager().destroyLoader(FAVORITE_LOADER_ID);
                    FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_FAVORITE_SORT;
                    ContentResolver.requestSync(SessionManager.getSyncAccount(), MoviesContract.CONTENT_AUTHORITY, settings);
                }
                break;
            case FAVORITE_OFFLINE_LOADER_ID:
                Log.d(LOG_TAG,"Working offline.  Loader has returned with data.");
                //We are working offline so we will not use the Sync Adapter to get our details.
                if(mGridCursorAdapterOffline != null && data!=null){
                    Log.d(LOG_TAG, "Data is not null.  Swapping cursor and updating view.");
                    mGridCursorAdapterOffline.swapCursor(data);
                    //Refresh the view now that we have new data.
                    mGridCursorAdapterOffline.notifyDataSetChanged();

                }else{
                    Log.d(LOG_TAG, "offline onLoadFinished() and adapter is null");
                }

                break;
            default:
                break;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");

    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        switch (loader.getId()) {
            case MOVIE_LOADER_ID:
                if (mGridCursorAdapter != null) {
                    mGridCursorAdapter.swapCursor(null);
                } else {
                    Log.d(LOG_TAG, "onLoadReset() and adapter is null");
                }
                Log.d(LOG_TAG, "onLoadReset()");
                break;
            case FAVORITE_LOADER_ID:
                if (mGridCursorAdapter != null) {
                    mGridCursorAdapter.swapCursor(null);
                } else {
                    Log.d(LOG_TAG, "onLoadReset() and adapter is null");
                }
                Log.d(LOG_TAG, "onLoadReset()");
                break;
            case FAVORITE_OFFLINE_LOADER_ID:
                if (mGridCursorAdapterOffline != null) {
                    mGridCursorAdapterOffline.swapCursor(null);
                } else {
                    Log.d(LOG_TAG, "onLoadReset() and offline adapter is null");
                }
                Log.d(LOG_TAG, "onLoadReset()");
                break;
            default:
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(SessionManager.isNetworkConnected()) {
            //Clear any pre-existing menu items
            menu.clear();
            //Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.movies_fragment, menu);
            //Remove the share menu item - This will only be used on the detail screen
            MenuItem item = menu.findItem(R.id.menuShareTrailer);
            item.setVisible(false);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //The menu options are used to set the static sort mode flag in the FlicFinderSyncAdapter
        //to indicate the order of the movies we want to receive.

        //Request a new sync from the sync adapter now that the sort order has been changed
        Bundle settings = new Bundle();
        settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        if (id == R.id.menuSortNone) {
            IS_QUERY_CHANGE = true;
            CURRENT_SORT = NO_SORT_TITLE;
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(NO_SORT_TITLE);;
            FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_NO_SORT;
            ContentResolver.requestSync(SessionManager.getSyncAccount(), MoviesContract.CONTENT_AUTHORITY, settings);
            return true;
        }
        if (id == R.id.menuSortPopular) {
            IS_QUERY_CHANGE = true;
            CURRENT_SORT = POPULAR_TITLE;
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(POPULAR_TITLE);;
            FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_POPULAR_SORT;
            ContentResolver.requestSync(SessionManager.getSyncAccount(), MoviesContract.CONTENT_AUTHORITY, settings);
            return true;
        }
        if (id == R.id.menuSortRating) {
            IS_QUERY_CHANGE = true;
            CURRENT_SORT = RATING_TITLE;
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(RATING_TITLE);;
            FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_RATINGS_SORT;
            ContentResolver.requestSync(SessionManager.getSyncAccount(), MoviesContract.CONTENT_AUTHORITY, settings);
            return true;
        }
        if(id == R.id.menuSortFavorites){
            IS_QUERY_CHANGE = true;
            getLoaderManager().initLoader(FAVORITE_LOADER_ID, null, this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
