package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.androidutilsx.utils.ViewUtils;
import it.Ettore.egalfilemanager.R;

import static it.Ettore.egalfilemanager.Costanti.GIORNI_RECENTI_DEFAULT;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_GIORNI_RECENTI;


/**
 * Builder per la Dialog che gestisce il numero di giorni da utilizzare per i files recenti
 */
public class DialogGiorniFilesRecentiBuilder {
    private final Context context;
    private final SharedPreferences prefs;
    private final DialogGiorniFilesRecentiListener listener;


    /**
     *
     * @param context Context
     * @param listener Listener chiamato quando vengono modificati i giorni
     */
    public DialogGiorniFilesRecentiBuilder(@NonNull Context context, DialogGiorniFilesRecentiListener listener){
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.listener = listener;
    }


    /**
     * Crea la dialog. Se il numero impostato nella dialog è corretto, alla pressione del tasto OK viene salvato il valore nelle preferences
     * @return AlertDialog
     */
    public AlertDialog create(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.setTitle(R.string.giorni);
        builder.hideIcon(true);

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_ultimi_giorni, null);
        final EditText editTextGiorni = view.findViewById(R.id.edittext_giorni);
        int giorni = prefs.getInt(KEY_PREF_GIORNI_RECENTI, GIORNI_RECENTI_DEFAULT);
        editTextGiorni.setText(String.valueOf(giorni));
        ViewUtils.cursoreAllaFine(editTextGiorni);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int giorni = 0;
                try {
                    giorni = Integer.parseInt(editTextGiorni.getText().toString());
                } catch (NumberFormatException ignored){}
                if(giorni > 0){
                    prefs.edit().putInt(KEY_PREF_GIORNI_RECENTI, giorni).apply();
                    if(listener != null){
                        listener.onDaysChanged(giorni);
                    }
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }



    /**
     * Listener della dialog
     */
    public interface DialogGiorniFilesRecentiListener {

        /**
         * Chiamato quando vengono modificati i giorni
         * @param newDays Nuovo valore giorni
         */
        void onDaysChanged(int newDays);
    }
}
