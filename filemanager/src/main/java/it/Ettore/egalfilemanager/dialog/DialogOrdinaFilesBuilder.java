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
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase;


/**
 * Builder per la Dialog che gestisce l'ordinamento degli albums
 */
public class DialogOrdinaFilesBuilder {
    private final Context context;
    private final OrdinatoreFilesBase ordinatoreFiles;
    private final DialogInterface.OnClickListener listener;


    /**
     *
     * @param context Context
     * @param ordinatoreFiles Oggetto che gestisce l'ordinamento dei files
     * @param listener Chiamato alla selezione della dialog
     */
    public DialogOrdinaFilesBuilder(@NonNull Context context, @NonNull OrdinatoreFilesBase ordinatoreFiles, DialogInterface.OnClickListener listener){
        this.context = context;
        this.ordinatoreFiles = ordinatoreFiles;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return AlertDialog
     */
    public AlertDialog create(){
        final AlertDialog.Builder dialogOrdinaBuilder = new AlertDialog.Builder(context);
        final View ordinaView = LayoutInflater.from(context).inflate(R.layout.dialog_ordina_files, null);
        final RadioButton radioOrdinaNome = ordinaView.findViewById(R.id.radio_ordina_nome);
        final RadioButton radioOrdinaDimensione = ordinaView.findViewById(R.id.radio_ordina_dimesione);
        final RadioButton radioOrdinaData = ordinaView.findViewById(R.id.radio_ordina_data);
        final RadioButton radioOrdinaTipo = ordinaView.findViewById(R.id.radio_ordina_tipo);
        final RadioButton radioOrdinaCrescente = ordinaView.findViewById(R.id.radio_ordina_crescente);
        final RadioButton radioOrdinaDecrescente = ordinaView.findViewById(R.id.radio_ordina_decrescente);
        if(ordinatoreFiles.getOrdinaPer() == OrdinatoreFiles.OrdinaPer.NOME){
            radioOrdinaNome.setChecked(true);
        } else if (ordinatoreFiles.getOrdinaPer() == OrdinatoreFiles.OrdinaPer.DIMENSIONE){
            radioOrdinaDimensione.setChecked(true);
        } else if (ordinatoreFiles.getOrdinaPer() == OrdinatoreFiles.OrdinaPer.DATA){
            radioOrdinaData.setChecked(true);
        } else if (ordinatoreFiles.getOrdinaPer() == OrdinatoreFiles.OrdinaPer.TIPO){
            radioOrdinaTipo.setChecked(true);
        }
        if(ordinatoreFiles.getTipoOrdinamento() == OrdinatoreFiles.TipoOrdinamento.CRESCENTE){
            radioOrdinaCrescente.setChecked(true);
        } else {
            radioOrdinaDecrescente.setChecked(true);
        }
        dialogOrdinaBuilder.setView(ordinaView);
        dialogOrdinaBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(radioOrdinaNome.isChecked()){
                    ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.NOME);
                } else if(radioOrdinaDimensione.isChecked()){
                    ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.DIMENSIONE);
                } else if (radioOrdinaData.isChecked()){
                    ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.DATA);
                } else if(radioOrdinaTipo.isChecked()){
                    ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.TIPO);
                }
                if(radioOrdinaCrescente.isChecked()){
                    ordinatoreFiles.setTipoOrdinamento(OrdinatoreFiles.TipoOrdinamento.CRESCENTE);
                } else {
                    ordinatoreFiles.setTipoOrdinamento(OrdinatoreFiles.TipoOrdinamento.DESCRESCENTE);
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
