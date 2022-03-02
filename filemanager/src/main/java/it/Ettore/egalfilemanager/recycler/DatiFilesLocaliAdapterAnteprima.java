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
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato anteprima
 */
public class DatiFilesLocaliAdapterAnteprima extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public DatiFilesLocaliAdapterAnteprima(@NonNull Context context, OnItemTouchListener listener){
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
    public DatiFilesLocaliViewHolderAnteprima onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_anteprima_files, viewGroup, false);
        return new DatiFilesLocaliViewHolderAnteprima(layout);
    }






    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesLocaliViewHolderAnteprima extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView;
        private final ImageView anteprimaImageView, miniaturaImageView, collegamentoImageView;

        private DatiFilesLocaliViewHolderAnteprima(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.text_view_nome_file);
            anteprimaImageView = itemView.findViewById(R.id.image_view_anteprima);
            miniaturaImageView = itemView.findViewById(R.id.image_view_miniatura);
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
            anteprimaImageView.setScaleType(ImageView.ScaleType.CENTER);
            if(file.isDirectory()){
                anteprimaImageView.setImageResource(R.drawable.ico_cartella);
                miniaturaImageView.setImageResource(R.drawable.miniatura_cartella);
            } else {
                anteprimaImageView.setImageResource(IconManager.iconForFile(file));
                miniaturaImageView.setImageResource(IconManager.miniaturaForFile(file));
                if (mostraAnteprime()) {
                    //120 è l'altezza fissa della view, 200 (penso che la massima lunghezza non superi mai 200dp)
                    //getExecutorService().execute(new LoadPreviewThread(itemView.getContext(), anteprimaImageView, file, 200, 120));
                    IconManager.showImageWithGlide(file, anteprimaImageView, (int) MyMath.dpToPx(anteprimaImageView.getContext(), 200), (int)MyMath.dpToPx(anteprimaImageView.getContext(), 120));
                }
            }
            manageFileVisibility(DatiFilesLocaliAdapterAnteprima.this, file, anteprimaImageView, nomeFileTextView);
            manageFileLink(file, collegamentoImageView);
            manageTouch(file, DatiFilesLocaliAdapterAnteprima.this);
        }
    }
}
