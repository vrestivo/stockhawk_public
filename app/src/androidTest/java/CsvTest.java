import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.DropBoxManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.udacity.stockhawk.data.Contract;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVParser;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static java.lang.Float.parseFloat;
import static org.junit.Assert.*;

/**
 * Created by devbox on 1/13/17.
 */
@RunWith(JUnit4.class)
public class CsvTest {

    private static final String[] projection =
            new String[]{Contract.Quote.COLUMN_SYMBOL, Contract.Quote.COLUMN_HISTORY};
    Context context;

    public static String getRawPriceHistory(Context context, @NonNull String ticker){

        String LOG_TAG = "_getRawPriceHistory";

        if(ticker==null){
            return null;
        }

        ContentResolver cr = context.getContentResolver();

        Uri stockUri = Contract.Quote.makeUriForStock(ticker);

        //TODO delete logging
        Log.v(LOG_TAG, projection[0] + "," + projection[1]);

        Cursor results = cr.query(
                stockUri,
                projection,
                null,
                null,
                null,
                null
        );

        Assert.assertNotNull("_returned null cursor", results);



        int count = results.getCount();

        //TODO delete logging
        Log.v(LOG_TAG, String.valueOf(count));

        Assert.assertTrue("error moving to first", results.moveToFirst());

        String rawCsv = results.getString(1);

        CSVParser csvparser = new CSVParser();


        String[] data = rawCsv.split("[\n]");

        //TODO delete logging
        Log.v(LOG_TAG, "_entries:" + String.valueOf(data.length));

        for (String value : data){
            System.out.println(value);
        }

        Assert.assertNotNull("_no csv results", results);

        //close cursor
        results.close();

        System.out.print("_" + ticker +" :" + rawCsv);


        return rawCsv;

    }

    @Test
    public void stockHistroryRetrievalTest(){
        Context myContext = getTargetContext();

        String rawCsv = getRawPriceHistory(myContext, "AAPL");

        if(rawCsv!=null){

        }

        List<Entry> entries = rawCSVToChartEntries(rawCsv);

        Assert.assertNotNull("_entries are null", entries);

        for(Entry entry : entries){
            System.out.print(entry.getX() + " ");
            System.out.println(entry.getY());

        }

    }


    public List<Entry> rawCSVToChartEntries(@NonNull String rawData){
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