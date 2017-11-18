package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by devbox on 1/20/17.
 */

public class WidgetRemoteViewService extends RemoteViewsService {


    public WidgetRemoteViewService() {
        super();

    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetRemoteViewFactory(this.getApplicationContext(), intent);
    }

}

class WidgetRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Intent mIntent;
    private int mWidgetId;
    private Cursor mCursor;
    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;


    public static final String HANLDER_THREAD_HANDLE = "HANDLER_THREAD_HANDLE";
    private static HandlerThread sHandlerThread;
    private static Handler sHandler;


    public WidgetRemoteViewFactory(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");


        sHandlerThread = new HandlerThread(HANLDER_THREAD_HANDLE);
        sHandlerThread.start();
        sHandler = new Handler(sHandlerThread.getLooper());


    }

    @Override
    public void onCreate() {
        //nothing here. data is handled in onDataSetChanged
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        Uri uri = Contract.Quote.URI;


        ContentResolver contentResolver = mContext.getContentResolver();

        final long token = Binder.clearCallingIdentity();
        try {

            mCursor = contentResolver
                    .query(Contract.Quote.URI,
                            Contract.Quote.QUOTE_COLUMNS,
                            null,
                            null,
                            Contract.Quote.COLUMN_SYMBOL);
        } finally {
            Binder.restoreCallingIdentity(token);
        }

    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }

    }

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public RemoteViews getViewAt(int i) {

        String symbol = "****";
        String price = "0";
        String change = "0%";
        float priceChange = 0.0F;
        float percentageChange = 0.0F;

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_widget);


        if (mCursor.moveToPosition(i)) {
            symbol = mCursor.getString(Contract.Quote.POSITION_SYMBOL);
            priceChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
            price = dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE));
        }

        if(priceChange < 0){
            remoteViews.setInt(R.id.widget_list_change_tv, "setBackgroundResource", R.drawable.percent_change_pill_red);
        }
        else {
            remoteViews.setInt(R.id.widget_list_change_tv, "setBackgroundResource", R.drawable.percent_change_pill_green);

        }

        if(PrefUtils.getDisplayMode(mContext).equals(mContext.getString(R.string.pref_display_mode_absolute_key))){
            change = dollarFormat.format(priceChange);
        }else {
            change = percentageFormat.format(percentageChange / 100);

        }

        remoteViews.setTextViewText(R.id.widget_list_symbol_tv, symbol);
        remoteViews.setTextViewText(R.id.widget_list_price_tv, price);
        remoteViews.setTextViewText(R.id.widget_list_change_tv, change);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}





