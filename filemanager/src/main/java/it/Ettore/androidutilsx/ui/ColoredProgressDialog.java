package it.Ettore.androidutilsx.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

public class ColoredProgressDialog extends ProgressDialog {

    public ColoredProgressDialog(Context context){
        super(context);
        impostaColoreDiSfondo();

        //imposto dei drawable personalizzati per android 4 (perchè non viene utilizzato il material
        //è comunque possibile anche utulizzarli su android successivi sovrascrivendo quelli di default
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_drawable));
            setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indeterminate_drawable_circular));
        }
    }


    public ColoredProgressDialog(Context context, int theme){
        super(context, theme);
        impostaColoreDiSfondo();
    }


    public static ColoredProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable, OnCancelListener cancelListener){
        final ColoredProgressDialog progress = new ColoredProgressDialog(context);
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setIndeterminate(indeterminate);
        progress.setCancelable(cancelable);
        progress.setOnCancelListener(cancelListener);
        progress.show();
        return progress;
    }


    public static ColoredProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable){
        return show(context, title, message, indeterminate, cancelable, null);
    }


    public static ColoredProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate){
        return show(context, title, message, indeterminate, true, null);
    }


    public static ColoredProgressDialog show(Context context, CharSequence title, CharSequence message){
        return show(context, title, message, false, true, null);
    }


    /*
        In questa libreria il colore di sfondo è impostato a @null
        Se non viene sovrascritto nel file color.xml viene lanciata un'eccezione e quindi la ProgressDialog non viene colorata
        Visualizzando la dialog predefinita di Android.
        Per versioni precedenti a Lollipop lo sfondo non viene impostato nella window
        ma viene modificato lo sfondo di ogni singola view quando si mostra la dialog
     */
    private void impostaColoreDiSfondo(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                int color = ContextCompat.getColor(getContext(), R.color.colored_progress_dialog_background);
                getWindow().setBackgroundDrawable(new ColorDrawable(color));
            } catch (Resources.NotFoundException ignored) {}
        }
    }


    @Override
    public void setProgressStyle(int progressStyle){
        super.setProgressStyle(progressStyle);
        //imposto dei drawable personalizzati per android 4 (perchè non viene utilizzato il material
        //è comunque possibile anche utilizzarli su android successivi sovrascrivendo quelli di default
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (progressStyle == ProgressDialog.STYLE_HORIZONTAL) {
                setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indeterminate_drawable_horizontal));
            } else {
                setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indeterminate_drawable_circular));
            }
        }
    }


    public void setMessage(@StringRes int resIdMessage){
        setMessage(getContext().getString(resIdMessage));
    }




    @Override
    public void show(){
        super.show();
        //Imposto i colori
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                int titleTextViewId = getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
                final TextView titleTextView = (TextView) getWindow().getDecorView().findViewById(titleTextViewId);
                final int defaultTextColor = new TextView(getContext()).getTextColors().getDefaultColor();
                titleTextView.setTextColor(defaultTextColor); // cambio il colore del titolo con il colore del testo di default

                int dividerId = getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
                final View divider = findViewById(dividerId);
                divider.setBackgroundColor(Color.TRANSPARENT); // change divider color

                int color = ContextCompat.getColor(getContext(), R.color.colored_progress_dialog_background);

                int topPanelId = getContext().getResources().getIdentifier("android:id/topPanel", null, null);
                final View topPanel = findViewById(topPanelId);
                topPanel.setBackgroundColor(color);

                int contentPanelId = getContext().getResources().getIdentifier("android:id/contentPanel", null, null);
                final View contentPanel = findViewById(contentPanelId);
                contentPanel.setBackgroundColor(color);

                int customPanelId = getContext().getResources().getIdentifier("android:id/customPanel", null, null);
                final View customPanel = findViewById(customPanelId);
                customPanel.setBackgroundColor(color);

                int buttonPanelId = getContext().getResources().getIdentifier("android:id/buttonPanel", null, null);
                final View buttonPanel = findViewById(buttonPanelId);
                buttonPanel.setBackgroundColor(color);

            } catch (Exception ignored) {}
        }
    }
}
