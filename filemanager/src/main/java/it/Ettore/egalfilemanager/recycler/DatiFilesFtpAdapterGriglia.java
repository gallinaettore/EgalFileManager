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
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato griglia
 */
public class DatiFilesFtpAdapterGriglia extends DatiFilesFtpBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesFtpAdapterGriglia(@NonNull Context context, OnItemTouchListener listener){
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
    public DatiFilesFtpViewHolderGriglia onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_griglia_files, viewGroup, false);
        return new DatiFilesFtpViewHolderGriglia(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesFtpViewHolderGriglia extends DatiFilesFtpBaseViewHolder {
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView;

        private DatiFilesFtpViewHolderGriglia(View itemView){
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
            final FtpElement file = getListaFiles().get(position);
            final String nome = file.getName();
            if(file.isDirectory()){
                //cartella
                nomeFileTextView.setText(nome);
                iconaImageView.setImageResource(R.drawable.ico_cartella);
            } else {
                //files
                nomeFileTextView.setText(nome);
                iconaImageView.setImageResource(IconManager.iconForFile(nome));
            }

            if(file.isHidden()){
                nomeFileTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
            } else {
                nomeFileTextView.setTextColor(getThemeUtils().getPrimaryTextColor());
            }

            manageView(file, DatiFilesFtpAdapterGriglia.this);
        }
    }
}
