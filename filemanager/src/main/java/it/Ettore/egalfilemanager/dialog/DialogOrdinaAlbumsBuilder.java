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
import it.Ettore.egalfilemanager.mediastore.OrdinatoreAlbums;


/**
 * Builder per la Dialog che gestisce l'ordinamento degli albums
 */
public class DialogOrdinaAlbumsBuilder {
    private final Context context;
    private final OrdinatoreAlbums ordinatoreAlbums;
    private final boolean ordinamentoPercorsoConsentito;
    private final DialogInterface.OnClickListener listener;


    /**
     *
     * @param context Context
     * @param ordinatoreAlbums Oggetto che gestisce l'ordinamento degli albums
     * @param ordinamentoPercorsoConsentito True se è consentito l'ordinamento per percorso.
     *                                      L'ordinamento per percorso non deve essere consentito per gli album che all'interno contengono file presenti in percorsi diversi come audio o altro...
     * @param listener Chiamato alla selezione della dialog
     */
    public DialogOrdinaAlbumsBuilder(@NonNull Context context, @NonNull OrdinatoreAlbums ordinatoreAlbums, boolean ordinamentoPercorsoConsentito, DialogInterface.OnClickListener listener){
        this.context = context;
        this.ordinatoreAlbums = ordinatoreAlbums;
        this.ordinamentoPercorsoConsentito = ordinamentoPercorsoConsentito;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return AlertDialog
     */
    public AlertDialog create(){
        final AlertDialog.Builder dialogOrdinaBuilder = new AlertDialog.Builder(context);
        final View ordinaView = LayoutInflater.from(context).inflate(R.layout.dialog_ordina_albums, null);
        final RadioButton radioOrdinaNome = ordinaView.findViewById(R.id.radio_ordina_nome);
        final RadioButton radioOrdinaElementi = ordinaView.findViewById(R.id.radio_ordina_elementi);
        final RadioButton radioOrdinaPercorso = ordinaView.findViewById(R.id.radio_ordina_percorso);
        final RadioButton radioOrdinaCrescente = ordinaView.findViewById(R.id.radio_ordina_crescente);
        final RadioButton radioOrdinaDecrescente = ordinaView.findViewById(R.id.radio_ordina_decrescente);
        if(!ordinamentoPercorsoConsentito){
            radioOrdinaPercorso.setVisibility(View.GONE);
        }
        if(ordinatoreAlbums.getOrdinaPer() == OrdinatoreAlbums.OrdinaPer.NOME){
            radioOrdinaNome.setChecked(true);
        } else if (ordinatoreAlbums.getOrdinaPer() == OrdinatoreAlbums.OrdinaPer.ELEMENTI){
            radioOrdinaElementi.setChecked(true);
        } else if (ordinatoreAlbums.getOrdinaPer() == OrdinatoreAlbums.OrdinaPer.PERCORSO){
            if(ordinamentoPercorsoConsentito) {
                radioOrdinaPercorso.setChecked(true);
            } else {
                //se non è permesso ordinare per percorso, seleziono l'ordinamento standard per nome
                radioOrdinaNome.setChecked(true);
            }
        }
        if(ordinatoreAlbums.getTipoOrdinamento() == OrdinatoreAlbums.TipoOrdinamento.CRESCENTE){
            radioOrdinaCrescente.setChecked(true);
        } else {
            radioOrdinaDecrescente.setChecked(true);
        }
        dialogOrdinaBuilder.setView(ordinaView);
        dialogOrdinaBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(radioOrdinaNome.isChecked()){
                    ordinatoreAlbums.setOrdinaPer(OrdinatoreAlbums.OrdinaPer.NOME);
                } else if(radioOrdinaElementi.isChecked()){
                    ordinatoreAlbums.setOrdinaPer(OrdinatoreAlbums.OrdinaPer.ELEMENTI);
                } else if (radioOrdinaPercorso.isChecked()){
                    ordinatoreAlbums.setOrdinaPer(OrdinatoreAlbums.OrdinaPer.PERCORSO);
                }
                if(radioOrdinaCrescente.isChecked()){
                    ordinatoreAlbums.setTipoOrdinamento(OrdinatoreAlbums.TipoOrdinamento.CRESCENTE);
                } else {
                    ordinatoreAlbums.setTipoOrdinamento(OrdinatoreAlbums.TipoOrdinamento.DESCRESCENTE);
                }
                if(listener != null){
                    listener.onClick(dialogInterface, i);
                }
            }
        });
        dialogOrdinaBuilder.setNegativeButton(android.R.string.cancel, null);
        return dialogOrdinaBuilder.create();
    }

}
