package com.threerings.s3.client;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class RFC822Format extends ThreadLocal<DateFormat>
{
    public static String format (Date date)
    {
        return INSTANCE.get().format(date);
    }

    public static Date parse (String date)
        throws ParseException
    {
        return INSTANCE.get().parse(date);
    }

    private RFC822Format () {}

    @Override
    protected DateFormat initialValue ()
    {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format;
    }

    private static final RFC822Format INSTANCE = new RFC822Format();
}
