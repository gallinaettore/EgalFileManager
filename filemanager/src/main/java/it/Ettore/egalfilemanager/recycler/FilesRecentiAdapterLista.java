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
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per la visualizzazione dei dati in formato lista
 */
public final class FilesRecentiAdapterLista extends DatiFilesLocaliBaseAdapter {

    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    public FilesRecentiAdapterLista(@NonNull Context context, OnItemTouchListener listener){
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
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_files, viewGroup, false);
        return new DatiFilesRecentiViewHolderLista(layout);
    }





    /**
     *  View Holder che si occupa anche del bind i dati
     */
    private final class DatiFilesRecentiViewHolderLista extends DatiFilesLocaliBaseViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView, infoFileTextView;
        private final ImageView iconaImageView;

        @SuppressLint("RtlHardcoded")
        private DatiFilesRecentiViewHolderLista(View itemView){
            super(itemView, R.id.checkbox_selezionato);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            infoFileTextView = itemView.findViewById(R.id.infoFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);

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
            iconaImageView.setImageResource(IconManager.iconForFile(file));
            if (mostraAnteprime()) {
                //getExecutorService().execute(new LoadThumbnailThread(itemView.getContext(), iconaImageView, file, 36, 27));
                int imageSizePx = (int) iconaImageView.getContext().getResources().getDimension(R.dimen.size_icona_lista_files); //ritorna pixel anche se espresso in dp
                IconManager.showImageWithGlide(file, iconaImageView, imageSizePx, imageSizePx);
            }
            final Date date = new Date(file.lastModified());
            String dateString;
            try {
                final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT, Locale.getDefault());
                dateString = dateFormat.format(date);
            } catch (Exception e){
                final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT, Locale.ENGLISH);
                dateString = dateFormat.format(date);
            }
            final String fileSize = MyMath.humanReadableByte(file.length(), getArrayBytes());

            infoFileTextView.setText(String.format("%s   (%s)", dateString, fileSize));
            if(file.isHidden()){
                nomeFileTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
                infoFileTextView.setTextColor(getThemeUtils().getHiddenFileTextColor());
            } else {
                nomeFileTextView.setTextColor(getThemeUtils().getPrimaryTextColor());
                infoFileTextView.setTextColor(getThemeUtils().getSecondaryTextColor());
            }
            manageTouch(file, FilesRecentiAdapterLista.this);
        }
    }

}
