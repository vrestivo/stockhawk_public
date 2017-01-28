package com.udacity.stockhawk.data;

import android.support.annotation.NonNull;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for data manipulation
 */

public class DataUtil {


    /**
     * converts raw historical stock data in CSV format
     * to List<Entry> usable for the Chart class
     * @param rawData raw stock price data in CSV format
     * @return entries in List<Entry> format, readty for the
     * Chart class consumption
     */
    public static List<Entry> rawCSVToChartEntries(@NonNull String rawData){
        String LOG_TAG = "rawCSVToChart: ";
        String[] lineArray;
        List<Entry> entries = new ArrayList<Entry>();

        if(rawData!=null){
            lineArray = rawData.split("[\n]");
            for(String valuePair : lineArray){
                String[] pairSplit = valuePair.split("[,]");
                Long date = Long.parseLong(pairSplit[0].trim());
                Float price = Float.parseFloat(pairSplit[1].trim());
                entries.add(new Entry(date, price));
            }
            return entries;
        }
        return null;
    }

}
