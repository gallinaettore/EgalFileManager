package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
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
import java.util.Arrays;
import java.util.List;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogContenutoClipboardBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewFileBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewFolderBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewNameBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaFilesBuilder;
import it.Ettore.egalfilemanager.dialog.DialogVisualizzazioneBuilder;
import it.Ettore.egalfilemanager.filemanager.thread.CompressHandler;
import it.Ettore.egalfilemanager.fileutils.Clipboard;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.lan.OrdinatoreFilesLan;
import it.Ettore.egalfilemanager.lan.SmbFileManager;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import it.Ettore.egalfilemanager.lan.thread.SmbCreaCartellaTask;
import it.Ettore.egalfilemanager.lan.thread.SmbCreaFileTask;
import it.Ettore.egalfilemanager.lan.thread.SmbEliminaHandler;
import it.Ettore.egalfilemanager.lan.thread.SmbLsTask;
import it.Ettore.egalfilemanager.lan.thread.SmbRinominaHandler;
import it.Ettore.egalfilemanager.pathbar.LanPathBar;
import it.Ettore.egalfilemanager.recycler.DatiFilesLanBaseAdapter;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneFilesLan;
import jcifs.smb.SmbFile;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_VISUALIZZAZIONE;


/**
 * Fragment per l'esplorazione dei files
 */
public class FragmentLanExplorer extends FragmentBaseExplorer implements
        DatiFilesLanBaseAdapter.OnItemTouchListener,
        LanPathBar.PathClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        CompressHandler.ZipCompressListener,
        SmbLsTask.SmbLsListener,
        SmbCreaCartellaTask.SmbNuovaCartellaListener,
        SmbCreaFileTask.SmbNuovoFileListener,
        CopyHandlerListener,
        SmbEliminaHandler.SmbEliminaListener,
        SmbRinominaHandler.SmbRinominaListener {

    private static final String KEY_BUNDLE_PATH = "path";
    private static final String KEY_BUNDLE_USER = "user";
    private static final String KEY_BUNDLE_PASSWORD = "password";
    private ActivityMain activityMain;
    private SwipeRefreshLayout swipeLayoutReciclerView, swipeLayoutEmptyView;
    private DatiFilesLanBaseAdapter adapter;
    private SmbFileManager smbFileManager;
    private LanPathBar pathBar;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private VisualizzazioneBase visualizzazione;
    private LinearLayout progressLayout, resultLayout;
    private String path, user, password;
    private OrdinatoreFilesLan ordinatoreFiles;
    private ListFilter filter;

    private CopyHandler copyHandler;
    private SmbEliminaHandler eliminaHandler;
    private SmbRinominaHandler rinominaHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentLanExplorer(){}


    public static FragmentLanExplorer getInstance(@NonNull String path, String user, String password){
        final FragmentLanExplorer fragment = new FragmentLanExplorer();
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE_PATH, path);
        if(user != null){
            bundle.putString(KEY_BUNDLE_USER, user);
        }
        if(password != null){
            bundle.putString(KEY_BUNDLE_PASSWORD, password);
        }
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        path = getArguments().getString(KEY_BUNDLE_PATH);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        eliminaHandler = new SmbEliminaHandler(getActivity(), this);
        rinominaHandler = new SmbRinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        activityMain = (ActivityMain)getActivity();
        setActivityMain(activityMain);
        setTitle(R.string.rete_locale);

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
        visualizzazione = new VisualizzazioneFilesLan(recyclerView, this);
        visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
        adapter = (DatiFilesLanBaseAdapter)recyclerView.getAdapter();
        setMultiselectableAdapter(adapter);

        //pathbar
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        pathBar = new LanPathBar(pathView, this);

        ordinatoreFiles = new OrdinatoreFilesLan(getPrefs());
        user = getArguments().getString(KEY_BUNDLE_USER);
        password = getArguments().getString(KEY_BUNDLE_PASSWORD);
        smbFileManager = new SmbFileManager(getActivity(), user, password);

        //fab
        this.fab = v.findViewById(R.id.fab);
        fab.bringToFront(); //compatibilità pre-lollipop
        fab.setOnClickListener(view -> incolla());

        //esco dalla modalità selezione multipla premendo il tasto indietro
        configuraBackButton(v);

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();

        ordinatoreFiles.ottieniStatoOrdinamento();
        ordinatoreFiles.ottieniStatoMostraNascosti();

        if(activityMain.getClipboard().isEmpty()){
            fab.hide();
        } else {
            fab.show();
        }
        disattivaSelezioneMultipla();

        ls();


    }


    private void ls(){
        progressLayout.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
        smbFileManager.ls(path, this);
    }



    @Override
    public void onStop(){
        super.onStop();
        if(filter != null) {
            filter.chiudiSearchView();
        }
        ordinatoreFiles.salvaStatoOrdinamento();
    }


    @Override
    public void onDestroy(){
        if(copyHandler != null) copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        if(eliminaHandler != null) eliminaHandler.dismissProgressDialogOnDestroy();
        super.onDestroy();
    }



    /**
     * Al click sull'elemento avvia la navigazione se è una cartella o apre il file se è un file
     * @param file File o cartella da gestire
     */
    @Override
    public void onItemClick(final SmbFile file) {
        if(!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            if (SmbFileUtils.isDirectory(file)) {
                final String user = getArguments().getString(KEY_BUNDLE_USER, null);
                final String password = getArguments().getString(KEY_BUNDLE_PASSWORD, null);
                activityMain.showFragment(FragmentLanExplorer.getInstance(file.getPath(), user, password));
            } else {
                final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                builder.setMessage(R.string.scaricare_il_file);
                builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    final File cartellaDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                    final List<SmbFile> daScaricare = new ArrayList<>(1);
                    daScaricare.add(file);
                    smbFileManager.download(daScaricare, cartellaDownload, copyHandler, null);
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
            }
        } else {
            //modalità selezione multipla
            mostraNumeroElementiSelezionati(true);
        }
    }


    @Override
    public void onItemLongClick(SmbFile file) {
        //dopo aver attivato la selezione multipla
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    /**
     * Al click di un item sull path bar
     * @param filePath Directory scelta
     */
    @Override
    public void onPathItemClick(String filePath) {
        this.path = filePath;
        if(activityMain.getClipboard().isEmpty()){
            fab.hide();
        } else {
            fab.show();
        }
        disattivaSelezioneMultipla();
        ls();
    }


    @Override
    public void onRefresh() {
        ls();
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
            case Clipboard.TIPOFILE_SMB:
                final List<SmbFile> filesDaIncollare = clipboard.getListaSmbFiles();
                smbFileManager.copia(new ArrayList<>(filesDaIncollare), clipboard.getSmbUser(), clipboard.getSmbPassword(), path, copyHandler);
                break;
            case Clipboard.TIPOFILE_LOCALE:
                smbFileManager.upload(new ArrayList<>(clipboard.getListaFiles()), path, clipboard.isCutMode(), copyHandler);
                break;
            case Clipboard.TIPOFILE_FTP:
                CustomDialogBuilder.make(getContext(), R.string.no_ftp_to_smb, CustomDialogBuilder.TYPE_WARNING).show();
                break;
        }
    }





    /* FILE MANAGER LISTENERS */


    /**
     * Al termine della scansione della cartella visualizza il suo contenuto
     * @param directoryPath Cartella scansionata
     * @param files Lista di files o directory al suo interno
     */
    @Override
    public void onSmbLsFinished(String directoryPath, SmbFile[] files){
        progressLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        if(files == null){
            swipeLayoutReciclerView.setVisibility(View.GONE);
            swipeLayoutEmptyView.setVisibility(View.VISIBLE);
        } else {
            swipeLayoutReciclerView.setVisibility(View.VISIBLE);
            swipeLayoutEmptyView.setVisibility(View.GONE);
            final List<SmbFile> listaFile = new ArrayList<>(Arrays.asList(files));
            boolean aggiornato = adapter.update(ordinatoreFiles.ordinaListaFiles(listaFile));
            if(aggiornato) {
                recyclerView.scrollToPosition(0);
            }
        }

        //dopo l'aggiornamento disattivo la modalità filtro se attivata
        adapter.setFilterMode(false);
        if(filter != null) {
            filter.chiudiSearchView();
        }

        pathBar.visualizzaPath(directoryPath);
        this.path = directoryPath;
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    /**
     * Se la cartella è stata creata aggiornaFilesLocali la lista elementi per visualizzarla
     * @param created True se la cartella è stata creata o è già esistente.
     */
    @Override
    public void onSmbNewFolderFinished(boolean created){
        if(created) {
            ls();
        }
    }


    /**
     * Se il file è stato creato aggiornaFilesLocali la lista elementi per visualizzarlo
     * @param created True se il file è stato creato o è già esistente.
     */
    @Override
    public void onSmbNewFileFinished(boolean created) {
        if(created) {
            ls();
        }
    }


    /**
     * Al termine della cancellazione aggiornaFilesLocali la lista files per rimuovere i files cancellati
     * @param success True se tutti i files sono stati cancellati
     * @param deletedFiles Lista files cancellati
     */
    @Override
    public void onSmbDeleteFinished(boolean success, List<SmbFile> deletedFiles){
        //deleted è false anche quando l'operazione è stata annullata (ma qualche file è stato cancellato)
        ls();
    }


    /**
     * Al termine della rinominazione aggiornaFilesLocali la lista files per mostrare i nuovi nomi
     * @param success True se tutti i files sono stati rinominati con successo
     * @param oldFiles Files non più presenti (perchè sono stati rinominati e hanno un path diverso)
     * @param newFiles Nuovi files (files con il nuovo nome e quindi path diverso
     */
    @Override
    public void onSmbRenameFinished(boolean success, List<SmbFile> oldFiles, List<SmbFile> newFiles){
        ls();
    }



    /**
     * Chiamato quando il service termina la copia
     * @param success True se la copia è andata a buon fine
     * @param destinationPath Cartella in cui sono stati copiati i files
     * @param filesCopiati Lista con i path dei files copiati correttamente
     * @param tipoCopia Una della variabili COPY della classe CopyService (specifica se la copia è avvenuta ad esempio da smb a locale)
     */
    @Override
    public void onCopyServiceFinished(boolean success, final String destinationPath, final List<String> filesCopiati, int tipoCopia) {
        switch (tipoCopia){
            case CopyService.COPY_SMB_TO_LOCAL:
                //quando l'utente ha scaricato il file in locale
                if(!filesCopiati.isEmpty()){
                    final CustomDialogBuilder dBuilder = new CustomDialogBuilder(getContext());
                    dBuilder.setMessage(getString(R.string.file_scaricato_nella_cartella, destinationPath));
                    dBuilder.setPositiveButton(R.string.apri_file, (dialogInterface, i) -> {
                        final File cartellaDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                        final String fileName = SmbFileUtils.getNameFromPath(filesCopiati.get(0));
                        final File fileScaricato = new File(cartellaDownload, fileName);
                        final FileOpener fileOpener = new FileOpener(getContext());
                        fileOpener.openFile(fileScaricato);
                    });
                    dBuilder.setNegativeButton(R.string.apri_cartella_destinazione, (dialogInterface, i) -> {
                        if(destinationPath != null && !destinationPath.isEmpty()) {
                            activityMain.showFragment(FragmentFilesExplorer.getInstance(new File(destinationPath)));
                        }
                    });
                    dBuilder.setNeutralButton(android.R.string.cancel, null);
                    dBuilder.create().show();
                }
                break;
            case CopyService.COPY_LOCAL_TO_SMB:
            case CopyService.COPY_SMB_TO_SMB:
                //quando l'utente ha copiato su percorsi smb
                final Clipboard clipboard = activityMain.getClipboard();
                clipboard.clear();
                activityMain.invalidateOptionsMenu();
                fab.hide();
                ls();
                break;
        }

    }


    @Override
    public void onZipCompressFinished(boolean success, File destinationFile){}







    /* MENU */


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);

        //selezione multipla
        if(adapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_selezione_multipla_lan, menu);
        }
        inflater.inflate(R.menu.menu_files_lan, menu);

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
        this.filter = new ListFilter((SearchView) searchItem.getActionView());
        this.filter.configuraSearchView(adapter);
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
                final DialogOrdinaFilesBuilder dialogOrdinaBuilder = new DialogOrdinaFilesBuilder(getContext(), ordinatoreFiles, (dialogInterface, i) -> ls());
                dialogOrdinaBuilder.create().show();
                return true;
            case R.id.visualizzazione:
                final int visualizzazioneCorrente = visualizzazione.getVisualizzazioneCorrente();
                final DialogVisualizzazioneBuilder dialogVisualizzazioneBuilder = new DialogVisualizzazioneBuilder(getContext(), visualizzazioneCorrente, (dialogInterface, tipoVisualizzazione) -> {
                    disattivaSelezioneMultipla();
                    visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
                    ls(); //richiamo ls perchè la visualizzazione non aggiorna l'adapter
                    adapter = (DatiFilesLanBaseAdapter)recyclerView.getAdapter();
                    setMultiselectableAdapter(adapter);
                    getPrefs().edit().putInt(KEY_PREF_TIPO_VISUALIZZAZIONE, tipoVisualizzazione).apply();
                });
                dialogVisualizzazioneBuilder.nascondiVisualizzazioneAnteprima();
                dialogVisualizzazioneBuilder.create().show();
                return true;
            case R.id.nuova_cartella:
                final DialogNewFolderBuilder dialogNewFolderBuilder = new DialogNewFolderBuilder(getContext(), name -> smbFileManager.creaCartella(path, name, FragmentLanExplorer.this));
                dialogNewFolderBuilder.create().show();
                return true;
            case R.id.nuovo_file:
                final DialogNewFileBuilder dialogNewFileBuilder = new DialogNewFileBuilder(getContext(), name -> smbFileManager.creaFile(path, name, FragmentLanExplorer.this));
                dialogNewFileBuilder.create().show();
                return true;
            case R.id.copia:
                if(adapter.numElementiSelezionati() > 0) {
                    ColoredToast.makeText(getContext(), String.format(getString(R.string.elementi_negli_appunti), String.valueOf(adapter.numElementiSelezionati())), Toast.LENGTH_LONG).show();
                    activityMain.getClipboard().aggiornaFilesSmb(adapter.getElementiSelezionati(), user, password);
                    fab.show();
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.incolla:
                incolla();
                return true;
            case R.id.rinomina:
                if(adapter.numElementiSelezionati() > 0) {
                    String nomeFile = adapter.getElementiSelezionati().get(0).getName();
                    if (nomeFile.endsWith("/")) {
                        nomeFile = nomeFile.replace("/", ""); //tolgo lo slash finale se è una cartella
                    }
                    final DialogNewNameBuilder dialogNewNameBuilder = new DialogNewNameBuilder(getContext(), nomeFile, name -> {
                        smbFileManager.rinomina(adapter.getElementiSelezionati(), name, rinominaHandler);
                        disattivaSelezioneMultipla();
                    });
                    dialogNewNameBuilder.create().show();
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.elimina:
                if(adapter.numElementiSelezionati() > 0) {
                    smbFileManager.elimina(adapter.getElementiSelezionati(), eliminaHandler);
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.proprieta:
                if(adapter.numElementiSelezionati() > 0) {
                    smbFileManager.mostraProprieta(adapter.getElementiSelezionati());
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
