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
public class DialogNewFileBuilder {
    private final Context context;
    private final DialogNewFileListener listener;


    public DialogNewFileBuilder(@NonNull Context context, DialogNewFileListener listener){
        this.context = context;
        this.listener = listener;
    }


    public AlertDialog create(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.hideIcon(true);
        builder.setTitle(R.string.nuovo_file);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_nuova_cartella, null);
        final EditText editText = view.findViewById(R.id.editText);
        editText.requestFocus();
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); //mostro la tastiera
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0); //nascondo la tastiera
                if(listener != null){
                    listener.onNewFileInput(editText.getText().toString());
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0); //nascondo la tastiera
            }
        });
        return builder.create();
    }



    /**
     * Interfaccia per la gestione della creazione del nuovo file
     */
    public interface DialogNewFileListener {

        /**
         * Chiamato all'inserimento del nome del file da creare
         * @param name Nome file
         */
        void onNewFileInput(String name);
    }
}
