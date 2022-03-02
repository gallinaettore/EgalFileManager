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
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato lista
 */
public final class DatiFilesLocaliAdapterLista extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLocaliAdapterLista(@NonNull Context context, OnItemTouchListener listener){
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup .
     * @param viewType .
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesLocaliViewHolderLista onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_files, viewGroup, false);
        return new DatiFilesLocaliViewHolderLista(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLocaliViewHolderLista extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView, infoFileTextView;
        private final ImageView iconaImageView, collegamentoImageView;

        @SuppressLint("RtlHardcoded")
        private DatiFilesLocaliViewHolderLista(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            infoFileTextView = itemView.findViewById(R.id.infoFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);
            collegamentoImageView = itemView.findViewById(R.id.collegamentoImageView);

            if(LayoutDirectionHelper.isRightToLeft(itemView.getContext())){
                nomeFileTextView.setGravity(Gravity.RIGHT);
                infoFileTextView.setGravity(Gravity.RIGHT);
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
                infoFileTextView.setText(R.string.directory);
            } else {
                int defaultIcon = IconManager.iconForFile(file);
                iconaImageView.setImageResource(defaultIcon);
                if (mostraAnteprime()) {
                    int imageSizePx = (int) iconaImageView.getContext().getResources().getDimension(R.dimen.size_icona_lista_files); //ritorna pixel anche se espresso in dp
                    IconManager.showImageWithGlide(file, iconaImageView, imageSizePx, imageSizePx);
                }
                infoFileTextView.setText(MyMath.humanReadableByte(file.length(), getArrayBytes()));
            }
            manageFileVisibility(DatiFilesLocaliAdapterLista.this, file, iconaImageView, nomeFileTextView, infoFileTextView);
            manageFileLink(file, collegamentoImageView);
            manageTouch(file, DatiFilesLocaliAdapterLista.this);
        }
    }

}
