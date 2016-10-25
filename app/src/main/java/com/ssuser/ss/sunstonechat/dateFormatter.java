package com.ssuser.ss.sunstonechat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.SimpleFormatter;

/**
 * Created by Eric on 4/3/2015.
 */
public class dateFormatter {

    private final static String dateFormat = "E, MMM dd, h:mm aa";

    public static String getDate(long milliseconds){

        SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        return simpleDate.format(calendar.getTime());

    }

    public static String getDate(String milliseconds){

        SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.valueOf(milliseconds));
        return simpleDate.format(calendar.getTime());
    }

}
