package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityAggiungiFileAPlaylist;
import it.Ettore.egalfilemanager.activity.ActivityMusicPlayer;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.mediastore.Album;
import it.Ettore.egalfilemanager.mediastore.FindAlbumsTask;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneFilesLocali;

import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE;


public class FragmentAggiungiAPlaylistElementi extends FragmentBaseExplorer implements DatiFilesLocaliBaseAdapter.OnItemTouchListener, FindAlbumsTask.AlbumsSearchListener {
    private static final String KEY_BUNDLE_NOME_ALBUM = "nome_album";
    private static final String KEY_BUNDLE_ID_ALBUM = "id_album";

    private long idAlbum;
    private RecyclerView recyclerView;
    private DatiFilesLocaliBaseAdapter adapter;
    private LinearLayout progressLayout, resultLayout;
    private List<String> elementi;



    /**
     * Costruttore di base (necessario)
     */
    public FragmentAggiungiAPlaylistElementi() {
    }


    /**
     * Metodo factory per creare un'istanza del fragment
     * @param idAlbum Id dell'album di cui visualizzare il contenuto
     * @param nomeAlbum Nome dell'album
     * @return Instanza del fragment
     */
    public static FragmentAggiungiAPlaylistElementi getInstance(long idAlbum, @NonNull String nomeAlbum) {
        final FragmentAggiungiAPlaylistElementi fragment = new FragmentAggiungiAPlaylistElementi();
        final Bundle bundle = new Bundle();
        bundle.putLong(KEY_BUNDLE_ID_ALBUM, idAlbum);
        bundle.putString(KEY_BUNDLE_NOME_ALBUM, nomeAlbum);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        this.idAlbum = bundle.getLong(KEY_BUNDLE_ID_ALBUM);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        ActivityAggiungiFileAPlaylist activity = (ActivityAggiungiFileAPlaylist) getActivity();
        setActivityMain(activity);
        setTitle(R.string.aggiungi_a_playlist);

        //visualizzazione
        recyclerView = v.findViewById(R.id.recycler_view);
        int tipoVisualizzazione = VisualizzazioneBase.VISUALIZZAZIONE_LISTA;
        final VisualizzazioneFilesLocali visualizzazione = new VisualizzazioneFilesLocali(recyclerView, this);
        visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
        adapter = (DatiFilesLocaliBaseAdapter) recyclerView.getAdapter();
        setMultiselectableAdapter(adapter);

        //pathbar e progress
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        pathView.setVisibility(View.GONE);
        progressLayout = v.findViewById(R.id.progress_layout);
        resultLayout = v.findViewById(R.id.result_layout);

        //fab
        final FloatingActionButton fab = v.findViewById(R.id.fab);
        fab.hide();

        //esco dalla modalità selezione multipla premendo il tasto indietro
        configuraBackButton(v);

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();
        showProgress(true);
        avviaScansioneAlbums();
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    private void showProgress(boolean show){
        if(show){
            progressLayout.setVisibility(View.VISIBLE);
            resultLayout.setVisibility(View.GONE);
        } else {
            progressLayout.setVisibility(View.GONE);
            resultLayout.setVisibility(View.VISIBLE);
        }
    }


    private void avviaScansioneAlbums(){
        final FindAlbumsTask findAlbumsTask = new FindAlbumsTask(getActivity(), MediaUtils.MEDIA_TYPE_AUDIO, this); //avvio la ricerca di tutti gli albums per poi scegliere solo quello che mi serve
        findAlbumsTask.setMostraCartellePerFilesAudio(true);
        findAlbumsTask.execute();
    }


    @Override
    public void onAlbumsFound(List<Album> albums) {
        Album currentAlbum = null;
        for (Album album : albums) {
            if (album.getId() == this.idAlbum) {
                currentAlbum = album; //estraggo l'album di cui visualizzare gli elementi
                break;
            }
        }
        if (currentAlbum != null) {
            elementi = currentAlbum.getElementi();

        } else {
            //l'album non è stato trovato (o è vuoto in seguito alla cancellazione di tutti i files o c'è un errore) lo visualizzo come vuoto
            elementi = new ArrayList<>();
        }
        mostraFiles();
    }


    /**
     * Mostra i files all'interno dell'adapter
     */
    private void mostraFiles() {
        showProgress(false);
        List<File> listaFiles = new ArrayList<>(elementi.size());
        for (String path : elementi) {
            listaFiles.add(new File(path));
        }
        listaFiles = new OrdinatoreFiles(getPrefs()).ordinaListaFiles(listaFiles);
        boolean aggiornato = adapter.update(listaFiles);
        if(aggiornato) {
            recyclerView.scrollToPosition(0);
        }
        recyclerView.requestFocus();

        //dopo l'aggiornamento disattivo la modalità filtro se attivata
        adapter.setFilterMode(false);
    }


    @Override
    public void onItemClick(File file) {
        //non effettuo nessuna operazione al click
    }


    @Override
    public void onItemLongClick(File file) {
        //dopo aver attivato la selezione multipla
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }



    /* MENU */


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        //selezione multipla
        if(adapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_aggiungi_a_playlist, menu);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aggiungi_a_playlist:
                if(adapter.numElementiSelezionati() > 0) {
                    //avvio l'activity music player che essendo già avviata aggiune i files alla playlist
                    final SerializableFileList listaAudio = SerializableFileList.fromFileList(adapter.getElementiSelezionati());
                    final Intent intentMusicPlayer = new Intent(getContext(), ActivityMusicPlayer.class);
                    //intentMusicPlayer.setFlags(Intent.FLAG_ACTIVITY_);
                    intentMusicPlayer.putExtra(KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE, listaAudio);
                    try {
                        startActivity(intentMusicPlayer);
                        getActivity().finish();
                    } catch (Exception e){
                        ColoredToast.makeText(getContext(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                        e.printStackTrace(); //si potrebbe generare una TransactionTooLargeException se il numero di files è troppo elevato
                    }
                } else {
                    ColoredToast.makeText(getContext(), R.string.nessun_file_audio, Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
