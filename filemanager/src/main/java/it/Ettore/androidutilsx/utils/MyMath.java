package it.Ettore.androidutilsx.utils;


import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * @author Ettore Gallina
 *
 */
public class MyMath {

    /**
     *
     * @param time tempo in millisecondi
     * @return String formattata in esempio 5:35:87
     */
    @NotNull
    public static String formatTimeMilliseconds(long time){
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;

        long elapsedHours = time / hoursInMilli;
        time = time % hoursInMilli;

        long elapsedMinutes = time / minutesInMilli;
        time = time % minutesInMilli;

        long elapsedSeconds = time / secondsInMilli;

        final NumberFormat nf1 = NumberFormat.getInstance(Locale.ENGLISH);
        final NumberFormat nf2 = NumberFormat.getInstance(Locale.ENGLISH);
        nf2.setMinimumIntegerDigits(2);

        if(elapsedHours > 0) {
            return String.format(Locale.ENGLISH, "%s:%s:%s", nf1.format(elapsedHours), nf2.format(elapsedMinutes), nf2.format(elapsedSeconds));
        } else {
            return String.format(Locale.ENGLISH, "%s:%s", nf1.format(elapsedMinutes), nf2.format(elapsedSeconds));
        }
    }


	//Elimina i problemi di notazione scientifica
    public static String doubleToString(double numero){
        return doubleToString(numero, 16);
    }


    public static String doubleToString(double numero, int maxCifreDecimali) {
        return doubleToString(numero, maxCifreDecimali, 0);
    }

    public static String doubleToString(double numero, int maxCifreDecimali, int minCifreDecimali){
        final NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        //Uso sempre 3 cifre per i numeri inferiori a 1
        if(numero < 1 && maxCifreDecimali < 3){
            maxCifreDecimali = 3;
        }
        nf.setMinimumFractionDigits(minCifreDecimali);
        nf.setMaximumFractionDigits(maxCifreDecimali);
        nf.setGroupingUsed(false); //elimino il separatore delle migliaia
        return nf.format(numero);
    }


	public static String dateFileName(String extension){
        final Date now = new Date() ;
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH);
        final String dateString = dateFormat.format(now);
        if(extension == null || extension.trim().isEmpty()){
            return dateString;
        } else {
            return dateString + "." + extension;
        }
    }


    public static float dpToPx(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }


    public static String humanReadableByte(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = ("KMGTPE").charAt(exp-1);
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public static String humanReadableByte(long bytes, @NonNull String[] localizedBytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " " + localizedBytes[0];
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        final String numberFormatted = nf.format(bytes / Math.pow(unit, exp));
        return String.format("%s %s", numberFormatted, localizedBytes[exp]);
    }
}
