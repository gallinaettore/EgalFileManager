package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;


/**
 * Builder per la Dialog che gestisce la visualizzazione dei files audio (Cartella o album)
 */
public class DialogDisponiFilesAudioBuilder {
    public static final int DISPOSIZIONE_NOME_CARTELLA = 0;
    public static final int DISPOSIZIONE_NOME_ALBUM = 1;
    private final Context context;
    private final DisponiFilesAudioListener listener;
    private final int disposizioneIniziale;


    /**
     *
     * @param context Context
     * @param disposizione Stato della disposizione alla visualizzazione della dialog. Una delle costanti DISPOSIZIONE di questa classe.
     * @param listener Chiamato alla selezione della dialog
     */
    public DialogDisponiFilesAudioBuilder(@NonNull Context context, int disposizione, DisponiFilesAudioListener listener){
        this.context = context;
        this.disposizioneIniziale = disposizione;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return AlertDialog
     */
    public AlertDialog create(){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        final View disponiView = LayoutInflater.from(context).inflate(R.layout.dialog_organizza_per, null);
        final RadioButton radioCartella = disponiView.findViewById(R.id.radio_cartella);
        final RadioButton radioAlbum = disponiView.findViewById(R.id.radio_album);
        switch (disposizioneIniziale){
            case DISPOSIZIONE_NOME_CARTELLA:
                radioCartella.setChecked(true);
                break;
            case DISPOSIZIONE_NOME_ALBUM:
                radioAlbum.setChecked(true);
                break;
            default:
                throw new IllegalArgumentException("Tipo disposizione non gestita: " + disposizioneIniziale);
        }
        dialogBuilder.setView(disponiView);
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int nuovaDisposizione = 0;
                if(radioCartella.isChecked()){
                    nuovaDisposizione = DISPOSIZIONE_NOME_CARTELLA;
                } else if (radioAlbum.isChecked()){
                    nuovaDisposizione = DISPOSIZIONE_NOME_ALBUM;
                }
                if(nuovaDisposizione != disposizioneIniziale && listener != null){
                    listener.onArrangementChanged(nuovaDisposizione);
                }
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);
        return dialogBuilder.create();
    }



    /**
     * Listener per la gestione della nuova disposizione
     */
    public interface DisponiFilesAudioListener {

        /**
         * Chiamato quando si seleziona una nuova disposizione dalla dialog. Il metodo viene chiamato solo se la disposizione è cambiata.
         * @param nuovaDisposizione Nuova disposizione. Una delle costanti di questa classe.
         */
        void onArrangementChanged(int nuovaDisposizione);
    }
}
