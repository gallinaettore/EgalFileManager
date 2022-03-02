package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityAggiungiFileAPlaylist;
import it.Ettore.egalfilemanager.mediastore.Album;
import it.Ettore.egalfilemanager.mediastore.FindAlbumsTask;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mediastore.OrdinatoreAlbums;
import it.Ettore.egalfilemanager.recycler.AlbumAdapter;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;



public class FragmentAggiungiAPlaylistAlbum extends GeneralFragment implements AlbumAdapter.OnItemTouchListener, FindAlbumsTask.AlbumsSearchListener {
    private ActivityAggiungiFileAPlaylist activity;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private List<Album> albums;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentAggiungiAPlaylistAlbum(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_aggiungi_a_playlist_album, container, false);

        //visualizzazione
        recyclerView = v.findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());
        emptyView = v.findViewById(R.id.empty_view);
        progressBar = v.findViewById(R.id.progressBar);

        //avvia la ricerca se è la prima volta che il fragment viene visualizzato
        //altrimenti mostra la ricerca precedente
        if(albums == null) {
            avviaRicerca();
        } else {
            mostraAlbum();
        }

        return v;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (ActivityAggiungiFileAPlaylist)getActivity();
    }

    private void avviaRicerca(){
        final FindAlbumsTask findAlbumsTask = new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_AUDIO, this);
        findAlbumsTask.setMostraCartellePerFilesAudio(true);
        findAlbumsTask.execute();
    }


    /**
     * Chiamato al termine della ricerca degli albums
     * @param albums Lista degli albums trovati
     */
    @Override
    public void onAlbumsFound(List<Album> albums) {
        if(getContext() == null){
            //il context è null quando cambio orientamento durante la ricerca e non ha il tempo di attaccare il nuovo fragment all'activity
            return;
        }

        this.albums = albums;
        mostraAlbum();
    }


    /**
     * Mostra i dati dell'album trovato
     */
    private void mostraAlbum(){
        progressBar.setVisibility(View.GONE);
        if(albums.size() > 0){
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.requestFocus();
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        albums = new OrdinatoreAlbums().ordinaAlbums(albums);
        final AlbumAdapter adapter = new AlbumAdapter(getContext(), albums, this);
        recyclerView.setAdapter(adapter);
    }


    @Override
    public void onItemClick(Album album) {
        final FragmentAggiungiAPlaylistElementi fragment = FragmentAggiungiAPlaylistElementi.getInstance(album.getId(), album.getNome());
        activity.showFragment(fragment);
    }
}
