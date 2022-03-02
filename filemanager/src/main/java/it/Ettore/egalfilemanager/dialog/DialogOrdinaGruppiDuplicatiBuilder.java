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
import it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati;
import it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati.OrdinaPer;
import it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati.TipoOrdinamento;


/**
 * Builder per la Dialog che gestisce l'ordinamento dei gruppi di files duplicati
 */
public class DialogOrdinaGruppiDuplicatiBuilder {
    private final Context context;
    private final OrdinatoreGruppiFilesDuplicati ordinatore;
    private final DialogInterface.OnClickListener listener;


    /**
     *
     * @param context Context
     * @param ordinatore Oggetto che gestisce l'ordinamento dei gruppi
     * @param listener Chiamato alla selezione della dialog
     */
    public DialogOrdinaGruppiDuplicatiBuilder(@NonNull Context context, @NonNull OrdinatoreGruppiFilesDuplicati ordinatore, DialogInterface.OnClickListener listener){
        this.context = context;
        this.ordinatore = ordinatore;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return AlertDialog
     */
    public AlertDialog create(){
        final AlertDialog.Builder dialogOrdinaBuilder = new AlertDialog.Builder(context);
        final View ordinaView = LayoutInflater.from(context).inflate(R.layout.dialog_ordina_gruppi_duplicati, null);
        final RadioButton radioOrdinaNome = ordinaView.findViewById(R.id.radio_ordina_nome);
        final RadioButton radioOrdinaElementi = ordinaView.findViewById(R.id.radio_ordina_elementi);
        final RadioButton radioOrdinaDimensione = ordinaView.findViewById(R.id.radio_ordina_dimensione);
        final RadioButton radioOrdinaCrescente = ordinaView.findViewById(R.id.radio_ordina_crescente);
        final RadioButton radioOrdinaDecrescente = ordinaView.findViewById(R.id.radio_ordina_decrescente);
        if(ordinatore.getOrdinaPer() == OrdinaPer.NOME){
            radioOrdinaNome.setChecked(true);
        } else if (ordinatore.getOrdinaPer() == OrdinaPer.ELEMENTI){
            radioOrdinaElementi.setChecked(true);
        } else if (ordinatore.getOrdinaPer() == OrdinaPer.DIMENSIONE){
            radioOrdinaDimensione.setChecked(true);
        }
        if(ordinatore.getTipoOrdinamento() == TipoOrdinamento.CRESCENTE){
            radioOrdinaCrescente.setChecked(true);
        } else {
            radioOrdinaDecrescente.setChecked(true);
        }
        dialogOrdinaBuilder.setView(ordinaView);
        dialogOrdinaBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(radioOrdinaNome.isChecked()){
                    ordinatore.setOrdinaPer(OrdinaPer.NOME);
                } else if(radioOrdinaElementi.isChecked()){
                    ordinatore.setOrdinaPer(OrdinaPer.ELEMENTI);
                } else if (radioOrdinaDecrescente.isChecked()){
                    ordinatore.setOrdinaPer(OrdinaPer.DIMENSIONE);
                }
                if(radioOrdinaCrescente.isChecked()){
                    ordinatore.setTipoOrdinamento(TipoOrdinamento.CRESCENTE);
                } else {
                    ordinatore.setTipoOrdinamento(TipoOrdinamento.DESCRESCENTE);
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
