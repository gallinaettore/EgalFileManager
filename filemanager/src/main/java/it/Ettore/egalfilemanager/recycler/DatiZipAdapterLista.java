package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;
import it.Ettore.egalfilemanager.zipexplorer.ArchiveEntry;


/**
 *  Adapter per la visualizzazione degli elementi degli archivi compressi
 */
public final class DatiZipAdapterLista extends RecyclerView.Adapter<DatiZipAdapterLista.DatiFilesViewHolderLista> {
    private List<ArchiveEntry> listaEntry;
    private final OnItemTouchListener listener;


    /**
     *
     * @param listaEntry Lista di elementi
     * @param listener Listener per la gestione dei tocchi di un'item sulla RecyclerView
     */
    public DatiZipAdapterLista(final List<ArchiveEntry> listaEntry, OnItemTouchListener listener){
        this.listaEntry = listaEntry;
        this.listener = listener;
    }


    /**
     * Creazione del View Holder
     * @param viewGroup .
     * @param viewType .
     * @return ViewHolder creato
     */
    @Override
    public DatiFilesViewHolderLista onCreateViewHolder(ViewGroup viewGroup, int viewType){
        final View layout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.riga_lista_files, viewGroup, false);
        return new DatiFilesViewHolderLista(layout);
    }


    /**
     * Bind del View Holder
     * @param holder View Holder
     * @param position Posizione
     */
    @Override
    public void onBindViewHolder(DatiFilesViewHolderLista holder, int position) {
        holder.bind(listaEntry.get(position));
    }


    /**
     * Numero di elementi nell'adapter
     * @return Numero di elementi nell'adapter
     */
    @Override
    public int getItemCount() {
        return listaEntry.size();
    }


    /**
     * Aggiorna l'adapter con una nuova lista di elementi
     * @param listaEntry Nuova lista di elementi
     */
    public void update(List<ArchiveEntry> listaEntry){
        this.listaEntry = listaEntry;
        notifyDataSetChanged();
    }




    /**
     *  View Holder che si occupa anche del bind i dati
     */
    final class DatiFilesViewHolderLista extends RecyclerView.ViewHolder {
        private final View itemView;
        private final TextView nomeFileTextView, infoFileTextView;
        private final ImageView iconaImageView;
        private final CheckBox checkboxSelezionato;

        @SuppressLint("RtlHardcoded")
        private DatiFilesViewHolderLista(View itemView){
            super(itemView);
            this.itemView = itemView;
            nomeFileTextView = itemView.findViewById(R.id.nomeFileTextView);
            infoFileTextView = itemView.findViewById(R.id.infoFileTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);
            checkboxSelezionato = itemView.findViewById(R.id.checkbox_selezionato);

            if(LayoutDirectionHelper.isRightToLeft(itemView.getContext())){
                nomeFileTextView.setGravity(Gravity.RIGHT);
                infoFileTextView.setGravity(Gravity.RIGHT);
            }
        }


        /**
         * Effettua il bind dei dati
         * @param entry Elemento dell'archivio
         */
        void bind(final ArchiveEntry entry){
            nomeFileTextView.setText(entry.getName());
            if(entry.isDirectory()){
                iconaImageView.setImageResource(R.drawable.ico_cartella);
                infoFileTextView.setText(R.string.directory);
            } else {
                iconaImageView.setImageResource(IconManager.iconForFile(entry.getName()));
                infoFileTextView.setText(R.string.file);
            }
            checkboxSelezionato.setVisibility(View.GONE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemClick(entry);
                    }
                }
            });

        }
    }



    //Interfaccia per la gestione dei tocchi
    public interface OnItemTouchListener {

        /**
         * Chiamato al tocco semplice di un elemento sulla RecyclerView
         * @param entry Elemento dell'archivio selezionato
         */
        void onItemClick(ArchiveEntry entry);
    }
}
