package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.R;


/**
 * Builder per la creazione di AlertDialog con titlebar grande e colorata
 */
public class CustomDialogBuilder extends AlertDialog.Builder {
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_WARNING = 1;
    public static final int TYPE_ERROR = 2;
    private final View titleView;
    private final TextView titleTextView;
    private final ImageView titleImageView;
    private final View spaceView;
    private int type = TYPE_NORMAL;
    private String title;
    private boolean hideIcon, removeTitleSpace;
    //private Drawable icon;


    public CustomDialogBuilder(@NonNull Context context) {
        super(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.view_titolo_dialog, null);
        titleView = view.findViewById(R.id.view_tile);
        titleImageView = view.findViewById(R.id.imageview_title);
        titleTextView = view.findViewById(R.id.textview_titolo);
        spaceView = view.findViewById(R.id.space_view);
        setCustomTitle(view);
    }



    /**
     * Setta il tipo di dialog utilizzando le variabili TYPE di questa classe
     * @param type Variabile TYPE di questa classe
     * @return Builder
     */
    public AlertDialog.Builder setType(int type){
        this.type = type;
        return this;
    }



    /**
     * Setta il titolo della dialog
     * @param titleId Risorsa del titolo
     * @return Builder
     */
    @Override
    public AlertDialog.Builder setTitle(@StringRes int titleId) {
        this.title = getContext().getString(titleId);
        return this;
    }



    /**
     * Setta il titolo della dialog
     * @param title Titolo
     * @return Builder
     */
    @Override
    public AlertDialog.Builder setTitle(@Nullable CharSequence title) {
        this.title = title.toString();
        return this;
    }



    /**
     * Permette di nascondere l'icona
     * @param hideIcon nascondi l'icona
     * @return Builder
     */
    public AlertDialog.Builder hideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
        return this;
    }



    /**
     * Rimuove lo spazio bianco sotto la barra del titolo
     * @param remove Rimuovi
     * @return Builder
     */
    public AlertDialog.Builder removeTitleSpace(boolean remove){
        this.removeTitleSpace = remove;
        return this;
    }



    /**
     * Crea la dialog con i parametri impostati
     * @return AlertDialog customizzata
     */
    @Override
    public AlertDialog create(){
        switch (type){
            case TYPE_WARNING:
                titleView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.dialog_background_title_view_warning));
                titleImageView.setImageResource(R.drawable.ic_warning_white_48dp);
                break;
            case TYPE_ERROR:
                titleView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.dialog_background_title_view_error));
                titleImageView.setImageResource(R.drawable.ic_report_white_48dp);
                break;
            default:
                titleView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                titleImageView.setImageResource(R.drawable.ic_info_white_48dp);
        }
        if(title != null){
            titleTextView.setVisibility(View.VISIBLE);
            titleTextView.setText(title);
        } else {
            titleTextView.setVisibility(View.GONE);
        }
        if(hideIcon){
            titleImageView.setVisibility(View.GONE);
        } else {
            titleImageView.setVisibility(View.VISIBLE);
        }
        if(removeTitleSpace){
            spaceView.setVisibility(View.GONE);
        } else {
            spaceView.setVisibility(View.VISIBLE);
        }
        return super.create();
    }



    /**
     * Crea istantaneamente una Dialog
     * @param context Context
     * @param message Messaggio da mostrare
     * @param type Tipo di dialog (Variabile TYPE di questa classe)
     * @return AlertDialog customizzata
     */
    public static AlertDialog make(@NonNull Context context, String message, int type){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.setMessage(message);
        builder.setType(type);
        builder.setNeutralButton(android.R.string.ok, null);
        return builder.create();
    }



    /**
     * Crea istantaneamente una Dialog
     * @param context Context
     * @param resIdMessage Risorsa del messaggio da mostrare
     * @param type Tipo di dialog (Variabile TYPE di questa classe)
     * @return AlertDialog customizzata
     */
    public static AlertDialog make(@NonNull Context context, @StringRes int resIdMessage, int type){
        return make(context, context.getString(resIdMessage), type);
    }



    /**
     * Crea istantaneamente una Dialog di tipo NORMAL
     * @param context Context
     * @param message Messaggio da mostrare
     * @return AlertDialog customizzata
     */
    public static AlertDialog make(@NonNull Context context, String message){
        return make(context, message, TYPE_NORMAL);
    }



    /**
     * Crea istantaneamente una Dialog di tipo NORMAL
     * @param context Context
     * @param resIdMessage Risorsa del messaggio da mostrare
     * @return AlertDialog customizzata
     */
    public static AlertDialog make(@NonNull Context context, @StringRes int resIdMessage){
        return make(context, context.getString(resIdMessage), TYPE_NORMAL);
    }
}
