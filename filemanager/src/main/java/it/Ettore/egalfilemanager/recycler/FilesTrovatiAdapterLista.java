package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.graphics.Color;
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
import it.Ettore.egalfilemanager.ThemeUtils;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati (risultato della ricerca files) in formato lista
 */
public final class FilesTrovatiAdapterLista extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public FilesTrovatiAdapterLista(@NonNull Context context, OnItemTouchListener listener){
        super(context, listener);
    }


    /**
     * Creazione del View Holder
     * @param viewGroup .
     * @param viewType .
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesRecentiViewHolderLista onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_risultati_ricerca_files, viewGroup, false);
        return new DatiFilesRecentiViewHolderLista(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesRecentiViewHolderLista extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeTextView, descrizioneTextView, dimensioneTextView;
        private final ImageView iconaImageView;

        private DatiFilesRecentiViewHolderLista(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeTextView = itemView.findViewById(R.id.nomeFileTextView);
            descrizioneTextView = itemView.findViewById(R.id.infoFileTextView);
            dimensioneTextView = itemView.findViewById(R.id.dimensioneTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);

            if(LayoutDirectionHelper.isRightToLeft(itemView.getContext())){
                nomeTextView.setGravity(Gravity.RIGHT);
                descrizioneTextView.setGravity(Gravity.RIGHT);
                dimensioneTextView.setGravity(Gravity.RIGHT);
            }
        }


        /**
         * Effettua il bind dei dati
         * @param position posizione nell'adapter
         */
        @Override
        void bind(final int position){

            final File file = getListaFiles().get(position);
            if(file.isDirectory()){
                iconaImageView.setImageResource(R.drawable.ico_cartella);
            } else {
                iconaImageView.setImageResource(IconManager.iconForFile(file));
                if(mostraAnteprime()) {
                    //getExecutorService().execute(new LoadThumbnailThread(itemView.getContext(), iconaImageView, file, 36, 27));
                    int imageSizePx = (int) iconaImageView.getContext().getResources().getDimension(R.dimen.size_icona_lista_files); //ritorna pixel anche se espresso in dp
                    IconManager.showImageWithGlide(file, iconaImageView, imageSizePx, imageSizePx);
                }
            }
            nomeTextView.setText(file.getName());
            descrizioneTextView.setText(file.getParent());
            dimensioneTextView.setText(file.isDirectory() ? itemView.getContext().getString(R.string.directory) : MyMath.humanReadableByte(file.length(), getArrayBytes()));
            if(file.isHidden()){
                nomeTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
                descrizioneTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
                dimensioneTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
            } else {
                nomeTextView.setTextColor(getThemeUtils().getPrimaryTextColor());
                descrizioneTextView.setTextColor(getThemeUtils().getSecondaryTextColor());
                dimensioneTextView.setTextColor(getThemeUtils().getSecondaryTextColor());
            }

            manageTouch(file, FilesTrovatiAdapterLista.this);
        }
    }

}
