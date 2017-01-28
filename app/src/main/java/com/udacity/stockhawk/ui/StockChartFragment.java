package com.udacity.stockhawk.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.ChartXValueFormatter;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.DataUtil;

import java.util.Collections;
import java.util.List;

import static com.github.mikephil.charting.charts.Chart.LOG_TAG;

/**
 * will be used to display stock price over time
 */

public class StockChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor
        > {

    private static final String[] projection =
            new String[]{Contract.Quote.COLUMN_SYMBOL, Contract.Quote.COLUMN_HISTORY};

    private static final int LOADER_ID = 321;

    private String ticker;
    private List<Entry> chartEntries;
    private LineChart chart;
    private String xLabel;
    private String yLabel;





    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        xLabel = getString(R.string.chart_x_axis_label);
        yLabel = getString(R.string.chart_y_axis_label);


        View rootView;

        if (MainActivity.isTwoPane()){
            ticker = getArguments().getString(MainActivity.TICKER);
        }
        else {
            Intent receivedIntent = getActivity().getIntent();
            if (receivedIntent != null && receivedIntent.hasExtra(MainActivity.TICKER)) {
                ticker = receivedIntent.getStringExtra(MainActivity.TICKER);
            }
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rootView = inflater.inflate(R.layout.chart_fragment, container, false);
            chart = (LineChart) rootView.findViewById(R.id.line_chart);
            formatChart(chart, false);
        }
        else{
            rootView = inflater.inflate(R.layout.chart_fragment, container, false);
            chart = (LineChart) rootView.findViewById(R.id.line_chart);
            formatChart(chart, true);
        }

        return rootView;

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if(ticker!=null) {
            Uri stockUri = Contract.Quote.makeUriForStock(ticker);

            ContentResolver cr = getContext().getContentResolver();

            return new CursorLoader(
                    getActivity().getApplicationContext(),
                    stockUri,
                    projection,
                    null,
                    null,
                    null
            );
        }
        return null;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data !=null && data.moveToFirst()){
            String rawData = data.getString(Contract.COLUMN_RAW_CSV);
            if(rawData!=null && rawData.length()>0){
                chartEntries = DataUtil.rawCSVToChartEntries(rawData);

                //sort entries
                Collections.sort(chartEntries, new EntryXComparator());
                refreshChartData(chartEntries);
            }
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        refreshChartData(null);

    }

    public void refreshChartData(List<Entry> entries){
        if(entries!=null) {
            LineDataSet dataSet = new LineDataSet(entries, ticker);
            LineData lineData = new LineData(dataSet);
            dataSet.setColor(getResources().getColor(R.color.stock_hawk_udacity_blue_500));
            chart.setData(lineData);
            chart.invalidate();
        }
        else {
            chart.setData(null);
            chart.invalidate();
        }
    }

    private void formatChart(LineChart chart, boolean phonePortrait){
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ChartXValueFormatter());

        chart.setKeepPositionOnRotation(true);
        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.setAutoScaleMinMaxEnabled(true);
        if(phonePortrait){
            xAxis.setLabelCount(5, true);
        }
    }

    public void changeTicker(String newTicker){
        if(ticker!=null){
            ticker = newTicker;
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    public void clearChart(){
        if(chart!= null){
            chart.clear();
        }

    }

}
