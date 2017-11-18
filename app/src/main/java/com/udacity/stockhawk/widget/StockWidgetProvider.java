package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;

/**
 * Created by devbox on 1/19/17.
 */

public class StockWidgetProvider extends AppWidgetProvider {

    ContentResolver contentResolver;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        contentResolver = context.getContentResolver();

        for(int i =0; i < appWidgetIds.length; i++ ){
            int widgetId =  appWidgetIds[i];

            RemoteViews views = buildLayout(context, widgetId);
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);
            appWidgetManager.updateAppWidget(widgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

    }


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    private RemoteViews buildLayout(Context context, int widgetId){
        final Intent intent = new Intent(context, WidgetRemoteViewService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setRemoteAdapter(R.id.widget_list_view, intent);

        return remoteViews;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(QuoteSyncJob.ACTION_DATA_UPDATED)){
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.notifyAppWidgetViewDataChanged(
                    manager.getAppWidgetIds(new ComponentName(context, StockWidgetProvider.class)),
                    R.id.widget_list_view);
        }
    }

}


