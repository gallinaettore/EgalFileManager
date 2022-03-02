package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE;
import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_PRESENTAZIONE;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ORDINA_DOWNLOAD_PER_DATA;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_VISUALIZZAZIONE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.activity.ActivityMusicPlayer;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogContenutoClipboardBuilder;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewFileBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewFolderBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewNameBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaFilesBuilder;
import it.Ettore.egalfilemanager.dialog.DialogVisualizzazioneBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.filemanager.ProprietaTask;
import it.Ettore.egalfilemanager.filemanager.thread.CompressHandler;
import it.Ettore.egalfilemanager.filemanager.thread.CreaCartellaTask;
import it.Ettore.egalfilemanager.filemanager.thread.CreaFileTask;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.filemanager.thread.LsTask;
import it.Ettore.egalfilemanager.filemanager.thread.RinominaHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.Clipboard;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.fileutils.PreferitiManager;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.fileutils.TroppiElementiException;
import it.Ettore.egalfilemanager.ftp.FtpFileManager;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.lan.SmbFileManager;
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.pathbar.FilePathBar;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneFilesLocali;
import it.Ettore.egalfilemanager.widget.MyWidgetManager;


/**
 * Fragment per l'esplorazione dei files
 */
public class FragmentFilesExplorer extends FragmentBaseExplorer implements DatiFilesLocaliBaseAdapter.OnItemTouchListener, FilePathBar.PathClickListener,
        SwipeRefreshLayout.OnRefreshListener, CompressHandler.ZipCompressListener, CopyHandlerListener, EliminaHandler.EliminaListener, RinominaHandler.RinominaListener, LsTask.LsListener,
        CreaCartellaTask.CreaCartellaListener, CreaFileTask.CreaFileListener, ProprietaTask.ProprietaNascondiListener {

    private static final String KEY_BUNDLE_START_DIRECTORY = "start_directory";
    private static final String KEY_BUNDLE_START_DIRECTORY_NAME = "start_directory_name";
    private static final String KEY_BUNDLE_CURRENT_DIRECTORY = "current_directory";
    private static final String KEY_BUNDLE_TITOLO = "titolo";

    private ActivityMain activityMain;
    private String titolo, startDirectoryName;
    private SwipeRefreshLayout swipeLayoutReciclerView, swipeLayoutEmptyView;
    private DatiFilesLocaliBaseAdapter adapter;
    private FileManager fileManager;
    private FilePathBar pathBar;
    private FileOpener fileOpener;
    private RecyclerView recyclerView;
    private File startDirectory, currentDirectory;
    private FloatingActionButton fab;
    private VisualizzazioneFilesLocali visualizzazione;
    private LinearLayout progressLayout, resultLayout;
    private ListFilter filter;
    private OrdinatoreFiles ordinatoreFiles;
    private MyWidgetManager widgetManager;
    private CopyHandler copyHandler;
    private CompressHandler compressHandler;
    private EliminaHandler eliminaHandler;
    private RinominaHandler rinominaHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentFilesExplorer(){}



    /**
     * Metodo factory per creare un'istanza del fragment
     * @param titolo Titolo da mostrare sull'action bar
     * @param startDirectory Directory da cui iniziare la navigazione
     * @param startDirectoryName Nome della directory da cui iniziare la navigazione
     * @param currentDirectory Directory da esplorare
     * @return Istanza del fragment
     */
    public static FragmentFilesExplorer getInstance(@NonNull String titolo, File startDirectory, String startDirectoryName, File currentDirectory){
        final FragmentFilesExplorer fragment = new FragmentFilesExplorer();
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE_TITOLO, titolo);
        if(startDirectory != null) {
            bundle.putString(KEY_BUNDLE_START_DIRECTORY, startDirectory.getAbsolutePath());
        }
        if(startDirectoryName != null) {
            bundle.putString(KEY_BUNDLE_START_DIRECTORY_NAME, startDirectoryName);
        }
        if(currentDirectory != null) {
            bundle.putString(KEY_BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
        }
        fragment.setArguments(bundle);
        return fragment;
    }


    /**
     * Metodo factory per creare un'istanza del fragment. Da usare quando della directory da esplorare non si conosce lo storage di appartenenza.
     * La start directory sarà cercata automaticamente (ma il metodo risulta un più lento).
     * @param currentDirectory Directory da esplorare
     * @return Istanza del fragment
     */
    public static FragmentFilesExplorer getInstance(File currentDirectory){
        final FragmentFilesExplorer fragment = new FragmentFilesExplorer();
        final Bundle bundle = new Bundle();
        if(currentDirectory != null) {
            bundle.putString(KEY_BUNDLE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
        }
        fragment.setArguments(bundle);
        return fragment;
    }





    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        final Bundle args = getArguments();
        titolo = getString(R.string.archivio_locale);
        this.currentDirectory = new File(args.getString(KEY_BUNDLE_CURRENT_DIRECTORY, "/"));
        if(args.getString(KEY_BUNDLE_START_DIRECTORY) != null){
            //il bundle contiene le informazioni complete per la configurazione della path bar
            this.startDirectory = new File(args.getString(KEY_BUNDLE_START_DIRECTORY, "/"));
            this.startDirectoryName = args.getString(KEY_BUNDLE_START_DIRECTORY_NAME, startDirectory.getName());
        } else {
            //ottengo le info per la path bar automaticamente dal percorso della cartella passata
            final StoragesUtils storagesUtils = new StoragesUtils(getContext());
            if (storagesUtils.isOnInternalSdCard(currentDirectory)){
                this.startDirectory = storagesUtils.getInternalStorage();
                this.startDirectoryName = getString(R.string.memoria_interna);
            } else if(storagesUtils.isOnExtSdCard(currentDirectory)){
                final File extStorage = storagesUtils.getExtStorageForFile(currentDirectory);
                if(extStorage != null){
                    this.startDirectory = extStorage;
                    this.startDirectoryName = getString(R.string.memoria_esterna);
                } else {
                    this.startDirectory = new File("/");
                    this.startDirectoryName = "/";
                }
            } else {
                this.startDirectory = new File("/");
                this.startDirectoryName = "/";
            }
        }
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        compressHandler = new CompressHandler(getActivity(), this);
        eliminaHandler = new EliminaHandler(getActivity(), this);
        rinominaHandler = new RinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        activityMain = (ActivityMain)getActivity();
        setActivityMain(activityMain);
        setTitle(titolo);

        swipeLayoutReciclerView = v.findViewById(R.id.swipe_layout_recyclerview);
        swipeLayoutReciclerView.setOnRefreshListener(this);
        swipeLayoutReciclerView.setColorSchemeResources(R.color.colorAccent);
        swipeLayoutEmptyView = v.findViewById(R.id.swipe_layout_emptyview);
        swipeLayoutEmptyView.setOnRefreshListener(this);
        swipeLayoutEmptyView.setColorSchemeResources(R.color.colorAccent);
        progressLayout = v.findViewById(R.id.progress_layout);
        resultLayout = v.findViewById(R.id.result_layout);

        //visualizzazione
        recyclerView = v.findViewById(R.id.recycler_view);
        int tipoVisualizzazione = getPrefs().getInt(KEY_PREF_TIPO_VISUALIZZAZIONE, VisualizzazioneBase.VISUALIZZAZIONE_LISTA);
        visualizzazione = new VisualizzazioneFilesLocali(recyclerView, this);
        visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
        adapter = (DatiFilesLocaliBaseAdapter)recyclerView.getAdapter();
        setMultiselectableAdapter(adapter);

        //pathbar
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        pathBar = new FilePathBar(pathView, this);
        pathBar.setStartFolderName(startDirectory, startDirectoryName);

        fileManager = new FileManager(getContext(), getPrefs());
        ordinatoreFiles = new OrdinatoreFiles(getPrefs());
        fileOpener = new FileOpener(getContext());
        widgetManager = new MyWidgetManager(getContext());

        //fab
        this.fab = v.findViewById(R.id.fab);
        fab.bringToFront(); //compatibilità pre-lollipop
        fab.setOnClickListener(view -> incolla());

        //esco dalla modalità selezione multipla premendo il tasto indietro
        configuraBackButton(v);

        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
        new ChiediTreeUriTask(activityMain, currentDirectory).execute();

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();

        if(!activityMain.getPermissionsManager().hasPermissions()){
            activityMain.getPermissionsManager().requestPermissions();
            activityMain.finishCurrentFragment();
            return;
        }

        fileManager.ottieniStatoRootExplorer();
        ordinatoreFiles.ottieniStatoMostraNascosti();
        if(getPrefs().getBoolean(KEY_PREF_ORDINA_DOWNLOAD_PER_DATA, true) && Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).equals(currentDirectory)){
            ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.DATA);
            ordinatoreFiles.setTipoOrdinamento(OrdinatoreFiles.TipoOrdinamento.DESCRESCENTE);
        } else {
            ordinatoreFiles.ottieniStatoOrdinamento();
        }

        if(activityMain.getClipboard().isEmpty()){
            fab.hide();
        } else {
            fab.show();
        }
        disattivaSelezioneMultipla();

        ls(currentDirectory);
    }


    private void ls(File directory){
        progressLayout.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        fileManager.ls(directory, this);
    }


    @Override
    public void onStop(){
        super.onStop();
        if(filter != null){
            filter.chiudiSearchView();
        }
        if(!Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).equals(currentDirectory)) {
            ordinatoreFiles.salvaStatoOrdinamento();
        }
    }


    @Override
    public void onDestroy(){
        if(copyHandler != null) copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        if(compressHandler != null) compressHandler.dismissProgressDialogOnDestroy();
        if(eliminaHandler != null) eliminaHandler.dismissProgressDialogOnDestroy();
        if(rinominaHandler != null) rinominaHandler.dismissProgressDialogOnDestroy();
        super.onDestroy();
    }


    /**
     * Al click sull'elemento avvia la navigazione se è una cartella o apre il file se è un file
     * @param file File o cartella da gestire
     */
    @Override
    public void onItemClick(File file) {
        if(!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            if (file.isDirectory()) {
                activityMain.showFragment(FragmentFilesExplorer.getInstance(titolo, startDirectory, startDirectoryName, file));
            } else {
                fileOpener.openFile(file);
            }
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


    /**
     * Al click di un item sull path bar
     * @param file Directory scelta
     */
    @Override
    public void onPathItemClick(File file) {
        currentDirectory = file;
        if(activityMain.getClipboard().isEmpty()){
            fab.hide();
        } else {
            fab.show();
        }
        disattivaSelezioneMultipla();
        ls(file);
    }


    @Override
    public void onRefresh() {
        ls(currentDirectory);
        swipeLayoutReciclerView.setRefreshing(false);
        swipeLayoutEmptyView.setRefreshing(false);
    }



    /**
     * Avvia la funzione incolla se la clipboard non è vuota
     */
    private void incolla(){
        final Clipboard clipboard = activityMain.getClipboard();
        if(clipboard.isEmpty()) return;
        switch (clipboard.getTipoFile()){
            case Clipboard.TIPOFILE_LOCALE:
                if(clipboard.isCutMode()){
                    fileManager.sposta(clipboard.getListaFiles(), currentDirectory, copyHandler);
                } else {
                    fileManager.copia(clipboard.getListaFiles(), currentDirectory, copyHandler);
                }
                break;
            case Clipboard.TIPOFILE_SMB:
                final SmbFileManager smbFileManager = new SmbFileManager(getActivity(), clipboard.getSmbUser(), clipboard.getSmbPassword());
                smbFileManager.download(clipboard.getListaSmbFiles(), currentDirectory, copyHandler, null);
                break;
            case Clipboard.TIPOFILE_FTP:
                final FtpFileManager ftpFileManager = new FtpFileManager(getActivity(), activityMain.getFtpSession());
                ftpFileManager.download(clipboard.getListaFtpFiles(), currentDirectory, copyHandler, null);
                break;
        }
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
        final Clipboard clipboard = activityMain.getClipboard();
        clipboard.clear();
        activityMain.invalidateOptionsMenu();
        fab.hide();
        ls(currentDirectory);
    }



    /* FILE MANAGER LISTENERS */


    /**
     * Al termine della scansione della cartellavisualizza il suo contenuto
     * @param directory Cartella scansionata
     * @param listaFiles Lista di files o directory al suo interno
     */
    @Override
    public void onFileManagerLsFinished(File directory, List<File> listaFiles){
        progressLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        final List<File> listaFilesOrdinata = ordinatoreFiles.ordinaListaFiles(listaFiles);
        try{
            if(listaFilesOrdinata == null || listaFilesOrdinata.isEmpty()){
                swipeLayoutReciclerView.setVisibility(View.GONE);
                swipeLayoutEmptyView.setVisibility(View.VISIBLE);
            } else {
                swipeLayoutReciclerView.setVisibility(View.VISIBLE);
                swipeLayoutEmptyView.setVisibility(View.GONE);
            }
            boolean aggiornato = adapter.update(listaFilesOrdinata);
            if(aggiornato) {
                recyclerView.scrollToPosition(0);
            }

            //dopo l'aggiornamento disattivo la modalità filtro se attivata
            adapter.setFilterMode(false);
            if(filter != null){
                filter.chiudiSearchView();
            }

            pathBar.visualizzaPath(directory);
            currentDirectory = directory;
            mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
        } catch (Exception ignored){}
    }


    /**
     * Se la cartella è stata creata aggiornaFilesLocali la lista elementi per visualizzarla
     * @param created True se la cartella è stata creata o è già esistente.
     */
    @Override
    public void onFileManagerNewFolderFinished(boolean created){
        if(created) {
            ls(currentDirectory);
        }
    }


    /**
     * Se il file è stato creato aggiornaFilesLocali la lista elementi per visualizzarlo
     * @param created True se il file è stato creato o è già esistente.
     */
    @Override
    public void onFileManagerNewFileFinished(boolean created) {
        if(created) {
            ls(currentDirectory);
        }
    }




    /**
     * Al termine della cancellazione aggiornaFilesLocali la lista files per rimuovere i files cancellati
     * @param success True se tutti i files sono stati cancellati
     */
    @Override
    public void onFileManagerDeleteFinished(boolean success, List<File> deletedFiles){
        //deleted è false anche quando l'operazione è stata annullata (ma qualche file è stato cancellato)
        ls(currentDirectory);
    }


    /**
     * Al termine della rinominazione aggiornaFilesLocali la lista files per mostrare i nuovi nomi
     * @param success True se tutti i files sono stati rinominati con successo
     */
    @Override
    public void onFileManagerRenameFinished(boolean success, List<File> oldFiles, List<File> newFiles){
        ls(currentDirectory);
    }


    /**
     * Mostra o nasconde un file se la sua proprietà visibilità è stata modificata
     * @param file File modificato
     * @param isHidden True se nascosto, False se visibile
     */
    @Override
    public void onFileManagerHidePropertyChanged(File file, boolean isHidden){
        //se la proprietà mostra/nascondi è cambiata
        fileManager.nascondiFile(file, isHidden, rinominaHandler);
    }


    /**
     * Al termine della compressione viene mostrato il fragment che visualizza la cartella decompressa
     * @param success True se la compressione è avvenuta con successo
     * @param destinationFile File zip creato
     */
    @Override
    public void onZipCompressFinished(boolean success, File destinationFile){
        if(success){
            activityMain.showFragment(FragmentFilesExplorer.getInstance(destinationFile.getParentFile()));
        }
    }


    /**
     * Dopo aver visualizzato le dialog di richiesta destinazione, avvia la compressione dei files
     * @param elementiSelezionati Lista di files da comprime
     */
    private void comprimi(final List<File> elementiSelezionati){
        final List<File> elementiDaComprimere = new ArrayList<>(elementiSelezionati);
        final String zipName;
        if(adapter.numElementiSelezionati() == 1){
            //se c'è un solo elemento il zip avrà lo stesso nome dell'elemento
            zipName = FileUtils.getFileNameWithoutExt(adapter.getElementiSelezionati().get(0).getName()) + ".zip";
        } else {
            //se sono stati selezionati più elementi il zip avrà il nome della cartella genitore
            zipName = adapter.getElementiSelezionati().get(0).getParentFile().getName() + ".zip";
        }
        //dialog per la scelta dello storage
        final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
        builder.setTitle(R.string.seleziona_destinazione);
        builder.hideIcon(true);
        builder.setStorageItems(new HomeNavigationManager(getActivity()).listaItemsArchivioLocale());
        builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
            @Override
            public void onSelectStorage(File storagePath) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(getContext(), DialogFileChooserBuilder.TYPE_SAVE_FILE);
                fileChooser.setTitle(R.string.seleziona_destinazione);
                fileChooser.setStartFolder(storagePath);
                fileChooser.setFileName(zipName);
                fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                    @Override
                    public void onFileChooserSelected(final File selected) {
                        //dopo aver scelto la destinazione, verifico che il file esiste (in caso notifico) e poi effettuo la compressione
                        final FileManager fileManager = new FileManager(getContext());
                        if(!FileUtils.fileNameIsValid(selected.getName())){
                            CustomDialogBuilder.make(getContext(), R.string.nome_non_valido, CustomDialogBuilder.TYPE_ERROR).show();
                        } else if (fileManager.fileExists(selected)){
                            final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                            builder.setType(CustomDialogBuilder.TYPE_WARNING);
                            builder.setMessage(getString(R.string.sovrascrivi_file, selected.getName()));
                            builder.setPositiveButton(R.string.sovrascrivi, (dialogInterface, i) -> {
                                fileManager.comprimiFiles(elementiDaComprimere, selected, compressHandler);
                            });
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.create().show();
                        } else {
                            fileManager.comprimiFiles(elementiDaComprimere, selected, compressHandler);
                        }
                    }

                    @Override
                    public void onFileChooserCanceled() {}
                });
                fileChooser.create().show();

                //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                new ChiediTreeUriTask(activityMain, storagePath, true).execute();
            }

            @Override
            public void onCancelStorageSelection() {}
        });
        builder.showSelectDialogIfNecessary();
    }






    /* MENU */


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);

        //selezione multipla
        if(adapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_selezione_multipla_locali, menu);
            if(adapter.numElementiSelezionati() == 1 && !adapter.getElementiSelezionati().get(0).isDirectory()){
                //se è selezionato un solo file (non directory), aggiungo al menu "apri come"
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
        inflater.inflate(R.menu.menu_lista_files, menu);

        //clipboard
        if(activityMain.getClipboard().isEmpty()){
            final MenuItem itemIncolla = menu.findItem(R.id.incolla);
            itemIncolla.setVisible(false);
        }
        final MenuItem itemMostraClipbard = menu.findItem(R.id.mostra_clipboard);
        itemMostraClipbard.setVisible(!activityMain.getClipboard().isEmpty());

        //filtro
        final MenuItem searchItem = menu.findItem(R.id.filtro);
        searchItem.setVisible(!adapter.modalitaSelezioneMultipla());
        filter = new ListFilter((SearchView) searchItem.getActionView());
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
                final DialogOrdinaFilesBuilder dialogOrdinaBuilder = new DialogOrdinaFilesBuilder(getContext(), ordinatoreFiles, (dialogInterface, i) -> ls(currentDirectory));
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
            case R.id.nuova_cartella:
                final DialogNewFolderBuilder dialogNewFolderBuilder = new DialogNewFolderBuilder(getContext(), name -> fileManager.creaCartella(currentDirectory, name, FragmentFilesExplorer.this));
                dialogNewFolderBuilder.create().show();
                return true;
            case R.id.nuovo_file:
                final DialogNewFileBuilder dialogNewFileBuilder = new DialogNewFileBuilder(getContext(), name -> fileManager.creaFile(currentDirectory, name, FragmentFilesExplorer.this));
                dialogNewFileBuilder.create().show();
                return true;
            case R.id.taglia:
                if(adapter.numElementiSelezionati() > 0) {
                    activityMain.getClipboard().setCutMode(true);
                }
                //non c'è return perchè per il resto utilizza il codice di "copia"
            case R.id.copia:
                if(adapter.numElementiSelezionati() > 0) {
                    ColoredToast.makeText(getContext(), String.format(getString(R.string.elementi_negli_appunti), String.valueOf(adapter.numElementiSelezionati())), Toast.LENGTH_LONG).show();
                    activityMain.getClipboard().aggiornaFilesLocali(adapter.getElementiSelezionati());
                    fab.show();
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.incolla:
                incolla();
                return true;
            case R.id.rinomina:
                if(adapter.numElementiSelezionati() > 0) {
                    final String nomeFile = adapter.getElementiSelezionati().get(0).getName();
                    final DialogNewNameBuilder dialogNewNameBuilder = new DialogNewNameBuilder(getContext(), nomeFile, name -> {
                        fileManager.rinomina(adapter.getElementiSelezionati(), name, rinominaHandler);
                        disattivaSelezioneMultipla(); //disattivo solo dopo aver mostrato la dialog altrimenti gli elementi selezionati vengono eliminati prima che l'utente inserisce il nome cartella
                    });
                    dialogNewNameBuilder.create().show();
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.elimina:
                if(adapter.numElementiSelezionati() > 0) {
                    fileManager.elimina(adapter.getElementiSelezionati(), eliminaHandler);
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.proprieta:
                if(adapter.numElementiSelezionati() > 0) {
                    fileManager.mostraProprieta(adapter.getElementiSelezionati(), this);
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.compressione:
                if(adapter.numElementiSelezionati() > 0) {
                    comprimi(adapter.getElementiSelezionati());
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.mostra_clipboard:
                final DialogContenutoClipboardBuilder clipboardBuilder = new DialogContenutoClipboardBuilder(getContext(), activityMain.getClipboard(), () -> {
                    fab.hide();
                    activityMain.invalidateOptionsMenu();
                });
                clipboardBuilder.create().show();
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
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.presentazione:
                final SerializableFileList listaImmagini = new SerializableFileList(adapter.getItemCount());
                for(File f : adapter.getListaFiles()){
                    if(FileTypes.getTypeForFile(f) == FileTypes.TYPE_IMMAGINE){
                        listaImmagini.addFile(f);
                    }
                }
                listaImmagini.trimToSize();
                if(listaImmagini.size() > 0){
                    final Intent intentPresentazione = new Intent(getContext(), ActivityImageViewer.class);
                    intentPresentazione.putExtra(KEY_BUNDLE_ELEMENTI_PRESENTAZIONE, listaImmagini);
                    try {
                        startActivity(intentPresentazione);
                    } catch (Exception e){
                        ColoredToast.makeText(getContext(), R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
                        e.printStackTrace(); //si potrebbe generare una TransactionTooLargeException se il numero di files è troppo elevato
                    }
                } else {
                    ColoredToast.makeText(getContext(), R.string.nessuna_immagine, Toast.LENGTH_LONG).show();
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
