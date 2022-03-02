package it.Ettore.androidutilsx.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

public class ColoredToast {

    protected ColoredToast(){}


    public static Toast makeText(Context context, @DrawableRes int resIdIcon, String text, int duration){
        try {
            ContextCompat.getColor(context, R.color.colored_toast_background); //verifico se il colore è stato impostato, altrimenti lancia un'eccezione

            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View layout = inflater.inflate(R.layout.colored_toast,(ViewGroup) ((Activity)context).findViewById(R.id.colored_toast_container));

            final TextView textView = (TextView) layout.findViewById(R.id.textView);
            textView.setText(text);

            final ImageView imageView = (ImageView) layout.findViewById(R.id.imageView);
            if(resIdIcon == 0) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setImageResource(resIdIcon);
            }

            final Toast toast = new Toast(context);
            toast.setGravity(Gravity.BOTTOM, 0, 70);
            toast.setDuration(duration);
            toast.setView(layout);
            return toast;
        } catch (Resources.NotFoundException e){
            //Se non viene sovrascritto nel file color.xml viene lanciata un'eccezione e quindi il tost non viene modificato
            //Visualizzando il toast predefinito di Android
            return Toast.makeText(context, text, duration);
        } catch (Exception e2){
            return Toast.makeText(context, text, duration);
        }
    }


    public static Toast makeText(Context context, @DrawableRes int resIdIcon, @StringRes int resIdText, int duration){
        return makeText(context, resIdIcon, context.getString(resIdText), duration);
    }


    public static Toast makeText(Context context, String text, int duration){
        return makeText(context, 0, text, duration);
    }


    public static Toast makeText(Context context, @StringRes int resIdText, int duration){
        return makeText(context, context.getString(resIdText), duration);
    }
}
