package com.udacity.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.StringDef;
import android.util.Log;

import com.udacity.stockhawk.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public final class PrefUtils {

    public static final int STATUS_OK = 0;
    public static final int STATUS_NOT_FOUND = 1;
    public static final int STATUS_NO_NET_NO_DATA = 2;
    public static final int STATUS_NO_NET_OLD_DATA = 3;
    public static final int STATUS_DATA_ERROR = 4;
    public static final int STATUS_SYNC_COMPLETE = 5;
    public static final int STATUS_UNKNOWN = 6;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK,
            STATUS_NOT_FOUND,
            STATUS_NO_NET_NO_DATA,
            STATUS_NO_NET_OLD_DATA,
            STATUS_DATA_ERROR,
            STATUS_SYNC_COMPLETE,
            STATUS_UNKNOWN
    })
    public @interface StatusCode{}


    private PrefUtils() {
    }

    public static Set<String> getStocks(Context context) {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);

        HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));
        SharedPreferences prefs = getDefaultSharedPreferences(context);

        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }

        Set <String> stocks = prefs.getStringSet(stocksKey, defaultStocks);

        return stocks;


    }

    private static void editStockPref(Context context, String symbol, Boolean add) {
        String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks_set = getStocks(context);

        //converting returned Set<String> instance from sharedPreferences
        //in order to propertly persiste changes as per android documentation:
        //https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%20java.util.Set%3Cjava.lang.String%3E)
        ArrayList<String> stocks = new ArrayList<String>(stocks_set);

        if (add) {
            stocks.add(symbol);

        } else {
            stocks.remove(symbol);
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> stock_set_to_store = new HashSet<String>(stocks);

        editor.putStringSet(key, stock_set_to_store);
        editor.apply();
    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) {
            editor.putString(key, percentageKey);
        } else {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }

    /**
     * changes status preference to supplied status code
     * @param context
     * @param statusCode status code to change preference to
     */
    public static void setPrefStatusCode(Context context, @StatusCode int statusCode){
        //getting prefs from a pref manager
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = preferences.edit();
        prefEditor.putInt(context.getString(R.string.pref_status_key), statusCode);

        //if running on UI thread use apply(), else use commit()
        if(Looper.myLooper() == Looper.getMainLooper()){
            prefEditor.apply();
        }
        else{
            prefEditor.commit();
        }

    }


    /**
     * stores the symbol of the stock for which data was not found
     * @param context
     * @param ticker stock symbol
     */
    public static void setNotFoundTicker(Context context, String ticker){

        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefEditor.putString(context.getString(R.string.pref_not_found_key), ticker);

        //if running on UI thread use apply(), else use commit()
        if(Looper.myLooper() == Looper.getMainLooper()){
            prefEditor.apply();
        }
        else{
            prefEditor.commit();
        }
    }

    /**
     * return status preference
     * @param context
     * @return
     */
    public static int getStatus(Context context){

        int status = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.pref_status_key), STATUS_UNKNOWN);
        
        return status;
    }

}
