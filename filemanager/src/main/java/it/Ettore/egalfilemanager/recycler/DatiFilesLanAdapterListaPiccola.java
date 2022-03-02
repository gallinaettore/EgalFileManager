package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.SmbFile;


/**
 * Adapter per la visualizzazione dei dati in formato lista piccola
 */
public final class DatiFilesLanAdapterListaPiccola extends DatiFilesLanBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLanAdapterListaPiccola(@NonNull Context context, OnItemTouchListener listener) {
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup
     * @param viewType
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesLanViewHolderListaPiccola onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_piccola_files, viewGroup, false);
        return new DatiFilesLanViewHolderListaPiccola(layout);
    }






    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLanViewHolderListaPiccola extends DatiFilesLanBaseViewHolder {
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView;

        @SuppressLint("RtlHardcoded")
        private DatiFilesLanViewHolderListaPiccola(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);

            if(LayoutDirectionHelper.isRightToLeft(itemView.getContext())){
                nomeFileTextView.setGravity(Gravity.RIGHT);
            }
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
            manageFileVisibility(DatiFilesLanAdapterListaPiccola.this, file, iconaImageView, nomeFileTextView);
            manageTouch(file, DatiFilesLanAdapterListaPiccola.this);
        }
    }

}
