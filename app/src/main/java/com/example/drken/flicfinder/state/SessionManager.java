package com.example.drken.flicfinder.state;

import android.accounts.Account;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Surface;

/**
 * This class will be used to keep track of session related data.
 * @author Dr. Ken
 */
public final class SessionManager {

    public static boolean isNetworkConnected() {
        return NETWORK_CONNECTED;
    }

    public static void setNetworkConnected(boolean networkConnected) {
        NETWORK_CONNECTED = networkConnected;
    }

    //Store connectivity staus
    private static boolean NETWORK_CONNECTED = true;

    //Store sync account created for Sync Adapter
    private static Account SYNC_ACCOUNT = null;

    //Data-related
    private static int QUERY_TOTAL_PAGES = 1;
    private static int QUERY_CURRENT_PAGE = 1;

    

    //Screen-related
    private static float sOriginalScreenPosition = Surface.ROTATION_0;

    public static Account getSyncAccount() {
        return SYNC_ACCOUNT;
    }

    public static void setSyncAccount(Account syncAccount) {
        SYNC_ACCOUNT = syncAccount;
    }

    public static int getQueryTotalPages() {
        return QUERY_TOTAL_PAGES;
    }

    private SessionManager(){

    }

    public static void setQueryTotalPages(int queryTotalPages) {
        QUERY_TOTAL_PAGES = queryTotalPages;
    }

    public static int getQueryCurrentPage() {
        return QUERY_CURRENT_PAGE;
    }

    public static void setQueryCurrentPage(int queryCurrentPage) {
        QUERY_CURRENT_PAGE = queryCurrentPage;
    }

    public static void resetQueryTotalPages(){
        QUERY_TOTAL_PAGES = 1;
    }

    public static void resetCurrentPage(){
        QUERY_CURRENT_PAGE = 1;
    }



    public static void incrementCurrentPage(){
        if(QUERY_CURRENT_PAGE < QUERY_TOTAL_PAGES) QUERY_CURRENT_PAGE += 1;
    }

    public static void decrementCurrentPage(){
        if(QUERY_CURRENT_PAGE > 1) QUERY_CURRENT_PAGE -= 1;
    }

    public static float getsOriginalScreenPosition() {
        return sOriginalScreenPosition;
    }

    public static void setsOriginalScreenPosition(float sOriginalScreenPosition) {
        SessionManager.sOriginalScreenPosition = sOriginalScreenPosition;
    }


}
