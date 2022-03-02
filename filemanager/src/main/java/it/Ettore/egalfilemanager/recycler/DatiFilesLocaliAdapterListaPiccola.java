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

import java.io.File;

import androidx.annotation.NonNull;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato lista piccola
 */
public final class DatiFilesLocaliAdapterListaPiccola extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLocaliAdapterListaPiccola(@NonNull Context context, OnItemTouchListener listener) {
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup .
     * @param viewType .
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesLocaliViewHolderListaPiccola onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_piccola_files, viewGroup, false);
        return new DatiFilesLocaliViewHolderListaPiccola(layout);
    }






    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLocaliViewHolderListaPiccola extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView, collegamentoImageView;

        @SuppressLint("RtlHardcoded")
        private DatiFilesLocaliViewHolderListaPiccola(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);
            collegamentoImageView = itemView.findViewById(R.id.collegamentoImageView);

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
            final File file = getListaFiles().get(position);
            nomeFileTextView.setText(file.getName());
            if(file.isDirectory()){
                iconaImageView.setImageResource(R.drawable.ico_cartella);
            } else {
                iconaImageView.setImageResource(IconManager.iconForFile(file));
                if(mostraAnteprime()){
                    //getExecutorService().execute(new LoadThumbnailThread(itemView.getContext(), iconaImageView, file, 30, 23));
                    int imageSizePx = (int) iconaImageView.getContext().getResources().getDimension(R.dimen.size_icona_lista_piccola); //ritorna pixel anche se espresso in dp
                    IconManager.showImageWithGlide(file, iconaImageView, imageSizePx, imageSizePx);
                }
            }
            manageFileVisibility(DatiFilesLocaliAdapterListaPiccola.this, file, iconaImageView, nomeFileTextView);
            manageFileLink(file, collegamentoImageView);
            manageTouch(file, DatiFilesLocaliAdapterListaPiccola.this);
        }
    }

}
