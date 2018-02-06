package com.example.drken.flicfinder;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * This is the intro fragment that holds a button (Now Playing...) to kick of the application.
 * @author Dr. Ken
 */
public class MainFragment extends android.app.Fragment {


    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main,container,false);

        return rootView;
    }

}
