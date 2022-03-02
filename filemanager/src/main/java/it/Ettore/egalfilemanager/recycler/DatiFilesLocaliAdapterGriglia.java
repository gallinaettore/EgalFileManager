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

import java.io.File;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato griglia
 */
public class DatiFilesLocaliAdapterGriglia extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLocaliAdapterGriglia(@NonNull Context context, DatiFilesLocaliBaseAdapter.OnItemTouchListener listener){
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup .
     * @param viewType .
     * @return ViewHolder creato
     */
    @NonNull
    @Override
    public DatiFilesLocaliViewHolderGriglia onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_griglia_files, viewGroup, false);
        return new DatiFilesLocaliViewHolderGriglia(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLocaliViewHolderGriglia extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView;
        private final ImageView iconaImageView, collegamentoImageView;

        private DatiFilesLocaliViewHolderGriglia(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);
            collegamentoImageView = itemView.findViewById(R.id.collegamentoImageView);
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
                int defaultIcon = IconManager.iconForFile(file);
                iconaImageView.setImageResource(defaultIcon);
                if (mostraAnteprime()) {
                    //getExecutorService().execute(new LoadThumbnailThread(itemView.getContext(), iconaImageView, file, 36, 36));
                    int imageSizePx = (int) iconaImageView.getContext().getResources().getDimension(R.dimen.size_icona_lista_files); //ritorna pixel anche se espresso in dp
                    IconManager.showImageWithGlide(file, iconaImageView, imageSizePx, imageSizePx);
                }
            }
            manageFileVisibility(DatiFilesLocaliAdapterGriglia.this, file, iconaImageView, nomeFileTextView);
            manageFileLink(file, collegamentoImageView);
            manageTouch(file, DatiFilesLocaliAdapterGriglia.this);
        }
    }
}
