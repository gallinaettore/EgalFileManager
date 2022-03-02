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
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;


/**
 * Builder per la Dialog di inserimento del nome del file
 */
public class DialogNewNameBuilder {
    private final Context context;
    private final String nomePrimoFile;
    private final DialogRinominaListener listener;


    /**
     *
     * @param context Context chiamante
     * @param nomePrimoFile Nome del file da rinominare, se bisogna rinominare più files passare il nome del primo file della lista
     * @param listener Listener chiamato all'inserimento del nuovo nome
     */
    public DialogNewNameBuilder(@NonNull Context context, @NonNull String nomePrimoFile, DialogRinominaListener listener){
        this.context = context;
        this.nomePrimoFile = nomePrimoFile;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return Alert Dialog
     */
    public AlertDialog create(){
        final CustomDialogBuilder builderRinomina = new CustomDialogBuilder(context);
        builderRinomina.hideIcon(true);
        builderRinomina.setTitle(R.string.nuovo_nome);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_nuova_cartella, null);
        final EditText nomeEditText = view.findViewById(R.id.editText);
        nomeEditText.setText(nomePrimoFile);
        nomeEditText.setSelection(0, FileUtils.getFileNameWithoutExt(nomePrimoFile).length());
        nomeEditText.requestFocus();
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY); //mostro la tastiera
        builderRinomina.setView(view);
        builderRinomina.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(nomeEditText.getWindowToken(), 0); //nascondo la tastiera
                if(listener != null){
                    listener.onNewNameInput(nomeEditText.getText().toString());
                }
            }
        });
        builderRinomina.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                imm.hideSoftInputFromWindow(nomeEditText.getWindowToken(), 0); //nascondo la tastiera
            }
        });
        return builderRinomina.create();
    }


    /**
     * Interfaccia per la gestione dell'inserimento del nuovo nome
     */
    public interface DialogRinominaListener {

        /**
         * Chiamato all'inserimento del nuovo nome
         * @param name Nuovo nome
         */
        void onNewNameInput(String name);
    }
}
