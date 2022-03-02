package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;


/**
 * Adapter per la visualizzazione dei files audio all'interno della RecyclerView playlist
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlayListViewHolder> implements PlaylistTouchCallback.OnItemMoveListener {
    private final Context context;
    private final int resIdColorAccent, defaultTextColor;
    private final RecyclerViewListener listener;
    private List<File> playlist;
    private int selectedIndex;



    /**
     *
     * @param context Context
     * @param listener Listener per la gestione dei tocchi
     */
    public PlaylistAdapter(@NonNull Context context, RecyclerViewListener listener){
        this.context = context;
        this.playlist = new ArrayList<>();
        this.listener = listener;
        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        resIdColorAccent = typedValue.resourceId;
        this.defaultTextColor = new TextView(context).getTextColors().getDefaultColor();
    }


    /**
     * Numero di files nella playlist
     * @return Numero di files
     */
    @Override
    public int getItemCount() {
        return playlist.size();
    }


    /**
     * Crea il view holder
     * @param parent .
     * @param viewType .
     * @return .
     */
    @NonNull
    @Override
    public PlayListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View layout = LayoutInflater.from(context).inflate(R.layout.riga_playlist_musicplayer, parent, false);
        return new PlayListViewHolder(layout);
    }


    /**
     * Effettua il bind dei dati sul viewholder
     * @param holder ViewHolder
     * @param position Posizione
     */
    @SuppressLint("RtlHardcoded")
    @Override
    public void onBindViewHolder(@NonNull PlayListViewHolder holder, int position) {
        final File currentFile = playlist.get(position);
        final String nomeFile = FileUtils.getFileNameWithoutExt(currentFile);
        holder.nomeTextView.setText(nomeFile);
        if(selectedIndex == position){
            holder.nomeTextView.setTextColor(ContextCompat.getColor(context, resIdColorAccent));
        } else {
            holder.nomeTextView.setTextColor(defaultTextColor);
        }
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRecyclerViewItemClick(currentFile);
            }
        });

        if(LayoutDirectionHelper.isRightToLeft(context)){
            holder.nomeTextView.setGravity(Gravity.RIGHT);
        }
    }


    /**
     * Imposta i dati della PLayList
     * @param queue Coda cotenente i files già presneti nel servizio (può essere null al primo avvio)
     * @param filesFromIntent Files passati tramite intent
     */
    public void setPlaylist(List<MediaSessionCompat.QueueItem> queue, List<File> filesFromIntent){
        playlist.clear();
        if(queue != null) {
            //se il service contiene già dei files in coda li includo nella playlist
            for (MediaSessionCompat.QueueItem item : queue) {
                final String filePath = item.getDescription().getMediaId();
                playlist.add(new File(filePath));
            }
        }
        //aggiungo i files passati tramite intent
        if(filesFromIntent != null) {
            playlist.addAll(filesFromIntent);
        }
        notifyDataSetChanged();
    }


    /**
     * Evidenzia il file (colorandolo)
     * @param selectedIndex Indice del file da selezionare
     */
    public void setSelectedIndex(int selectedIndex){
        this.selectedIndex = selectedIndex;
        notifyDataSetChanged();
    }


    /**
     * Svuota la playlist
     */
    public void removeAll(){
        this.playlist.clear();
        this.selectedIndex = -1;
        notifyDataSetChanged();
    }


    /**
     * Restituisce la playlist
     * @return Playlist
     */
    public List<File> getPlaylist(){
        return this.playlist;
    }


    /**
     * Chiamato quando si sposta un item
     * @param fromPosition posizione iniziale
     * @param toPosition posizione di destinazione
     */
    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if(fromPosition < toPosition){
            for(int i = fromPosition; i < toPosition; i++){
                Collections.swap(playlist, i, i+1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--){
                Collections.swap(playlist, i, i-1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        if(listener != null){
            listener.onRecyclerViewItemMoved(fromPosition, toPosition);
        }
    }


    /**
     * Chiamato quando si effettua lo swipe verso destra di un item
     * @param position Posizione dell'item su cui è stato effettuato lo swipe
     */
    @Override
    public void onItemSwiped(int position) {
        playlist.remove(position);
        notifyItemRemoved(position);
        if(listener != null){
            listener.onRecyclerViewItemDeleted(position);
        }
    }





    /**
     *  View Holder dell'adapter di visualizzazione albums
     */
    class PlayListViewHolder extends RecyclerView.ViewHolder {
        private final View itemView;
        private final TextView nomeTextView;

        /**
         *
         * @param itemView View del viewholder
         */
        private PlayListViewHolder(View itemView){
            super(itemView);
            this.itemView = itemView;
            this.nomeTextView = itemView.findViewById(R.id.nomeTextView);
        }
    }


    /**
     * Interfaccia per la gestione dei tocchi
     */
    public interface RecyclerViewListener {
        void onRecyclerViewItemClick(File file);
        void onRecyclerViewItemMoved(int fromPosition, int toPosition);
        void onRecyclerViewItemDeleted(int position);
    }
}
