package com.example.drken.flicfinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import com.example.drken.flicfinder.state.SessionManager;
import com.example.drken.flicfinder.utilities.ScreenDimenUitility;

/**
 * In the future code can be added to this class for handling user authentication.  Right now this class
 * will only use the ScreenDimenUtility class to determine screen dimensions and decide which layouts will be
 * most appropriate.
 *
 * @author Dr. Ken
 */
public class LoginActivity extends AppCompatActivity {

    ScreenDimenUitility mSdu;

    MainFragment mMainFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Login", "Login created!");
        setContentView(R.layout.login);

        mMainFragment = new MainFragment();
        if(savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.login_container, mMainFragment).addToBackStack("MainFragment").commit();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Are we connected to a network?
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        SessionManager.setNetworkConnected(isConnected);

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //Function to replace application intro screen with gridview of movie titles once Now Playing... is pressed
    public void findFilms(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }
}
