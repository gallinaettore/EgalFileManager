package it.Ettore.egalfilemanager.copyutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;


/**
 * Classe per la gestione della sovrascrittura
 * @param <T> Tipo di file (File, SmbFile...)
 */
public class SovrascritturaFiles<T> {
    public static final int AZIONE_IGNORA = 0;
    public static final int AZIONE_SOVRASCRIVI = 1;
    public static final int AZIONE_RINOMINA = 2;
    private final WeakReference<Activity> activity;
    private int indiceFileSovrascrittura = 0;
    private final List<T> filesGiaPresenti;
    private final List<String> nomiFiles;
    private final SovrascritturaListener<T> listener;
    private final Map<T, Integer> azioniFilesGiaPresenti;


    /**
     *
     * @param activity Activity chiamante
     * @param filesGiaPresenti Files già presenti nella directory di destinazione
     * @param nomiFiles Nomi dei files già presenti nella directory di destinazione
     * @param listener Listener chiamato al termine delle operazione. Dopo che l'utente tramite le dialogs ha scelto quale file sovrascrivere, quale ignorare...
     */
    public SovrascritturaFiles(@NonNull Activity activity, @NonNull List<T> filesGiaPresenti, @NonNull List<String> nomiFiles, @NonNull SovrascritturaListener<T> listener){
        this.activity = new WeakReference<>(activity);
        this.filesGiaPresenti = filesGiaPresenti;
        this.nomiFiles = nomiFiles;
        this.listener = listener;
        this.azioniFilesGiaPresenti = new HashMap<>();
    }


    /**
     * Mostra la dialog che chiede come comportarsi se il file di destinazione è già esistente
     */
    public void mostraDialogSovrascrittura(){
        if(activity.get() != null && !activity.get().isFinishing()) {
            final T file;
            final String nomeFile;
            if (indiceFileSovrascrittura < filesGiaPresenti.size()) {
                //ci sono ancora files già presenti da notificare
                file = filesGiaPresenti.get(indiceFileSovrascrittura);
                nomeFile = nomiFiles.get(indiceFileSovrascrittura);
            } else {
                //non ci sono più files da notificare, avvio la copia
                listener.onDialogOverwriteFinished(azioniFilesGiaPresenti);
                return;
            }
            final CustomDialogBuilder builder = new CustomDialogBuilder(activity.get());
            builder.setType(CustomDialogBuilder.TYPE_WARNING);
            builder.setCancelable(false);
            final View view = LayoutInflater.from(activity.get()).inflate(R.layout.dialog_sovrascrivi_file, null);
            final TextView messageTextView = view.findViewById(R.id.textview_messaggio);
            final CheckBox checkBoxApplicaATutti = view.findViewById(R.id.checkbox_tutti);
            if(filesGiaPresenti.size() == 1){
                checkBoxApplicaATutti.setVisibility(View.GONE);
            }
            builder.setView(view);
            messageTextView.setText(String.format(activity.get().getString(R.string.file_esistente), nomeFile));
            builder.setPositiveButton(R.string.sovrascrivi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!checkBoxApplicaATutti.isChecked()) {
                        //applica solo al file selezionato
                        azioniFilesGiaPresenti.put(file, AZIONE_SOVRASCRIVI);
                        indiceFileSovrascrittura++;
                        mostraDialogSovrascrittura();
                    } else {
                        applicaATuttiRestanti(AZIONE_SOVRASCRIVI);
                    }
                }
            });
            builder.setNegativeButton(R.string.rinomina, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!checkBoxApplicaATutti.isChecked()) {
                        //applica solo al file selezionato
                        azioniFilesGiaPresenti.put(file, AZIONE_RINOMINA);
                        indiceFileSovrascrittura++;
                        mostraDialogSovrascrittura();
                    } else {
                        applicaATuttiRestanti(AZIONE_RINOMINA);
                    }
                }
            });
            builder.setNeutralButton(R.string.ignora, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(!checkBoxApplicaATutti.isChecked()) {
                        //applica solo al file selezionato
                        azioniFilesGiaPresenti.put(file, AZIONE_IGNORA);
                        indiceFileSovrascrittura++;
                        mostraDialogSovrascrittura();
                    } else {
                        applicaATuttiRestanti(AZIONE_IGNORA);
                    }
                }
            });
            builder.create().show();
        }
    }


    /**
     * Setta la stessa azione per tutti i files restanti
     * @param azione Azione espressa dalla costanti AZIONE di questa classe
     */
    public void applicaATuttiRestanti(int azione){
        while (indiceFileSovrascrittura < filesGiaPresenti.size()){
            final T file = filesGiaPresenti.get(indiceFileSovrascrittura);
            azioniFilesGiaPresenti.put(file, azione);
            indiceFileSovrascrittura++;
        }
        //al termine avvio la copia
        listener.onDialogOverwriteFinished(azioniFilesGiaPresenti);
    }




    /**
     * Listener di sovrascrittura
     * @param <T> Tipo di file (File, SmbFile...)
     */
    public interface SovrascritturaListener<T> {

        /**
         * Listener chiamato al termine delle operazione. Dopo che l'utente tramite le dialogs ha scelto quale file sovrascrivere, quale ignorare...
         * @param azioniFilesGiaPresenti Map che contiene l'associazione file-azione da eseguire
         */
        void onDialogOverwriteFinished(Map<T, Integer> azioniFilesGiaPresenti);
    }
}
