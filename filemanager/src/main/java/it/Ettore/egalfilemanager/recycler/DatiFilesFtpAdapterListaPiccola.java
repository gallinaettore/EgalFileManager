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
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato lista piccola
 */
public final class DatiFilesFtpAdapterListaPiccola extends DatiFilesFtpBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesFtpAdapterListaPiccola(@NonNull Context context, OnItemTouchListener listener) {
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup
     * @param viewType
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesFtpViewHolderListaPiccola onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_piccola_files, viewGroup, false);
        return new DatiFilesFtpViewHolderListaPiccola(layout);
    }






    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesFtpViewHolderListaPiccola extends DatiFilesFtpBaseViewHolder {
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView;

        @SuppressLint("RtlHardcoded")
        private DatiFilesFtpViewHolderListaPiccola(View itemView){
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

            manageView(file, DatiFilesFtpAdapterListaPiccola.this);
        }
    }

}
