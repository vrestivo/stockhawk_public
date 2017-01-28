package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_UNKNOWN);

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = null;
            Iterator<String> iterator = null;

            try {
                quotes = YahooFinance.get(stockArray);
                iterator = stockCopy.iterator();
                Timber.d(quotes.toString());

            }
            catch (Exception error){
                if(error instanceof StringIndexOutOfBoundsException){
                    PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_DATA_ERROR);
                    return;
                }
                else{
                    PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_DATA_ERROR);
                    return;
                }
            }


            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator != null && iterator.hasNext()) {
                String symbol = iterator.next();

                //empty string check
                if (!symbol.isEmpty()) {

                    Stock stock = quotes.get(symbol);
                    if (stock != null && stock.getSymbol() != null) {


                        StockQuote quote = stock.getQuote();


                        if (quote.getPrice() != null &&
                                quote.getChange() != null &&
                                quote.getChangeInPercent() != null) {


                            float price = quote.getPrice().floatValue();
                            float change = quote.getChange().floatValue();
                            float percentChange = quote.getChangeInPercent().floatValue();


                            List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                            StringBuilder historyBuilder = new StringBuilder();

                            for (HistoricalQuote it : history) {
                                historyBuilder.append(it.getDate().getTimeInMillis());
                                historyBuilder.append(", ");
                                historyBuilder.append(it.getClose());
                                historyBuilder.append("\n");
                            }

                            ContentValues quoteCV = new ContentValues();
                            quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                            quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                            quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                            quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                            quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                            quoteCVs.add(quoteCV);

                            PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_OK);


                        } else {
                            //set pref status code not found
                            PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_NOT_FOUND);

                            PrefUtils.removeStock(context, symbol);

                            //just in case
                            context.getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);

                            PrefUtils.setNotFoundTicker(context, symbol);

                        }


                    } // end of if(stock!= null)

                } //end of if (!symbol.isEmpty())


            } //end of while (iterator.hasNext())

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);

            PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_SYNC_COMPLETE);

            context.sendBroadcast(dataUpdatedIntent);


        } catch (IOException exception) {

            if (exception instanceof FileNotFoundException) {
                PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_NOT_FOUND);

                //filter ticker out of url
                Uri uri = Uri.parse(exception.getMessage());
                String symbol = uri.getQueryParameter("s");

                //remove the ticker form the list
                PrefUtils.removeStock(context, symbol);

                //just in case
                context.getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);

                //stock not fond ticker
                PrefUtils.setNotFoundTicker(context, symbol);

            }
        }
    }

    private static void schedulePeriodic(Context context) {
        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {

            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);

        } else {

            PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_NO_NET_NO_DATA);
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.schedule(builder.build());


        }
    }


}
