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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.DialogGiorniFilesRecentiBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.dialog.DialogNewNameBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaFilesBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.filemanager.thread.RinominaHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.fileutils.PreferitiManager;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.fileutils.TroppiElementiException;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.mediastore.FilesRecentiTask;
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;
import it.Ettore.egalfilemanager.recycler.FilesRecentiAdapterLista;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;
import it.Ettore.egalfilemanager.widget.MyWidgetManager;

import static it.Ettore.egalfilemanager.Costanti.GIORNI_RECENTI_DEFAULT;
import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_PRESENTAZIONE;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_GIORNI_RECENTI;


/**
 * Fragment per la visualizzazione dei files recenti
 */
public class FragmentFilesRecenti extends FragmentBaseExplorer implements FilesRecentiTask.FilesRecentiTaskListener, DatiFilesLocaliBaseAdapter.OnItemTouchListener,
        CopyHandlerListener, EliminaHandler.EliminaListener, RinominaHandler.RinominaListener {
    private static final String KEY_BUNDLE_LISTA_FILES_SALVATA = "lista_files_salvata";
    private static final String FILENAME_SERIALIZZAZIONE_LISTA = "lista_files_recenti.ser";
    private static final int resIdTitolo = R.string.tool_recenti;
    private ActivityMain activityMain;
    private RecyclerView recyclerView;
    private LinearLayout progressLayout;
    private TextView emptyView;
    private FilesRecentiTask filesRecentiTask;
    private FileManager fileManager;
    private FilesRecentiAdapterLista adapter;
    private ListFilter filter;
    private OrdinatoreFiles ordinatoreFiles;
    private MyWidgetManager widgetManager;
    private CopyHandler copyHandler;
    private EliminaHandler eliminaHandler;
    private RinominaHandler rinominaHandler;


    /**
     * Costruttore di default (necessario)
     */
    public FragmentFilesRecenti(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        eliminaHandler = new EliminaHandler(getActivity(), this);
        rinominaHandler = new RinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_recenti, container, false);
        activityMain = (ActivityMain)getActivity();
        setActivityMain(activityMain);
        setTitle(resIdTitolo);

        fileManager = new FileManager(getContext());
        ordinatoreFiles = new OrdinatoreFiles(getPrefs());
        ordinatoreFiles.ottieniStatoMostraNascosti();
        adapter = new FilesRecentiAdapterLista(getContext(), this);
        widgetManager = new MyWidgetManager(getContext());
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());
        recyclerView.setAdapter(adapter);
        progressLayout = v.findViewById(R.id.progress_layout);
        emptyView = v.findViewById(R.id.empty_view);
        setMultiselectableAdapter(adapter);

        //esco dalla modalità selezione multipla premendo il tasto indietro
        configuraBackButton(v);

        if(savedInstanceState == null){
            //avvio normale
            int giorni = getPrefs().getInt(KEY_PREF_GIORNI_RECENTI, GIORNI_RECENTI_DEFAULT);
            avviaRicercaFiles(giorni);
        } else {
            //se il fragment cambia orientamento recupero la lista files salvata
            if(savedInstanceState.getBoolean(KEY_BUNDLE_LISTA_FILES_SALVATA, false)) {
                final SerializableFileList listaFilesTrovati = SerializableFileList.fromSavedFile(getContext(), FILENAME_SERIALIZZAZIONE_LISTA);
                onRecentFilesFound(listaFilesTrovati.toFileList());
            }
        }

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();

        if (!activityMain.getPermissionsManager().hasPermissions()) {
            activityMain.getPermissionsManager().requestPermissions();
            activityMain.finishCurrentFragment();
        }
    }


    /**
     * Avvia il task per la ricerca dei files
     * @param giorni Numero di giorni da includere nei files recenti
     */
    private void avviaRicercaFiles(int giorni){
        progressLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        if(filesRecentiTask != null){
            filesRecentiTask.cancel(true);
        }
        filesRecentiTask = new FilesRecentiTask(getActivity(), giorni, this);
        filesRecentiTask.setMostraNascosti(getPrefs().getBoolean(Costanti.KEY_PREF_MOSTRA_NASCOSTI, false));
        filesRecentiTask.execute();
    }


    /**
     * Chiamato al termine della ricerca
     * @param listaFiles Lista dei files trovati
     */
    @Override
    public void onRecentFilesFound(List<File> listaFiles) {
        activityMain.setActionBarTitle(String.format("%s (%s)", getString(resIdTitolo), String.valueOf(listaFiles.size())));
        progressLayout.setVisibility(View.GONE);
        if(listaFiles.isEmpty()){
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.requestFocus();
            ordinatoreFiles.setOrdinaPer(OrdinatoreFiles.OrdinaPer.DATA);
            ordinatoreFiles.setTipoOrdinamento(OrdinatoreFiles.TipoOrdinamento.DESCRESCENTE);
            final List<File> listaOrdinata = ordinatoreFiles.ordinaListaFiles(listaFiles);
            adapter.update(listaOrdinata);
            if(filter != null){
                filter.chiudiSearchView();
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        //interrompo la ricerca se è in corso quando il fragment non è visualizzato
        if(filesRecentiTask != null){
            filesRecentiTask.cancel(true);
        }
        if(filter != null){
            filter.chiudiSearchView();
        }
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
     * Chiamato al click sulla recicler view
     * @param file File clickato
     */
    @Override
    public void onItemClick(File file) {
        if(!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            new FileOpener(getContext()).openFile(file);
        } else {
            //modalità selezione multipla
            mostraNumeroElementiSelezionati(true);
        }
    }


    /**
     * Chiamato al click lungo sulla listview
     * @param file File clickato
     */
    @Override
    public void onItemLongClick(File file) {
        //dopo aver attivato la selezione multipla
        mostraNumeroElementiSelezionati(adapter.modalitaSelezioneMultipla());
    }


    /**
     * Mostra sull'action bar il titolo del fragment o il numero di elementi selezionati
     * @param mostraNumElementi True mostra gli elementi. False mostra il titolo.
     */
    @Override
    public void mostraNumeroElementiSelezionati(boolean mostraNumElementi){
        String nuovoTitolo;
        if(mostraNumElementi) {
            nuovoTitolo = String.format(Locale.ENGLISH, "%s/%s", adapter.numElementiSelezionati(), adapter.getItemCount());
        } else {
            nuovoTitolo = String.format("%s (%s)", getString(resIdTitolo), String.valueOf(adapter.getItemCount()));
        }
        activityMain.setActionBarTitle(nuovoTitolo);
        activityMain.invalidateOptionsMenu();
    }



    /**
     * Dopo aver avviato le dialog per la scelta della cartella di destinazione avvia la copia o lo spostamento dei files selezionati presenti nell'adapter
     * @param sposta True modalità sposta. False modalità copia.
     */
    private void copiaIn(final boolean sposta) {
        //dialog per la scelta dello storage
        final List<File> daCopiare = new ArrayList<>(adapter.getElementiSelezionati()); //creo una copia perchè cancello quelli dell'adapter disattivando la selezione multipla
        disattivaSelezioneMultipla();
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
                            fileManager.copia(daCopiare, destination, copyHandler);
                        } else {
                            fileManager.sposta(daCopiare, destination, copyHandler);
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
     * Se il file si trova su un percorso esterno, mostra la dialog per otteneri il tree uri della sd esterna
     * @param file File
     */
    private void chiediPermessiScritturaExtSd(File file){
        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
        new ChiediTreeUriTask(activityMain, file, true).execute();
    }


    /**
     * Effettua una nuova ricerca dei files recenti
     */
    private void aggiorna(){
        try {
            int giorni = getPrefs().getInt(KEY_PREF_GIORNI_RECENTI, GIORNI_RECENTI_DEFAULT);
            avviaRicercaFiles(giorni);
        } catch (Exception ignored){} //in rari casi non è possibile ottenere le prefs (forse l'activity è stata chiusa prima)
    }


    /**
     * Salva la lista files per evitare che cambiando orientamento si deve effettuare nuovamente la ricerca
     * @param outState Outstate
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(adapter != null) {
            adapter.setFilterMode(false); //disattivo il filtro se attivato per fare il backup di tutti i files
            final SerializableFileList serializableFileList = SerializableFileList.fromFileList(adapter.getListaFiles());
            boolean salvato = serializableFileList.saveToFile(getContext(), FILENAME_SERIALIZZAZIONE_LISTA);
            if (salvato) {
                outState.putBoolean(KEY_BUNDLE_LISTA_FILES_SALVATA, true);
            }
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);

        //selezione multipla
        if(adapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_selezione_multipla_recenti, menu);
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
            if(adapter.numElementiSelezionati() > 1){
                //nascondo i menu che richiedono un solo file
                final MenuItem rinominaItem = menu.findItem(R.id.rinomina);
                rinominaItem.setVisible(false);
                final MenuItem apriPercorsoItem = menu.findItem(R.id.apri_percorso);
                apriPercorsoItem.setVisible(false);
            }
        }
        inflater.inflate(R.menu.menu_recenti, menu);

        //filtro
        final MenuItem searchItem = menu.findItem(R.id.filtro);
        searchItem.setVisible(!adapter.modalitaSelezioneMultipla());
        filter = new ListFilter((SearchView) searchItem.getActionView());
        filter.configuraSearchView(adapter);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.giorni:
                final DialogGiorniFilesRecentiBuilder giorniFilesRecentiBuilder = new DialogGiorniFilesRecentiBuilder(getContext(), newDays -> avviaRicercaFiles(newDays));
                giorniFilesRecentiBuilder.create().show();
                return true;
            case R.id.ordina:
                final DialogOrdinaFilesBuilder dialogOrdinaBuilder = new DialogOrdinaFilesBuilder(getContext(), ordinatoreFiles, (dialogInterface, i) -> {
                    final List<File> listaOrdinata = ordinatoreFiles.ordinaListaFiles(adapter.getListaFiles());
                    adapter.update(listaOrdinata);
                    recyclerView.scrollToPosition(0);
                    if(filter != null){
                        filter.chiudiSearchView();
                    }
                });
                dialogOrdinaBuilder.create().show();
                return true;
            case R.id.aggiorna:
                aggiorna();
                return true;
            case R.id.copia_in:
                if(adapter.numElementiSelezionati() > 0) {
                    copiaIn(false);
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.sposta_in:
                if(adapter.numElementiSelezionati() > 0){
                    copiaIn(true);
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.rinomina:
                //solo un elemento
                if(adapter.numElementiSelezionati() == 1) {
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
            case R.id.condividi:
                if(adapter.numElementiSelezionati() > 0) {
                    new FileOpener(getContext()).shareFiles(adapter.getElementiSelezionati());
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
            case R.id.apri_come:
                if(adapter.numElementiSelezionati() == 1 && !adapter.getElementiSelezionati().get(0).isDirectory()) {
                    new FileOpener(getContext()).openFileAs(adapter.getElementiSelezionati().get(0));
                }
                disattivaSelezioneMultipla();
                return true;
            case R.id.apri_percorso:
                if(adapter.numElementiSelezionati() == 1) {
                    activityMain.showFragment(FragmentFilesExplorer.getInstance(adapter.getElementiSelezionati().get(0).getParentFile()));
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



    /** FILE MANAGER LISTENER */

    @Override
    public void onFileManagerDeleteFinished(boolean success, List<File> deletedFiles) {
        if(success){
            aggiorna();
        }
    }

    @Override
    public void onFileManagerRenameFinished(boolean success, List<File> oldFiles, List<File> newFiles) {
        if(success){
            //l'aggiornamento richiederebbe l'attesa dell'inserimento del file nel media store, quindi aggiorno l'adapter manualmente
            final List<File> nuovaListaFiles = new ArrayList<>(adapter.getListaFiles());
            nuovaListaFiles.removeAll(oldFiles);
            nuovaListaFiles.addAll(newFiles);
            final List<File> listaOrdinata = ordinatoreFiles.ordinaListaFiles(nuovaListaFiles);
            adapter.update(listaOrdinata);
        }
    }

}
