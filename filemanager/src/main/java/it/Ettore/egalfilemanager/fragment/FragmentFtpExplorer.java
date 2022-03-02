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
import it.Ettore.egalfilemanager.dialog.DialogNewFolderBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewNameBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaFilesBuilder;
import it.Ettore.egalfilemanager.dialog.DialogVisualizzazioneBuilder;
import it.Ettore.egalfilemanager.filemanager.thread.CompressHandler;
import it.Ettore.egalfilemanager.fileutils.Clipboard;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileManager;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.OrdinatoreFilesFtp;
import it.Ettore.egalfilemanager.ftp.ServerFtp;
import it.Ettore.egalfilemanager.ftp.thread.FtpCreaCartellaTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpEliminaHandler;
import it.Ettore.egalfilemanager.ftp.thread.FtpLsTask;
import it.Ettore.egalfilemanager.ftp.thread.FtpRinominaHandler;
import it.Ettore.egalfilemanager.pathbar.FtpPathBar;
import it.Ettore.egalfilemanager.recycler.DatiFilesFtpBaseAdapter;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneFilesFtp;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_VISUALIZZAZIONE;


/**
 * Fragment per l'esplorazione dei files
 */
public class FragmentFtpExplorer extends FragmentBaseExplorer implements
        DatiFilesFtpBaseAdapter.OnItemTouchListener,
        FtpPathBar.PathClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        CompressHandler.ZipCompressListener,
        FtpLsTask.FtpLsListener,
        FtpCreaCartellaTask.FtpNuovaCartellaListener,
        CopyHandlerListener,
        FtpEliminaHandler.FtpEliminaListener,
        FtpRinominaHandler.FtpRinominaListener {

    private static final String KEY_BUNDLE_PATH = "path";
    private ActivityMain activityMain;
    private SwipeRefreshLayout swipeLayoutReciclerView, swipeLayoutEmptyView;
    private DatiFilesFtpBaseAdapter adapter;
    private FtpPathBar pathBar;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private VisualizzazioneBase visualizzazione;
    private LinearLayout progressLayout, resultLayout;
    private String path;
    private OrdinatoreFilesFtp ordinatoreFiles;
    private ListFilter filter;
    private FtpFileManager ftpFileManager;

    private CopyHandler copyHandler;
    private FtpEliminaHandler eliminaHandler;
    private FtpRinominaHandler rinominaHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentFtpExplorer(){}


    public static FragmentFtpExplorer getInstance(String path){
        final FragmentFtpExplorer fragment = new FragmentFtpExplorer();
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE_PATH, path);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        path = getArguments().getString(KEY_BUNDLE_PATH, "");
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        eliminaHandler = new FtpEliminaHandler(getActivity(), this);
        rinominaHandler = new FtpRinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        activityMain = (ActivityMain)getActivity();
        setActivityMain(activityMain);
        setTitle(R.string.server_ftp);

        if(activityMain.getFtpSession() == null){ //esco in caso di errore
            return v;
        }

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
        visualizzazione = new VisualizzazioneFilesFtp(recyclerView, this);
        visualizzazione.aggiornaVisualizzazione(tipoVisualizzazione);
        adapter = (DatiFilesFtpBaseAdapter)recyclerView.getAdapter();
        setMultiselectableAdapter(adapter);

        //pathbar
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        pathBar = new FtpPathBar(pathView, this);
        pathBar.setStartFolderName(activityMain.getFtpSession().getServerFtp().getHost());

        ftpFileManager = new FtpFileManager(getActivity(), activityMain.getFtpSession());
        ordinatoreFiles = new OrdinatoreFilesFtp(getPrefs());

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

        if(activityMain.getFtpSession() == null){
            activityMain.finishCurrentFragment();
            return;
        }

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
        ftpFileManager.ls(path, this);
    }



    @Override
    public void onStop(){
        super.onStop();
        if(filter != null) {
            filter.chiudiSearchView();
        }
        if(ordinatoreFiles != null) {
            ordinatoreFiles.salvaStatoOrdinamento();
        }
    }


    @Override
    public void onDestroy(){
        if(copyHandler != null) copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        if(eliminaHandler != null) eliminaHandler.dismissProgressDialogOnDestroy();
        if(rinominaHandler != null) rinominaHandler.dismissProgressDialogOnDestroy();
        super.onDestroy();
    }



    /**
     * Al click sull'elemento avvia la navigazione se è una cartella o apre il file se è un file
     * @param file File o cartella da gestire
     */
    @Override
    public void onItemClick(final FtpElement file) {
        if(!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            if (file.isDirectory()) {
                final String newPath = path + "/" + file.getName();
                activityMain.showFragment(FragmentFtpExplorer.getInstance(newPath));
            } else {
                final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                builder.setMessage(R.string.scaricare_il_file);
                builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    final File cartellaDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                    final List<FtpElement> daScaricare = new ArrayList<>(1);
                    daScaricare.add(file);
                    ftpFileManager.download(daScaricare, cartellaDownload, copyHandler, null);
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
    public void onItemLongClick(FtpElement file) {
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
            case Clipboard.TIPOFILE_FTP:
                ftpFileManager.copia(new ArrayList<>(clipboard.getListaFtpFiles()), clipboard.getServerFtp(), path, copyHandler);
                break;
            case Clipboard.TIPOFILE_LOCALE:
                ftpFileManager.upload(new ArrayList<>(clipboard.getListaFiles()), path, clipboard.isCutMode(), copyHandler);
                break;
            case Clipboard.TIPOFILE_SMB:
                CustomDialogBuilder.make(getContext(), R.string.no_smb_to_ftp, CustomDialogBuilder.TYPE_WARNING).show();
                break;
        }
    }








    /* FILE MANAGER LISTENERS */


    /**
     * Al termine della scansione della cartella visualizza il suo contenuto
     * @param directoryPath Directory scansionata
     * @param files Lista di files o directory al suo interno
     */
    @Override
    public void onFtpLsFinished(String directoryPath, List<FtpElement> files) {
        progressLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        if(files.isEmpty()){
            swipeLayoutReciclerView.setVisibility(View.GONE);
            swipeLayoutEmptyView.setVisibility(View.VISIBLE);
        } else {
            swipeLayoutReciclerView.setVisibility(View.VISIBLE);
            swipeLayoutEmptyView.setVisibility(View.GONE);
            boolean aggiornato = adapter.update(ordinatoreFiles.ordinaListaFiles(files));
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
    public void onFtpNewFolderFinished(boolean created){
        if(created) {
            ls();
        }
    }



    /**
     * Al termine della cancellazione aggiornaFilesLocali la lista files per rimuovere i files cancellati
     * @param success True se tutti i files sono stati cancellati
     */
    @Override
    public void onFtpDeleteFinished(boolean success, List<FtpElement> deletedFiles){
        //deleted è false anche quando l'operazione è stata annullata (ma qualche file è stato cancellato)
        ls();
    }


    /**
     * Al termine della rinominazione aggiornaFilesLocali la lista files per mostrare i nuovi nomi
     * @param success True se tutti i files sono stati rinominati con successo
     * @param oldFiles Files non più presenti (perchè sono stati rinominati e hanno un path diverso)
     * @param newFilesPaths Nuovi files (files con il nuovo nome e quindi path diverso)
     */
    @Override
    public void onFtpRenameFinished(boolean success, List<FtpElement> oldFiles, List<String> newFilesPaths){
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
            case CopyService.COPY_FTP_TO_LOCAL:
                //quando l'utente ha scaricato il file in locale
                if(!filesCopiati.isEmpty()){
                    final CustomDialogBuilder dBuilder = new CustomDialogBuilder(getContext());
                    dBuilder.setMessage(getString(R.string.file_scaricato_nella_cartella, destinationPath));
                    dBuilder.setPositiveButton(R.string.apri_file, (dialogInterface, i) -> {
                        final File cartellaDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                        final String fileName = FtpFileUtils.getNameFromPath(filesCopiati.get(0));
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
            case CopyService.COPY_LOCAL_TO_FTP:
            case CopyService.COPY_FTP_TO_FTP:
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

        boolean selezioneMultipla = adapter != null && adapter.modalitaSelezioneMultipla();

        //selezione multipla
        if(selezioneMultipla){
            inflater.inflate(R.menu.menu_selezione_multipla_ftp, menu);
        }
        inflater.inflate(R.menu.menu_files_ftp, menu);

        //clipboard
        if(activityMain.getClipboard().isEmpty()){
            final MenuItem itemIncolla = menu.findItem(R.id.incolla);
            itemIncolla.setVisible(false);
        }
        final MenuItem itemMostraClipbard = menu.findItem(R.id.mostra_clipboard);
        itemMostraClipbard.setVisible(!activityMain.getClipboard().isEmpty());

        //filtro
        final MenuItem searchItem = menu.findItem(R.id.filtro);
        searchItem.setVisible(!selezioneMultipla);
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
                    adapter = (DatiFilesFtpBaseAdapter)recyclerView.getAdapter();
                    setMultiselectableAdapter(adapter);
                    getPrefs().edit().putInt(KEY_PREF_TIPO_VISUALIZZAZIONE, tipoVisualizzazione).apply();
                });
                dialogVisualizzazioneBuilder.nascondiVisualizzazioneAnteprima();
                dialogVisualizzazioneBuilder.create().show();
                return true;
            case R.id.nuova_cartella:
                final DialogNewFolderBuilder dialogNewFolderBuilder = new DialogNewFolderBuilder(getContext(), name -> ftpFileManager.creaCartella(path, name, FragmentFtpExplorer.this));
                dialogNewFolderBuilder.create().show();
                return true;
            case R.id.copia:
                if(adapter.numElementiSelezionati() > 0) {
                    ColoredToast.makeText(getContext(), String.format(getString(R.string.elementi_negli_appunti), String.valueOf(adapter.numElementiSelezionati())), Toast.LENGTH_LONG).show();
                    final ServerFtp serverFtp = activityMain.getFtpSession().getServerFtp();
                    activityMain.getClipboard().aggiornaFilesFtp(adapter.getElementiSelezionati(), serverFtp);
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
                    final DialogNewNameBuilder dialogNewNameBuilder = new DialogNewNameBuilder(getContext(), nomeFile, name -> {
                        ftpFileManager.rinomina(adapter.getElementiSelezionati(), name, rinominaHandler);
                        disattivaSelezioneMultipla();
                    });
                    dialogNewNameBuilder.create().show();
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.elimina:
                if(adapter.numElementiSelezionati() > 0) {
                    ftpFileManager.elimina(adapter.getElementiSelezionati(), eliminaHandler);
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.proprieta:
                if(adapter.numElementiSelezionati() > 0) {
                    ftpFileManager.mostraProprieta(adapter.getElementiSelezionati());
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
