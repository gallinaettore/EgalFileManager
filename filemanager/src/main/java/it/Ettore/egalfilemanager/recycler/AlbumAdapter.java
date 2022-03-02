package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.iconmanager.IconManager;
import it.Ettore.egalfilemanager.mediastore.Album;


/**
 * Adapter per la visualizzazione degli albums all'interno della RecyclerView
 */
public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> implements ListFilter.Filterable {
    private final Context context;
    private final OnItemTouchListener listener;
    private List<Album> albums, backupListaCompleta;
    private final boolean mostraAnteprime;
    //private final ExecutorService executorService;



    /**
     *
     * @param context Context
     * @param albums Lista di albums
     * @param listener Listener per la gestione dei tocchi
     */
    public AlbumAdapter(@NonNull Context context, @NonNull List<Album> albums, OnItemTouchListener listener){
        this.context = context;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mostraAnteprime = prefs.getBoolean(Costanti.KEY_PREF_MOSTRA_ANTEPRIME, true);
        this.albums = albums;
        this.listener = listener;
    }


    /**
     * Numero di albums visualizzati
     * @return Numero di albums
     */
    @Override
    public int getItemCount() {
        return albums.size();
    }


    /**
     * Crea il view holder
     * @param parent .
     * @param viewType .
     * @return .
     */
    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View layout = LayoutInflater.from(context).inflate(R.layout.riga_album_categorie, parent, false);
        return new AlbumViewHolder(layout);
    }


    /**
     * Effettua il bind dei dati sul viewholder
     * @param holder ViewHolder
     * @param position Posizione
     */
    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        holder.bind(position);
    }


    /**
     * Abilita/disabilita la modalità filtro files nell'adapter
     * @param filterMode True per abilitare la modalità filtro
     */
    @Override
    public void setFilterMode(boolean filterMode){
        if(filterMode){
            backupListaCompleta = new ArrayList<>(albums);
        } else {
            if(backupListaCompleta != null){
                albums = new ArrayList<>(backupListaCompleta);
                backupListaCompleta = null;
            }
        }
    }


    /**
     * Mostra solo i files il cui nome che contiene la strimga query
     * @param query Stringa di filtraggio files
     */
    @Override
    public void filter(String query){
        if(backupListaCompleta == null) return;
        final List<Album> listaFiltrata;
        if(query == null || query.isEmpty()){
            listaFiltrata = new ArrayList<>(backupListaCompleta);
        } else {
            listaFiltrata = new ArrayList<>();
            for(Album album : backupListaCompleta){
                if(album.getNome().toLowerCase().contains(query.toLowerCase())){
                    listaFiltrata.add(album);
                }
            }
        }
        albums = listaFiltrata;
        notifyDataSetChanged();
    }



    /**
     *  View Holder dell'adapter di visualizzazione albums
     */
    class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final View itemView;
        private final ImageView iconaImageView;
        private final TextView nomeTextView, infoTextView, pathTextView;

        /**
         *
         * @param itemView View del viewholder
         */
        @SuppressLint("RtlHardcoded")
        private AlbumViewHolder(View itemView){
            super(itemView);
            this.itemView = itemView;
            nomeTextView = itemView.findViewById(R.id.nomeTextView);
            pathTextView = itemView.findViewById(R.id.pathTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView);
            iconaImageView = itemView.findViewById(R.id.iconaImageView);

            if(LayoutDirectionHelper.isRightToLeft(itemView.getContext())){
                nomeTextView.setGravity(Gravity.RIGHT);
                pathTextView.setGravity(Gravity.RIGHT);
                infoTextView.setGravity(Gravity.RIGHT);
            }
        }

        /**
         * Effettual il bind dei dati sul view holder
         * @param position Posizione dell'adapter
         */
        void bind(final int position){
            final Album album = albums.get(position);
            nomeTextView.setText(album.getNome());
            if(album.getPath() != null) {
                pathTextView.setVisibility(View.VISIBLE);
                pathTextView.setText(album.getPath());
            } else {
                pathTextView.setVisibility(View.GONE);
                pathTextView.setText(null);
            }
            iconaImageView.setImageResource(IconManager.iconForFile(album.getPrimoFile()));
            if(mostraAnteprime) {
                if(FileTypes.getTypeForFile(album.getPrimoFile()) != FileTypes.TYPE_APK) { //non mostro l'alteprima dell'apk
                    //executorService.execute(new LoadPreviewThread(context, iconaImageView, album.getPrimoFile(), 60, 60));
                    int imageSizePx = (int) MyMath.dpToPx(iconaImageView.getContext(), 60);
                    IconManager.showImageWithGlide(album.getPrimoFile(), iconaImageView, imageSizePx, imageSizePx);
                }
            }
            infoTextView.setText(String.format(Locale.ENGLISH, "%s %s", String.valueOf(album.size()), context.getString(R.string.elementi)));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemClick(album);
                    }
                }
            });
        }
    }


    /**
     * Interfaccia per la gestione dei tocchi
     */
    public interface OnItemTouchListener {
        void onItemClick(Album album);
    }
}
