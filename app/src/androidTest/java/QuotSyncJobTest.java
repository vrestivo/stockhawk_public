import android.content.ContentValues;
import android.content.Context;

import com.udacity.stockhawk.sync.QuoteSyncJob;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import yahoofinance.Stock;
import yahoofinance.histquotes.HistoricalQuote;

import static android.support.test.InstrumentationRegistry.getTargetContext;

/**
 * Created by devbox on 2/8/18.
 */

public class QuotSyncJobTest {
    private Stock stock;
    private Map<String, Stock> stockMap;
    private List<HistoricalQuote> stockHistory;
    private String ticker = "GOOG";
    private String[] stockTickers = {"GOOG", "TSLA", "AAPL", "LMT"};
    private Context mContext;


    @Before
    public void setup(){
        mContext = getTargetContext();
    }

    @After
    public void tearDown(){
        mContext = null;
    }

    @Test
    public void getQuotesFromYahooTest(){
        Map<String, Stock> quotes = QuoteSyncJob.getQuotesFromYahoo(mContext);
        Assert.assertNotNull("null quotes returned",quotes);
        Assert.assertTrue(quotes.size() > 0);
    }

    @Test
    public void parseQuotesIntoCVsTest(){
        Map<String, Stock> quotes = QuoteSyncJob.getQuotesFromYahoo(mContext);
        Assert.assertNotNull("null quotes returned",quotes);
        Assert.assertTrue(quotes.size() > 0);

        ArrayList<ContentValues> quoteContentValues = QuoteSyncJob.parseQuotesIntoCVs(quotes, mContext);
        Assert.assertNotNull(quoteContentValues);
        Assert.assertTrue(quoteContentValues.size() > 0);
    }




}
