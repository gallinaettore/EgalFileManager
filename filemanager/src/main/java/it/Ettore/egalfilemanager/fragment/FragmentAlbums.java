package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.DialogDisponiFilesAudioBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaAlbumsBuilder;
import it.Ettore.egalfilemanager.mediastore.Album;
import it.Ettore.egalfilemanager.mediastore.FindAlbumsTask;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mediastore.OrdinatoreAlbums;
import it.Ettore.egalfilemanager.recycler.AlbumAdapter;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;

import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_AUDIO;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_DOCUMENTI;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_IMMAGINI;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_VIDEO;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_DISPOSIZIONE_FILES_AUDIO;
import static it.Ettore.egalfilemanager.dialog.DialogDisponiFilesAudioBuilder.DISPOSIZIONE_NOME_CARTELLA;


/**
 * Fragment per la visualizzazione degli album presenti nel media store
 */
public class FragmentAlbums extends GeneralFragment implements AlbumAdapter.OnItemTouchListener, FindAlbumsTask.AlbumsSearchListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String KEY_BUNDLE_TIPO_CATEGORIA = "tipo_categoria";
    private ActivityMain activityMain;
    private RecyclerView recyclerView;
    private View emptyView;
    private AlbumAdapter adapter;
    private OrdinatoreAlbums ordinatoreAlbums;
    private List<Album> albums;
    private ProgressBar progressBar;
    private ListFilter filter;
    private int categoriaFiles;
    private int disposizioneFilesAudio = DISPOSIZIONE_NOME_CARTELLA;
    private SwipeRefreshLayout swipeLayout;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentAlbums(){}


    /**
     * Metodo factory per creare un'istanza del fragment
     * @param categoria Costante categoria (utilizzare una delle costanti presenti nella classe Costanti)
     * @return Instanza del fragment
     */
    public static FragmentAlbums getInstance(int categoria){
        final FragmentAlbums fragment = new FragmentAlbums();
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_BUNDLE_TIPO_CATEGORIA, categoria);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        this.categoriaFiles = getArguments().getInt(KEY_BUNDLE_TIPO_CATEGORIA, 0);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_albums, container, false);
        activityMain = (ActivityMain)getActivity();
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();
        activityMain.setActionBarTitle(getNomeCategoria());

        swipeLayout = v.findViewById(R.id.swipe_layout);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.colorAccent);

        //visualizzazione
        recyclerView = v.findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());
        emptyView = v.findViewById(R.id.empty_view);
        progressBar = v.findViewById(R.id.progressBar);
        ordinatoreAlbums = new OrdinatoreAlbums();

        //avvia la ricerca se è la prima volta che il fragment viene visualizzato
        //altrimenti mostra la ricerca precedente
        if(albums == null) {
            avviaRicerca();
        } else {
            mostraAlbum();
        }

        return v;
    }


    private void avviaRicerca(){
        if(!activityMain.getPermissionsManager().hasPermissions()){
            activityMain.getPermissionsManager().requestPermissions();
            activityMain.finishCurrentFragment();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        ordinatoreAlbums.ottieniStatoOrdinamento(getPrefs());

        //avvio la ricerca degli albums per la categoria selezionata
        disposizioneFilesAudio = getPrefs().getInt(KEY_PREF_DISPOSIZIONE_FILES_AUDIO, DialogDisponiFilesAudioBuilder.DISPOSIZIONE_NOME_CARTELLA);
        switch (categoriaFiles){
            case CATEGORIA_IMMAGINI:
                new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_IMAGE, this).execute();
                break;
            case CATEGORIA_VIDEO:
                new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_VIDEO, this).execute();
                break;
            case CATEGORIA_AUDIO:
                final FindAlbumsTask findAlbumsTask = new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_AUDIO, this);
                findAlbumsTask.setMostraCartellePerFilesAudio(disposizioneFilesAudio == DISPOSIZIONE_NOME_CARTELLA);
                findAlbumsTask.execute();
                break;
            case CATEGORIA_DOCUMENTI:
                new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_FILES, this).execute();
                break;
            default:
                throw new IllegalArgumentException("Categoria non gestita: " + categoriaFiles);
        }
    }


    /**
     * Aggiorna tutto
     */
    @Override
    public void onRefresh() {
        swipeLayout.setRefreshing(false);
        avviaRicerca();
    }


    @Override
    public void onStop(){
        super.onStop();
        //chiudo la search view
        if(filter != null) {
            filter.chiudiSearchView();
        }
        ordinatoreAlbums.salvaStatoOrdinamento(getPrefs());
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

        albums = ordinatoreAlbums.ordinaAlbums(albums);
        adapter = new AlbumAdapter(getContext(), albums, this);
        recyclerView.setAdapter(adapter);
        if(filter != null){
            filter.configuraSearchView(adapter);
        }

        mostraNumeroFilesTotali(albums);
    }


    /**
     * Analizza gli albums trovati e mostra il numero di files totale
     * @param albums Albums trovati
     */
    private void mostraNumeroFilesTotali(List<Album> albums){
        int totFiles = 0;
        for(Album album : albums){
            totFiles += album.size();
        }
        activityMain.setActionBarTitle(String.format("%s  (%s)", getNomeCategoria(), String.valueOf(totFiles)));
    }


    /**
     * Restituisce il nome della categoria
     * @return Nome della categoria
     */
    private String getNomeCategoria(){
        switch (categoriaFiles){
            case CATEGORIA_IMMAGINI:
                return getString(R.string.categorie_immagini);
            case CATEGORIA_VIDEO:
                return getString(R.string.categorie_video);
            case CATEGORIA_AUDIO:
                return getString(R.string.categorie_audio);
            case CATEGORIA_DOCUMENTI:
                return getString(R.string.categorie_altro);
            default:
                throw new IllegalArgumentException("Categoria non gestita: " + categoriaFiles);
        }
    }


    /**
     * Chiamato al click di un album. Viene visualizzato il fragment col suo contenuto.
     * @param album Album su cui si effettua il click
     */
    @Override
    public void onItemClick(Album album) {
        //tratta il tipo playlist come un tipo audio
        int mediaType = album.getMediaType() == MediaUtils.MEDIA_TYPE_PLAYLIST ? MediaUtils.MEDIA_TYPE_AUDIO : album.getMediaType();
        activityMain.showFragment(FragmentElementiAlbum.getInstance(album.getId(), album.getNome(), mediaType));
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_album, menu);

        //funzione disponi solo per files audio
        final MenuItem itemDisponi = menu.findItem(R.id.disponi);
        itemDisponi.setVisible(categoriaFiles == CATEGORIA_AUDIO);

        //filtro
        final MenuItem searchItem = menu.findItem(R.id.filtro);
        this.filter = new ListFilter((SearchView) searchItem.getActionView());
        this.filter.configuraSearchView(adapter);

        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aggiorna:
                avviaRicerca();
                return true;
            case R.id.ordina:
                if(albums != null && !albums.isEmpty()) {
                    boolean usaOrdinaPercorso = albums.get(0).getPath() != null;
                    final DialogOrdinaAlbumsBuilder ordinaBuilder = new DialogOrdinaAlbumsBuilder(getContext(), ordinatoreAlbums, usaOrdinaPercorso, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            albums = ordinatoreAlbums.ordinaAlbums(albums);
                            adapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(0);
                        }
                    });
                    ordinaBuilder.create().show();
                }
                return true;
            case R.id.disponi:
                final DialogDisponiFilesAudioBuilder disponiBuilder = new DialogDisponiFilesAudioBuilder(getContext(), disposizioneFilesAudio, new DialogDisponiFilesAudioBuilder.DisponiFilesAudioListener() {
                    @Override
                    public void onArrangementChanged(int nuovaDisposizione) {
                        getPrefs().edit().putInt(KEY_PREF_DISPOSIZIONE_FILES_AUDIO, nuovaDisposizione).apply();
                        avviaRicerca();
                    }
                });
                disponiBuilder.create().show();
                return true;
            case R.id.filtro:
                return true;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
    }
}
