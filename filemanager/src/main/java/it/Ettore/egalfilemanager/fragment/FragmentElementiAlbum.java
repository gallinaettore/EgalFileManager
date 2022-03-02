package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
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
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.activity.ActivityMusicPlayer;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.dialog.DialogDisponiFilesAudioBuilder;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewNameBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaFilesBuilder;
import it.Ettore.egalfilemanager.dialog.DialogVisualizzazioneBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.filemanager.ProprietaTask;
import it.Ettore.egalfilemanager.filemanager.thread.CreaFileTask;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.filemanager.thread.RinominaHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.fileutils.PreferitiManager;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.fileutils.TroppiElementiException;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.mediastore.Album;
import it.Ettore.egalfilemanager.mediastore.FindAlbumsTask;
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneFilesLocali;
import it.Ettore.egalfilemanager.widget.MyWidgetManager;

import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE;
import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_PRESENTAZIONE;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_DISPOSIZIONE_FILES_AUDIO;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_VISUALIZZAZIONE;
import static it.Ettore.egalfilemanager.dialog.DialogDisponiFilesAudioBuilder.DISPOSIZIONE_NOME_CARTELLA;
import static it.Ettore.egalfilemanager.mediastore.MediaUtils.MEDIA_TYPE_IMAGE;


/**
 * Fragment per la visualizzazione degli elementi presenti in un album
 */
public class FragmentElementiAlbum extends FragmentBaseExplorer implements DatiFilesLocaliBaseAdapter.OnItemTouchListener, SwipeRefreshLayout.OnRefreshListener,
        FindAlbumsTask.AlbumsSearchListener, MediaScannerUtil.MediaScannerListener, CopyHandlerListener, RinominaHandler.RinominaListener, CreaFileTask.CreaFileListener,
        ProprietaTask.ProprietaNascondiListener, EliminaHandler.EliminaListener{
    private static final String KEY_BUNDLE_NOME_ALBUM = "nome_album";
    private static final String KEY_BUNDLE_ID_ALBUM = "id_album";
    private static final String KEY_BUNDLE_MEDIA_TYPE = "media_type";
    private ActivityMain activityMain;
    private SwipeRefreshLayout swipeLayoutReciclerView, swipeLayoutEmptyView;
    private RecyclerView recyclerView;
    private VisualizzazioneFilesLocali visualizzazione;
    private DatiFilesLocaliBaseAdapter adapter;
    private FileOpener fileOpener;
    private FileManager fileManager;
    private List<String> elementi;
    private long idAlbum;
    private int mediaType;
    private LinearLayout progressLayout, resultLayout;
    private ListFilter filter;
    private OrdinatoreFiles ordinatoreFiles;
    private MyWidgetManager widgetManager;
    private CopyHandler copyHandler;
    private EliminaHandler eliminaHandler;
    private RinominaHandler rinominaHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentElementiAlbum() {
    }


    /**
     * Metodo factory per creare un'istanza del fragment
     * @param idAlbum Id dell'album di cui visualizzare il contenuto
     * @param nomeAlbum Nome dell'album
     * @param mediaType Tipo di media di visualizzare (Usare le costanti della classe MediaUtils, tranne MEDIA_TYPE_INVALID)
     * @return Instanza del fragment
     */
    public static FragmentElementiAlbum getInstance(long idAlbum, @NonNull String nomeAlbum, int mediaType) {
        final FragmentElementiAlbum fragment = new FragmentElementiAlbum();
        final Bundle bundle = new Bundle();
        bundle.putLong(KEY_BUNDLE_ID_ALBUM, idAlbum);
        bundle.putString(KEY_BUNDLE_NOME_ALBUM, nomeAlbum);
        bundle.putInt(KEY_BUNDLE_MEDIA_TYPE, mediaType);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getArguments();
        this.idAlbum = bundle.getLong(KEY_BUNDLE_ID_ALBUM);
        this.mediaType = bundle.getInt(KEY_BUNDLE_MEDIA_TYPE);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        eliminaHandler = new EliminaHandler(getActivity(), this);
        rinominaHandler = new RinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        activityMain = (ActivityMain) getActivity();
        setActivityMain(activityMain);
        setTitle(getArguments().getString(KEY_BUNDLE_NOME_ALBUM, null));

        swipeLayoutReciclerView = v.findViewById(R.id.swipe_layout_recyclerview);
        swipeLayoutReciclerView.setOnRefreshListener(this);
        swipeLayoutReciclerView.setColorSchemeResources(R.color.colorAccent);
        swipeLayoutEmptyView = v.findViewById(R.id.swipe_layout_emptyview);
        swipeLayoutEmptyView.setOnRefreshListener(this);
        swipeLayoutEmptyView.setColorSchemeResources(R.color.colorAccent);

        //visualizzazione
        recyclerView = v.findViewById(R.id.recycler_view);
        int tipoVisualizzazione = getPrefs().getInt(KEY_PREF_TIPO_VISUALIZZAZIONE, VisualizzazioneBase.VISUALIZZAZIONE_LISTA);
        visualizzazione = new VisualizzazioneFilesLocali(recyclerView, this);
        visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
        adapter = (DatiFilesLocaliBaseAdapter) recyclerView.getAdapter();
        setMultiselectableAdapter(adapter);

        //pathbar e progress
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        pathView.setVisibility(View.GONE);
        progressLayout = v.findViewById(R.id.progress_layout);
        resultLayout = v.findViewById(R.id.result_layout);

        fileManager = new FileManager(getContext(), getPrefs());
        fileManager.setMediaScannerListener(this);
        fileOpener = new FileOpener(getContext());
        ordinatoreFiles = new OrdinatoreFiles(getPrefs());
        widgetManager = new MyWidgetManager(getContext());

        //fab
        final FloatingActionButton fab = v.findViewById(R.id.fab);
        fab.hide();
        fab.setFocusable(false);
        //fab.setVisibility(View.GONE);

        //esco dalla modalità selezione multipla premendo il tasto indietro
        configuraBackButton(v);

        return v;

    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();
        ordinatoreFiles.ottieniStatoOrdinamento();
        showProgress(true);
        avviaScansioneAlbums();
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    @Override
    public void onStop() {
        super.onStop();
        if(filter != null) {
            filter.chiudiSearchView();
        }
        ordinatoreFiles.salvaStatoOrdinamento();
    }


    @Override
    public void onDestroy(){
        if(copyHandler != null) {
            copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        }
        if(eliminaHandler != null) {
            eliminaHandler.dismissProgressDialogOnDestroy();
        }
        if(rinominaHandler != null) {
            rinominaHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
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
        listaFiles = ordinatoreFiles.ordinaListaFiles(listaFiles);
        boolean aggiornato = adapter.update(listaFiles);
        if(aggiornato) {
            recyclerView.scrollToPosition(0);
        }
        recyclerView.requestFocus();

        //dopo l'aggiornamento disattivo la modalità filtro se attivata
        adapter.setFilterMode(false);
        if(filter != null){
            filter.chiudiSearchView();
        }
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


    @Override
    public void onItemClick(File file) {
        if (!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            fileOpener.openFile(file);
        } else {
            //modalità selezione multipla
            mostraNumeroElementiSelezionati(true);
        }
    }


    @Override
    public void onItemLongClick(File file) {
        //dopo aver attivato la selezione multipla
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    @Override
    public void onRefresh() {
        avviaScansioneAlbums();
        swipeLayoutReciclerView.setRefreshing(false);
        swipeLayoutEmptyView.setRefreshing(false);
    }


    private void avviaScansioneAlbums(){
        final FindAlbumsTask findAlbumsTask = new FindAlbumsTask(getActivity(), mediaType, this); //avvio la ricerca di tutti gli albums per poi scegliere solo quello che mi serve
        if(getPrefs() != null) { //in rari casi prefs può essere null perchè il fragment è stato chiuso
            int disposizioneFilesAudio = getPrefs().getInt(KEY_PREF_DISPOSIZIONE_FILES_AUDIO, DialogDisponiFilesAudioBuilder.DISPOSIZIONE_NOME_CARTELLA);
            findAlbumsTask.setMostraCartellePerFilesAudio(disposizioneFilesAudio == DISPOSIZIONE_NOME_CARTELLA);
            findAlbumsTask.execute();
        }
    }


    /**
     * Al termine della ricerca (avviata in fase di aggiornamento) ottiene la lista di tutti gli albums
     * @param albums Lista albums trovati
     */
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
     * Dopo aver avviato le dialog per la scelta della cartella di destinazione avvia la copia o lo spostamento dei files selezionati presenti nell'adapter
     * @param sposta True modalità sposta. False modalità copia.
     */
    private void copiaIn(final boolean sposta) {
        if (adapter.numElementiSelezionati() == 0) return;
        //dialog per la scelta dello storage
        final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
        builder.setTitle(R.string.seleziona_destinazione);
        builder.hideIcon(true);
        builder.setStorageItems(new HomeNavigationManager(getActivity()).listaItemsArchivioLocale());
        builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
            @Override
            public void onSelectStorage(File storagePath) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(getContext(), DialogFileChooserBuilder.TYPE_SELECT_FOLDER);
                fileChooser.setTitle(R.string.seleziona_destinazione);
                fileChooser.setStartFolder(storagePath);
                fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                    @Override
                    public void onFileChooserSelected(final File destination) {
                        if (!sposta) {
                            fileManager.copia(adapter.getElementiSelezionati(), destination, copyHandler);
                        } else {
                            fileManager.sposta(adapter.getElementiSelezionati(), destination, copyHandler);
                        }
                    }

                    @Override
                    public void onFileChooserCanceled() {
                    }
                });
                fileChooser.create().show();
                chiediPermessiScritturaExtSd(storagePath);
            }

            @Override
            public void onCancelStorageSelection() {
            }
        });
        builder.showSelectDialogIfNecessary();
    }


    /**
     * Se il file si trova su un percorso esterno, mostra la dialog per ottenere il tree uri della sd esterna
     * @param file File
     */
    private void chiediPermessiScritturaExtSd(File file){
        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
        new ChiediTreeUriTask(activityMain, file, true).execute();
    }






    /**
     * Chiamato quando il service ha terminato la copia dei files
     * @param success True se la copia è avvenuta con successo
     * @param destinationPath Path di destinazione
     * @param filesCopiati Lista con i path dei files copiati correttamente
     * @param tipoCopia Una della variabili COPY della classe CopyService (specifica se la copia è avvenuta ad esempio da smb a locale)
     */
    @Override
    public void onCopyServiceFinished(boolean success, String destinationPath, List<String> filesCopiati, int tipoCopia) {
        if(destinationPath != null && !destinationPath.isEmpty() && filesCopiati != null && !filesCopiati.isEmpty()) {
            activityMain.showFragment(FragmentFilesExplorer.getInstance(new File(destinationPath)));
        }
    }



    /* FILE MANAGER LISTENERS */


    /**
     * Quando finisce di fare qualsiasi operazione sui files (copia, cancella, rinomina...) viene aggiornato il database del media store
     //al termine dell'aggiornamento ricarica la lista degli elementi
     */
    @Override
    public void onScanCompleted() {
        showProgress(false);
        avviaScansioneAlbums();
    }

    @Override
    public void onFileManagerNewFileFinished(boolean created) {}

    @Override
    public void onFileManagerDeleteFinished(boolean success, List<File> deletedFiles) {}

    @Override
    public void onFileManagerRenameFinished(boolean success, List<File> oldFiles, List<File> newFiles) {
        if(success) {
            showProgress(true); //dopo il multirinomina mostra la progress perchè si attende la fine della scansione del mediastore
        }
        if(filter != null){
            filter.chiudiSearchView();
        }
    }

    @Override
    public void onFileManagerHidePropertyChanged(File file, boolean isHidden) {}













    /* MENU */


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);

        //selezione multipla
        if(adapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_selezione_multipla_el_album, menu);
            if(adapter.numElementiSelezionati() == 1){
                //se è selezionato un solo file aggiungo al menu "apri come"
                inflater.inflate(R.menu.menu_apri_come, menu);
            }
            if(adapter.numElementiSelezionati() == 1 && MediaInfo.filesHasMediaMetadata(adapter.getElementiSelezionati().get(0))){
                //se è selezionato un solo file e sono disponibili le media info
                inflater.inflate(R.menu.menu_media_info, menu);
            }
            if(adapter.numElementiSelezionati() == 1 && widgetManager.isRequestPinAppWidgetSupported()){
                inflater.inflate(R.menu.menu_widget_collegamento, menu);
            }
        }
        inflater.inflate(R.menu.menu_lista_el_album, menu);

        //presentazione solo per le immagini
        if(mediaType != MEDIA_TYPE_IMAGE){
            final MenuItem presentazioneItem = menu.findItem(R.id.presentazione);
            presentazioneItem.setVisible(false);
        }

        //player solo per audio
        if(mediaType != MediaUtils.MEDIA_TYPE_AUDIO){
            final MenuItem playAudioItem = menu.findItem(R.id.aggiungi_a_playlist);
            if(playAudioItem != null) {
                playAudioItem.setVisible(false);
            }
        }

        //filtro
        final MenuItem searchMenuItem = menu.findItem(R.id.filtro);
        searchMenuItem.setVisible(!adapter.modalitaSelezioneMultipla());
        filter = new ListFilter((SearchView) searchMenuItem.getActionView());
        filter.configuraSearchView(adapter);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aggiorna:
                swipeLayoutReciclerView.setRefreshing(true);
                swipeLayoutEmptyView.setRefreshing(true);
                onRefresh();
                return true;
            case R.id.ordina:
                final DialogOrdinaFilesBuilder dialogOrdinaBuilder = new DialogOrdinaFilesBuilder(getContext(), ordinatoreFiles, (dialogInterface, i) -> mostraFiles());
                dialogOrdinaBuilder.create().show();
                return true;
            case R.id.visualizzazione:
                final int visualizzazioneCorrente = visualizzazione.getVisualizzazioneCorrente();
                final DialogVisualizzazioneBuilder dialogVisualizzazioneBuilder = new DialogVisualizzazioneBuilder(getContext(), visualizzazioneCorrente, (dialogInterface, tipoVisualizzazione) -> {
                    disattivaSelezioneMultipla();
                    visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
                    adapter = (DatiFilesLocaliBaseAdapter)recyclerView.getAdapter();
                    setMultiselectableAdapter(adapter);
                    getPrefs().edit().putInt(KEY_PREF_TIPO_VISUALIZZAZIONE, tipoVisualizzazione).apply();
                });
                dialogVisualizzazioneBuilder.create().show();
                return true;
            case R.id.apri_come:
                if(adapter.numElementiSelezionati() == 1 && !adapter.getElementiSelezionati().get(0).isDirectory()) {
                    fileOpener.openFileAs(adapter.getElementiSelezionati().get(0));
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.rinomina:
                if(adapter.numElementiSelezionati() > 0) {
                    final String nomeFile = adapter.getElementiSelezionati().get(0).getName();
                    final DialogNewNameBuilder dialogNewNameBuilder = new DialogNewNameBuilder(getContext(), nomeFile, name -> {
                        fileManager.rinomina(adapter.getElementiSelezionati(), name, rinominaHandler);
                        disattivaSelezioneMultipla();
                    });
                    dialogNewNameBuilder.create().show();
                    final File primoElemento = adapter.getElementiSelezionati().get(0);
                    chiediPermessiScritturaExtSd(primoElemento);
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.elimina:
                if(adapter.numElementiSelezionati() > 0) {
                    fileManager.elimina(adapter.getElementiSelezionati(), eliminaHandler);
                    final File primoElemento = adapter.getElementiSelezionati().get(0);
                    chiediPermessiScritturaExtSd(primoElemento);
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.proprieta:
                if(adapter.numElementiSelezionati() > 0) {
                    fileManager.mostraProprietaCategoria(adapter.getElementiSelezionati());
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.copia_in:
                copiaIn(false);
                return true;
            case R.id.sposta_in:
                copiaIn(true);
                return true;
            case R.id.filtro:
                return true;
            case R.id.condividi:
                if(adapter.numElementiSelezionati() > 0) {
                    fileOpener.shareFiles(adapter.getElementiSelezionati());
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.aggiungi_a_preferiti:
                if(adapter.numElementiSelezionati() > 0) {
                    try {
                        new PreferitiManager(getPrefs()).aggiungiPreferiti(adapter.getElementiSelezionati());
                        ColoredToast.makeText(getContext(), R.string.preferito_aggiunto, Toast.LENGTH_LONG).show();
                    } catch (TroppiElementiException e) {
                        ColoredToast.makeText(getContext(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                    }
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.media_info:
                if(adapter.numElementiSelezionati() > 0) {
                    final File file = adapter.getElementiSelezionati().get(0);
                    final Map<String, String> mapMediaInfo = MediaInfo.getMetadata(getContext(), file);
                    final DialogInfoBuilder dialogInfoBuilder = new DialogInfoBuilder(getContext(), R.string.media_info, mapMediaInfo);
                    dialogInfoBuilder.create().show();
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.presentazione:
                final Intent intentPresentazione = new Intent(getContext(), ActivityImageViewer.class);
                intentPresentazione.putExtra(KEY_BUNDLE_ELEMENTI_PRESENTAZIONE, SerializableFileList.fromPathList(elementi));
                try {
                    startActivity(intentPresentazione);
                } catch (Exception e){
                    ColoredToast.makeText(getContext(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                    e.printStackTrace(); //si potrebbe generare una TransactionTooLargeException se il numero di files è troppo elevato
                }
                return true;
            case R.id.aggiungi_a_playlist:
                final SerializableFileList listaAudio = new SerializableFileList(adapter.getItemCount());
                for(File f : adapter.getElementiSelezionati()){
                    if(FileTypes.getTypeForFile(f) == FileTypes.TYPE_AUDIO){
                        listaAudio.addFile(f);
                    }
                }
                listaAudio.trimToSize();
                if(listaAudio.size() > 0){
                    final Intent intentMusicPlayer = new Intent(getContext(), ActivityMusicPlayer.class);
                    intentMusicPlayer.putExtra(KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE, listaAudio);
                    try {
                        startActivity(intentMusicPlayer);
                    } catch (Exception e){
                        ColoredToast.makeText(getContext(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                        e.printStackTrace(); //si potrebbe generare una TransactionTooLargeException se il numero di files è troppo elevato
                    }
                } else {
                    ColoredToast.makeText(getContext(), R.string.nessun_file_audio, Toast.LENGTH_LONG).show();
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.collegamento_home:
                if(adapter.numElementiSelezionati() == 1 && widgetManager.isRequestPinAppWidgetSupported()) {
                    final File file = adapter.getElementiSelezionati().get(0);
                    widgetManager.addWidgetToHome(file);
                }
                disattivaSelezioneMultipla();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
