package it.Ettore.egalfilemanager.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.copyutils.AnalisiResult;
import it.Ettore.egalfilemanager.dialog.DialogAnalisiCartellaBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.pathbar.FilePathBar;
import it.Ettore.egalfilemanager.tools.analisispazio.AdapterAnalisiSpazio;
import it.Ettore.egalfilemanager.tools.analisispazio.AnalisiCartella;
import it.Ettore.egalfilemanager.tools.analisispazio.AnalisiSpazioThread;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione dello spazio occupato da files e cartelle
 */
public class FragmentAnalisiSpazio extends GeneralFragment implements FilePathBar.PathClickListener, SelectStorageDialogBuilder.SelectStorageListener,
        AnalisiSpazioThread.AnalisiSpazioListener, AdapterView.OnItemClickListener {
    private static final String KEY_BUNDLE_START_DIRECTORY = "start_directory";
    private static final String KEY_BUNDLE_START_DIRECTORY_NAME = "start_directory_name";
    private static final String KEY_BUNDLE_CURRENT_DIRECTORY = "current_directory";

    private ActivityMain activityMain;
    private String startDirectoryName;
    private File startDirectory, currentDirectory;
    private ListView listView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private AdapterAnalisiSpazio adapter;
    private List<AnalisiCartella> risultatiAnalisi;
    private AnalisiSpazioThread analisiSpazioThread;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentAnalisiSpazio(){}



    /**
     * Metodo factory per creare un'istanza del fragment
     * @param startDirectory Directory da cui iniziare la navigazione
     * @param startDirectoryName Nome della directory da cui iniziare la navigazione
     * @param currentDirectory Directory da esplorare
     * @return Istanza del fragment
     */
    public static FragmentAnalisiSpazio getInstance(@NonNull File startDirectory, @NonNull String startDirectoryName, File currentDirectory){
        final FragmentAnalisiSpazio fragment = new FragmentAnalisiSpazio();
        final Bundle bundle = new Bundle();
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



    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        final Bundle args = getArguments();
        if(args != null){ //se l'istanza del fragment è stata creata dal costruttore generico
            this.currentDirectory = new File(args.getString(KEY_BUNDLE_CURRENT_DIRECTORY, null));
            this.startDirectory = new File(args.getString(KEY_BUNDLE_START_DIRECTORY, "/"));
            this.startDirectoryName = args.getString(KEY_BUNDLE_START_DIRECTORY_NAME, startDirectory.getName());
        }
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_analisi_spazio_occupato, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.tool_analisi_spazio_occupato);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        //visualizzazione
        listView = v.findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        emptyView = v.findViewById(R.id.empty_view);
        progressBar = v.findViewById(R.id.progressBar);

        //pathbar
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);
        final FilePathBar pathBar = new FilePathBar(pathView, this);
        pathBar.setStartFolderName(startDirectory, startDirectoryName);
        pathBar.visualizzaPath(currentDirectory);

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();

        if(currentDirectory == null){
            //la directory corrente non è stata ancora impostata, seleziono lo storage
            final List<HomeItem> listaSdCards = new HomeNavigationManager(getActivity()).listaItemsSdCards();
            final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
            builder.setTitle(R.string.seleziona_storage);
            builder.hideIcon(true);
            builder.setStorageItems(listaSdCards);
            builder.setSelectStorageListener(FragmentAnalisiSpazio.this);
            builder.show();
        } else {
            if(risultatiAnalisi == null) {
                //avvio l'analisi della directory se non è stata già analizzata prima
                progressBar.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                analisiSpazioThread = new AnalisiSpazioThread(getActivity(), currentDirectory, this);
                analisiSpazioThread.start();
            } else {
                showResults();
            }
        }
    }


    @Override
    public void onStop(){
        super.onStop();
        //interrompo l'analisi se il fragment viene chiuso
        if(analisiSpazioThread != null){
            analisiSpazioThread.interrompi();
        }
    }


    /**
     * Gestisce i click sulla pathbar
     */
    @Override
    public void onPathItemClick(File file) {
        activityMain.showFragment(FragmentAnalisiSpazio.getInstance(startDirectory, startDirectoryName, file));
    }


    @Override
    public void onSelectStorage(File storagePath) {
        //calcolo percoso e nome della directory di inizio da mostrare nella path bar
        if(startDirectory == null){
            final StoragesUtils storagesUtils = new StoragesUtils(getContext());
            if (storagesUtils.isOnInternalSdCard(storagePath)){
                this.startDirectory = storagesUtils.getInternalStorage();
                this.startDirectoryName = getString(R.string.memoria_interna);
            } else if(storagesUtils.isOnExtSdCard(storagePath)){
                final File extStorage  = storagesUtils.getExtStorageForFile(storagePath);
                if(extStorage != null){
                    this.startDirectory = extStorage;
                    this.startDirectoryName = getString(R.string.memoria_esterna);
                } else {
                    this.startDirectory = new File("/");
                    this.startDirectoryName = "/";
                }
            }
        }
        //rimuovo dal backstack il fragment con la dialog di scelta dello storage e avvio il fragment per la visualizzazione
        activityMain.removeFragment(this);
        activityMain.showFragment(FragmentAnalisiSpazio.getInstance(startDirectory, startDirectoryName, storagePath));
    }


    @Override
    public void onCancelStorageSelection() {
        activityMain.finishCurrentFragment();
    }


    /**
     * Chiamato al termine dell'analisi. Mostra i risultati
     * @param listaRisultati Lista risultati dell'analisi. (Un risultato per ogni file).
     */
    @Override
    public void onAnalysisFinished(List<AnalisiCartella> listaRisultati) {
        risultatiAnalisi = listaRisultati;
        showResults();
    }


    /**
     * Mostra i risultati nella listview
     */
    private void showResults(){
        progressBar.setVisibility(View.GONE);
        if(risultatiAnalisi != null && !risultatiAnalisi.isEmpty()){
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }

        adapter = new AdapterAnalisiSpazio(getContext(), risultatiAnalisi != null ? risultatiAnalisi : new ArrayList<>());
        listView.setAdapter(adapter);
        activityMain.invalidateOptionsMenu();
    }


    /**
     * Al click sulla listview esplora le cartelle (non i files)
     * @param adapterView
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final File selectedFile = adapter.getItem(position).file;
        if(selectedFile.isDirectory()){
            activityMain.showFragment(FragmentAnalisiSpazio.getInstance(startDirectory, startDirectoryName, selectedFile));
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(risultatiAnalisi != null && !risultatiAnalisi.isEmpty()) {
            inflater.inflate(R.menu.menu_analisi_cartella, menu);
        }
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.analisi_cartella:
                final DialogAnalisiCartellaBuilder analisiCartellaBuilder = new DialogAnalisiCartellaBuilder(getContext(), AnalisiCartella.mergeList(risultatiAnalisi));
                analisiCartellaBuilder.create().show();
                return true;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
    }
}
