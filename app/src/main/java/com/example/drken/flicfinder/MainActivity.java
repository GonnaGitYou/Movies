package com.example.drken.flicfinder;

import android.accounts.Account;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.example.drken.flicfinder.data.MoviesContract;
import com.example.drken.flicfinder.state.SessionManager;
import com.example.drken.flicfinder.sync.FlicFinderSyncAdapter;
import com.example.drken.flicfinder.utilities.ScreenDimenUitility;

/**
 * The FlicFinder application was written to retrieve current information on movies provided by the MovieDB API.
 * @author Dr. Ken
 */
public class MainActivity extends AppCompatActivity implements MoviesFragment.OnMovieSelectedListener, MovieDetailFragment.OnDetailFragmentInteractionListener{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    MoviesFragment mMovieFragment;
    MainFragment mMainFragment;
    MovieDetailFragment mMovieDetailFragment;

    private boolean isTwoPanel = false;

    private boolean shouldRemoveDetailFragment = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(findViewById(R.id.detail_container) != null){
            isTwoPanel = true;
        }
        Log.d("Main", "isTwoPanel is now" + isTwoPanel);

        mMovieFragment = new MoviesFragment();
        mMainFragment = new MainFragment();



        if(SessionManager.isNetworkConnected()){
            FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_NO_SORT;
        }

        Log.d("Main", "The saved instance state is " + (savedInstanceState == null));
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.main_container, mMovieFragment).addToBackStack("MoviesFragment").commit();
            if(SessionManager.isNetworkConnected()) {
                FlicFinderSyncAdapter.initializeSyncAdapter(this);
                //Request a new sync from the sync adapter now that the sort order has been changed
                Bundle settings = new Bundle();
                settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                settings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                MoviesFragment.CURRENT_SORT = MoviesFragment.NO_SORT_TITLE;
                this.getSupportActionBar().setTitle(MoviesFragment.NO_SORT_TITLE);
                ;
                FlicFinderSyncAdapter.SORT_MODE = FlicFinderSyncAdapter.MOVIES_NO_SORT;
                ContentResolver.requestSync(SessionManager.getSyncAccount(), MoviesContract.CONTENT_AUTHORITY, settings);
            }
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

    }



    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MAIN", "IN onPause()");
        ContentResolver.cancelSync(new Account(getApplicationContext().getString(R.string.app_name), getApplicationContext().getString(R.string.sync_account_type)), MoviesContract.CONTENT_AUTHORITY);
        //The detail fragment may enter a pause state when we choose to share a trailer or when we rotate the device.  We only
        //want to remove the fragment if we are rotating the device and changing the layout.

        if (mMovieDetailFragment != null && shouldRemoveDetailFragment) {
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .remove(mMovieDetailFragment)
                    .commit();

        }else if(!shouldRemoveDetailFragment){
            shouldRemoveDetailFragment = true;
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MAIN", "IN onStop()");

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
     }

    @Override
    public void onMovieSelected(Uri movieUri) {
        //Take the Uri of the movie selected in the MoviesFragment and pass it to the MovieDetailFragment
        Bundle args = new Bundle();
        args.putParcelable(MovieDetailFragment.DETAIL_URI, movieUri);
        mMovieDetailFragment = new MovieDetailFragment();
        mMovieDetailFragment.setArguments(args);
        Log.d("Main", "isTwoPanel is " + isTwoPanel);
        if(isTwoPanel ) {
            getSupportFragmentManager().beginTransaction().replace(R.id.detail_container, mMovieDetailFragment).addToBackStack("MovieDetailFragmentTwoPane").commit();
        }else{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container, mMovieDetailFragment).addToBackStack("MovieDetailFragment").commit();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(LOG_TAG,"Poster selected-> " + uri.toString());
        shouldRemoveDetailFragment = false;
    }
}
