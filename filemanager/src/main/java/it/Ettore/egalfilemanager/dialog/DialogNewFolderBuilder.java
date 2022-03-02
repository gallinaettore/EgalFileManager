package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;


/**
 * Builder per la Dialog di inserimento del nome del file
 */
public class DialogNewFolderBuilder {
    private final Context context;
    private final DialogNewFolderListener listener;


    public DialogNewFolderBuilder(@NonNull Context context, DialogNewFolderListener listener){
        this.context = context;
        this.listener = listener;
    }


    public AlertDialog create(){
        final CustomDialogBuilder builderNuovaCartella = new CustomDialogBuilder(context);
        builderNuovaCartella.hideIcon(true);
        builderNuovaCartella.setTitle(R.string.nuova_cartella);
        final View viewNuovaCartella = LayoutInflater.from(context).inflate(R.layout.dialog_nuova_cartella, null);
        final EditText nomeCartellaEditText = viewNuovaCartella.findViewById(R.id.editText);
        nomeCartellaEditText.requestFocus();
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); //mostro la tastiera
        builderNuovaCartella.setView(viewNuovaCartella);
        builderNuovaCartella.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(nomeCartellaEditText.getWindowToken(), 0); //nascondo la tastiera
                if(listener != null){
                    listener.onNewFolderInput(nomeCartellaEditText.getText().toString());
                }
            }
        });
        builderNuovaCartella.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(nomeCartellaEditText.getWindowToken(), 0); //nascondo la tastiera
            }
        });
        return builderNuovaCartella.create();
    }



    /**
     * Interfaccia per la gestione dell'inserimento della nuova cartella
     */
    public interface DialogNewFolderListener {

        /**
         * Chiamato all'inserimento del nome della cartella da creare
         * @param name Nome cartella
         */
        void onNewFolderInput(String name);
    }
}
