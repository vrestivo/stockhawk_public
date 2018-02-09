import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;

/**
 * Created by devbox on 2/8/18.
 */

public class YahooFinanceApiTest {

    private Stock stock;
    private Map<String, Stock> stockMap;
    private List<HistoricalQuote> stockHistory;
    private String ticker = "GOOG";
    private String[] stockTickers = {"GOOG", "TSLA", "AAPL", "LMT"};

    @Before
    public void setup() {
        //TODO implement as needed
    }

    @Test
    public void getSingleSockQuoteTest(){
        try {
            stock = YahooFinance.get(ticker);
            stockHistory = stock.getHistory();
            for (HistoricalQuote quote : stockHistory){
                System.out.println(quote.toString());
            }
        }
        catch (IOException ioe){
            //TODO handle here
            ioe.printStackTrace();
        }

        Assert.assertNotNull("YahooFinance returned null data", stock);
        Assert.assertEquals("Stock symbol does not match",
                ticker, stock.getSymbol());
    }

    @Test
    public void getMultipleStockQuotes(){
        try {
            stockMap = YahooFinance.get(stockTickers);
        }
        catch (IOException ioe){
            //TODO handle here
            ioe.printStackTrace();
        }

        for(String tickerInArray : stockTickers){
            Assert.assertTrue("no results for: " + tickerInArray, stockMap.containsKey(tickerInArray));
            Assert.assertNotNull(tickerInArray + ": is null!", stockMap.get(tickerInArray));
        }

    }


}

