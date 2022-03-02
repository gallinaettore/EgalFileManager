package it.Ettore.egalfilemanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
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
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;
import it.Ettore.egalfilemanager.recycler.FilesTrovatiAdapterLista;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;
import it.Ettore.egalfilemanager.tools.ricercafiles.ParametriRicerca;
import it.Ettore.egalfilemanager.tools.ricercafiles.RicercaThread;
import it.Ettore.egalfilemanager.widget.MyWidgetManager;

import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_PRESENTAZIONE;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment che mostra i risultati della ricerca files
 */
public class FragmentRisultatiRicercaFiles extends FragmentBaseExplorer implements RicercaThread.RicercaFilesListener, DatiFilesLocaliBaseAdapter.OnItemTouchListener,
        CopyHandlerListener, EliminaHandler.EliminaListener, RinominaHandler.RinominaListener {
    private static final String KEY_BUNDLE_PARAMETRI_RICERCA = "parametri_ricerca";
    private static final String KEY_BUNDLE_LISTA_FILES_SALVATA = "lista_files_salvata";
    private static final String FILENAME_SERIALIZZAZIONE_LISTA = "lista_files_risultati_ricerca.ser";
    private static final int resIdTitolo = R.string.risultati_ricerca;
    private ActivityMain activityMain;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private RicercaThread ricercaThread;
    private boolean ricercaTerminata = false;
    private FilesTrovatiAdapterLista adapter;
    private FileManager fileManager;
    private OrdinatoreFiles ordinatoreFiles;
    private MyWidgetManager widgetManager;
    private CopyHandler copyHandler;
    private EliminaHandler eliminaHandler;
    private RinominaHandler rinominaHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentRisultatiRicercaFiles(){}


    /**
     * Metodo factory per creare un'istanza del fragment
     * @param parametriRicerca Oggetto wrapper che contiene tutti i dati per effettuare la ricerca
     * @return Fragment
     */
    public static FragmentRisultatiRicercaFiles getInstance(ParametriRicerca parametriRicerca){
        final FragmentRisultatiRicercaFiles fragment = new FragmentRisultatiRicercaFiles();
        final Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_BUNDLE_PARAMETRI_RICERCA, parametriRicerca);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //creo l'adapter qui in modo da evitare che ogni volta che la view viene creata (visualizata a schermo) ricrei l'adapter perdendo il suo contenuto
        adapter = new FilesTrovatiAdapterLista(getContext(), this);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copyHandler = new CopyHandler(getActivity(), this);
        eliminaHandler = new EliminaHandler(getActivity(), this);
        rinominaHandler = new RinominaHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_risultati_ricerca_files, container, false);
        activityMain = (ActivityMain)getActivity();
        setActivityMain(activityMain);
        setTitle(resIdTitolo);

        recyclerView = v.findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());
        recyclerView.setAdapter(adapter);
        emptyView = v.findViewById(R.id.empty_view);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        setMultiselectableAdapter(adapter);

        fileManager = new FileManager(getContext(), getPrefs());
        fileManager.ottieniStatoRootExplorer();
        ordinatoreFiles = new OrdinatoreFiles(getPrefs());
        ordinatoreFiles.ottieniStatoMostraNascosti();
        widgetManager = new MyWidgetManager(getContext());

        if(!ricercaTerminata && savedInstanceState == null) {
            //se il fragment è stato avviato per la prima volta eseguo la ricerca
            LockScreenOrientation.lock(getActivity()); //durante la ricerca nonfaccio ruotare il dispositivo
            final ParametriRicerca parametriRicerca = (ParametriRicerca) getArguments().getSerializable(KEY_BUNDLE_PARAMETRI_RICERCA);
            ricercaThread = new RicercaThread(getActivity(), parametriRicerca, this);
            ricercaThread.start();
            ricercaTerminata = false;
        } else if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_BUNDLE_LISTA_FILES_SALVATA, false)){
            //se il fragment cambia orientamento recupero la lista files salvata
            final SerializableFileList listaFilesTrovati = SerializableFileList.fromSavedFile(getContext(), FILENAME_SERIALIZZAZIONE_LISTA);
            onSearchFinished(listaFilesTrovati.toFileList());
        } else {
            //se il fragment viene mostrato nuovamente (ma già aveva ricercato) visualizzo i risultati trovati in precedenza
            onSearchFinished(adapter.getListaFiles());
        }

        return v;
    }



    public void onStart(){
        super.onStart();
        ordinatoreFiles.ottieniStatoOrdinamento();
    }


    /**
     * Chiamato al termine della ricerca
     * @param filesTrovati Lista di files trovati
     */
    @Override
    public void onSearchFinished(List<File> filesTrovati) {
        //i files ordinati possono essere di numero inferiore rispetto ai files trovati perchè possono non mostrare i files nascosti
        final List<File> filesOrdinati = ordinatoreFiles.ordinaListaFiles(filesTrovati);

        ricercaTerminata = true;
        activityMain.invalidateOptionsMenu();
        progressBar.setVisibility(View.GONE);
        if(filesOrdinati.isEmpty()){
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.requestFocus();
            adapter.update(filesOrdinati);
        }
        activityMain.setActionBarTitle(String.format("%s (%s)", getString(resIdTitolo), String.valueOf(filesOrdinati.size())));

        //rispristino la possibilità di ruotare il dispositivo
        LockScreenOrientation.unlock(getActivity());
    }


    @Override
    public void onStop(){
        super.onStop();
        ordinatoreFiles.salvaStatoOrdinamento();
    }


    @Override
    public void onDestroy(){
        if(ricercaThread != null) {
            ricercaThread.interrompi();
        }
        //rispristino la possibilità di ruotare il dispositivo
        LockScreenOrientation.unlock(getActivity());
        if(copyHandler != null) copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        if(eliminaHandler != null) eliminaHandler.dismissProgressDialogOnDestroy();
        if(rinominaHandler != null) rinominaHandler.dismissProgressDialogOnDestroy();
        super.onDestroy();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        final SerializableFileList serializableFileList = SerializableFileList.fromFileList(adapter.getListaFiles());
        boolean salvato = serializableFileList.saveToFile(getContext(), FILENAME_SERIALIZZAZIONE_LISTA);
        if(salvato){
            outState.putBoolean(KEY_BUNDLE_LISTA_FILES_SALVATA, true);
        }
        super.onSaveInstanceState(outState);
    }


    /**
     * Chiamato al click sulla recicler view
     * @param file File clickato
     */
    @Override
    public void onItemClick(File file) {
        if(!adapter.modalitaSelezioneMultipla()) {
            //modalità apertura file
            if(file.isDirectory()){
                activityMain.showFragment(FragmentFilesExplorer.getInstance(file));
            } else {
                new FileOpener(getContext()).openFile(file);
            }
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



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(ricercaTerminata) {
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
            inflater.inflate(R.menu.menu_risultati_ricerca, menu);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nuova_ricerca:
                activityMain.showFragment(new FragmentRicercaFiles());
                return true;
            case R.id.ordina:
                final DialogOrdinaFilesBuilder dialogOrdinaBuilder = new DialogOrdinaFilesBuilder(getContext(), ordinatoreFiles, (dialogInterface, i) -> {
                    adapter.update(ordinatoreFiles.ordinaListaFiles(adapter.getListaFiles()));
                    recyclerView.scrollToPosition(0);
                });
                dialogOrdinaBuilder.create().show();
                return true;
            case R.id.copia_in:
                if(adapter.numElementiSelezionati() > 0) {
                    copiaIn(false);
                } else {
                    disattivaSelezioneMultipla();
                }
                return true;
            case R.id.sposta_in:
                if(adapter.numElementiSelezionati() > 0) {
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
                        }
                    );
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
            //l'aggiornamento richiederebbe una nuova ricerca, quindi aggiorno l'adapter manualmente
            final List<File> nuovaListaFiles = new ArrayList<>(adapter.getListaFiles());
            nuovaListaFiles.removeAll(deletedFiles);
            adapter.update(nuovaListaFiles);
            activityMain.setActionBarTitle(String.format("%s (%s)", getString(resIdTitolo), String.valueOf(adapter.getItemCount())));
        }
    }

    @Override
    public void onFileManagerRenameFinished(boolean success, List<File> oldFiles, List<File> newFiles) {
        if(success){
            //l'aggiornamento richiederebbe una nuova ricerca, quindi aggiorno l'adapter manualmente
            final List<File> nuovaListaFiles = new ArrayList<>(adapter.getListaFiles());
            nuovaListaFiles.removeAll(oldFiles);
            nuovaListaFiles.addAll(newFiles);
            final List<File> listaOrdinata = ordinatoreFiles.ordinaListaFiles(nuovaListaFiles);
            adapter.update(listaOrdinata);
        }
    }
}
