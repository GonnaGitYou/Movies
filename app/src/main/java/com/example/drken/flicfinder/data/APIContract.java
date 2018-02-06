package com.example.drken.flicfinder.data;

import android.content.Context;
import android.net.Uri;

import com.example.drken.flicfinder.BuildConfig;

/**
 * This class is used to build Uri strings that match the API documentation.
 * @author Dr. Ken
 */
public class APIContract {

    public APIContract(){

    }

    /**
     * Build a URI to obtain a poster image from API.
     * @param posterPath The poster path provided by the discover/movie query.
     * @return URI to retrieve poster from API.
     */
    public Uri getPosterURI(String posterPath){
        Uri uriPoster;
        final String IMAGE_PATH = "http://image.tmdb.org/t/p/w154";   //Comment out to change image size
        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w75";  //Uncomment to change image size
        //final String IMAGE_PATH = "http://image.tmdb.org/t/p/w185"; //Uncomment to change image size
        final String APPID_PARAM = "api_key";
        uriPoster = Uri.parse(IMAGE_PATH.concat(posterPath)).buildUpon()
                .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                .build();

        return uriPoster;
    }
}
