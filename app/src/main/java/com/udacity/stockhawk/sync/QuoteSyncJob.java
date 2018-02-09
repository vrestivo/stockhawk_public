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

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
        Map<String, Stock> yahooQuotes = getQuotesFromYahoo(context);
        updateQuotesInDB(parseQuotesIntoCVs(yahooQuotes, context), context);
    }

    public static Map<String, Stock> getQuotesFromYahoo(Context context) {
        PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_UNKNOWN);
        //TODO make this user defined
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        Map<String, Stock> quotes = null;
        Set<String> stockTickerSet = PrefUtils.getStocks(context);
        Iterator<String> iterator = stockTickerSet.iterator();

        if (stockTickerSet == null || stockTickerSet.size() < 1) {
            return null;
        }

        String[] quotesToGet = new String[stockTickerSet.size()];
        stockTickerSet.toArray(quotesToGet);
        try {
            quotes = YahooFinance.get(quotesToGet);
            //TODO make interval user-defined
            YahooFinance.get(quotesToGet, from, to, Interval.WEEKLY);
            Timber.d(quotes.toString());
        } catch (Exception exception) {
            if (exception instanceof StringIndexOutOfBoundsException) {
                PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_DATA_ERROR);
                return null;
            } else if (exception instanceof FileNotFoundException) {
                PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_NOT_FOUND);
                //filter ticker out of url
                Uri uri = Uri.parse(exception.getMessage());
                String symbol = uri.getQueryParameter("s");
                //remove the ticker form the list
                PrefUtils.removeStock(context, symbol);
                //TODO check this
                //just in case
                context.getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                //stock not fond ticker
                PrefUtils.setNotFoundTicker(context, symbol);
                return null;
            }
            //TODO check this
            else {
                PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_DATA_ERROR);
                return null;
            }
        }

        return quotes;
    }


    public static ArrayList<ContentValues> parseQuotesIntoCVs(Map<String, Stock> quotes, Context context) {
        ArrayList<ContentValues> quoteCVs = new ArrayList<>();

        if (quotes == null || quotes.size() < 1) {
            return quoteCVs;
        }

        Iterator<String> iterator = quotes.keySet().iterator();

        while (iterator != null && iterator.hasNext()) {
            String symbol = iterator.next();

            //empty string check
            if (!symbol.isEmpty() && quotes.containsKey(symbol)) {
                Stock stock = quotes.get(symbol);

                if (stock == null || stock.getQuote() == null) {
                    continue;
                }

                StockQuote quote = stock.getQuote();
                if (quote.getPrice() != null &&
                        quote.getChange() != null &&
                        quote.getChangeInPercent() != null) {

                    StringBuilder stockHistoryStringBuilder = new StringBuilder();

                    try {
                        List<HistoricalQuote> history = stock.getHistory();

                        for (HistoricalQuote it : history) {
                            stockHistoryStringBuilder.append(it.getDate().getTimeInMillis());
                            stockHistoryStringBuilder.append(", ");
                            stockHistoryStringBuilder.append(it.getClose());
                            stockHistoryStringBuilder.append("\n");
                        }
                    } catch (IOException ioexception) {
                        ioexception.printStackTrace();
                    }

                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, quote.getPrice().floatValue());
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, quote.getChange().floatValue());
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, quote.getChangeInPercent().floatValue());
                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, stockHistoryStringBuilder.toString());
                    quoteCVs.add(quoteCV);

                    PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_OK);
                } else {
                    //FIXME handle errors
                    //set pref status code not found
                    PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_NOT_FOUND);
                    PrefUtils.setNotFoundTicker(context, symbol);
                }
            } //end of if (!symbol.isEmpty())
        } //end of while (iterator.hasNext())

        return quoteCVs;
    }


    static void updateQuotesInDB(ArrayList<ContentValues> quoteContentValues, Context context) {
        if (quoteContentValues != null && quoteContentValues.size() > 0) {
            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteContentValues.toArray(new ContentValues[quoteContentValues.size()])
                    );
        }
        //TODO set correct status on failure
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
        PrefUtils.setPrefStatusCode(context, PrefUtils.STATUS_SYNC_COMPLETE);
        context.sendBroadcast(dataUpdatedIntent);
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


    //FIXME
    public static synchronized void syncImmediately(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

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
