package it.Ettore.egalfilemanager.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.egalfilemanager.R;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * ProgressDialog con la titlebar grande e colorata
 */
public class CustomProgressDialog extends ColoredProgressDialog {
    private TextView titleTextView;


    public CustomProgressDialog(@NonNull Context context) {
        super(context);
        init();
    }


    public CustomProgressDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }


    /**
     * Inizializza la view della dialog
     */
    private void init(){
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.view_titolo_dialog, null);
        final ImageView titleImageView = view.findViewById(R.id.imageview_title);
        titleImageView.setVisibility(View.GONE);
        titleTextView = view.findViewById(R.id.textview_titolo);
        setCustomTitle(view);
    }


    @Override
    public void setTitle(CharSequence title) {
        titleTextView.setText(title);
    }


    @Override
    public void setTitle(@StringRes int titleId) {
        titleTextView.setText(titleId);
    }


    /**
     * Aggiunge alla dialog un eventuale bottone per l'annullamento
     * @param onClickListener Listener da eseguire alla pressione del bottone
     */
    public void addCancelButton(OnClickListener onClickListener){
        setButton(AlertDialog.BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), onClickListener);
        //imposto manualmente il colore del button altrimenti si vede bianco
        setOnShowListener(arg0 -> getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent)));
    }
}
