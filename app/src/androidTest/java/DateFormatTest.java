import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by devbox on 1/14/17.
 */

@RunWith(JUnit4.class)
public class DateFormatTest {

    String testValueString = "1483948800000";
    long dateStampInit = (long) 1483430400000L;
    String format = "dd MMM yy";

    @Test
    public void DateFormatTest(){

        long dateStamp = Long.parseLong(testValueString);

        Log.v("_dateString: ", testValueString);
        Log.v("_dateLong: ", String.valueOf(dateStamp));

        DateFormat dateFormat = DateFormat.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

        String dateFormatted = dateFormat.format(new Date(dateStampInit));
        String simpleDataFormatted = simpleDateFormat.format(new Date(dateStampInit));

        Log.v("_dateFormatted: ", dateFormatted);
        Log.v("_dateSimpleFormatted: ", simpleDataFormatted);








    }

    }



