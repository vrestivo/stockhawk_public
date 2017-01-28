package com.udacity.stockhawk.data;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by devbox on 1/13/17.
 */

public class ChartXValueFormatter implements IAxisValueFormatter {

    String format = "dd MMM yy";

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(new Date((long)value));
    }
}
