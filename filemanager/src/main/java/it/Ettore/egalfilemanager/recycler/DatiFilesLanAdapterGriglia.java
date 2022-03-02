package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.SmbFile;


/**
 * Adapter per la visualizzazione dei dati in formato griglia
 */
public class DatiFilesLanAdapterGriglia extends DatiFilesLanBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLanAdapterGriglia(@NonNull Context context, OnItemTouchListener listener){
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup
     * @param viewType
     * @return ViewHolder creato
     */
    @NonNull
    @Override
    public DatiFilesLanViewHolderGriglia onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_griglia_files, viewGroup, false);
        return new DatiFilesLanViewHolderGriglia(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLanViewHolderGriglia extends DatiFilesLanBaseViewHolder {
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView;

        private DatiFilesLanViewHolderGriglia(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);
        }


        /**
         * Effettua il bind dei dati
         * @param position posizione nell'adapter
         */
        @Override
        void bind(final int position){
            final SmbFile file = getListaFiles().get(position);
            final String nome = file.getName();
            if(SmbFileUtils.isDirectory(file)){
                //cartella
                nomeFileTextView.setText(nome.replace("/", ""));
                iconaImageView.setImageResource(R.drawable.ico_cartella);
            } else {
                //files
                nomeFileTextView.setText(nome);
                iconaImageView.setImageResource(IconManager.iconForFile(nome));
            }
            manageFileVisibility(DatiFilesLanAdapterGriglia.this, file, iconaImageView, nomeFileTextView);
            manageTouch(file, DatiFilesLanAdapterGriglia.this);
        }
    }
}
