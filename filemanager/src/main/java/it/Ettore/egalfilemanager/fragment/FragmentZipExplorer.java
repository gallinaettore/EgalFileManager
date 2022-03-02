package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.activity.ActivityZipViewer;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.thread.ExtractHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.pathbar.ZipPathBar;
import it.Ettore.egalfilemanager.recycler.DatiZipAdapterLista;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;
import it.Ettore.egalfilemanager.zipexplorer.ArchiveEntry;
import it.Ettore.egalfilemanager.zipexplorer.OpenFileFromZipTask;
import it.Ettore.egalfilemanager.zipexplorer.ZipExplorer;


/**
 * Fragment per l'esplorazione di archivi compressi
 */
public class FragmentZipExplorer extends GeneralFragment implements DatiZipAdapterLista.OnItemTouchListener, ZipPathBar.PathClickListener, ExtractHandler.ZipExtractListener,
        SelectStorageDialogBuilder.SelectStorageListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String KEY_BUNDLE_FILE_ZIP = "zip_file";
    private static final String KEY_BUNDLE_ENTRY_PATH = "entry_path";
    private ActivityZipViewer activityZipViewer;
    private String titolo;
    private File zipFile;
    private ZipExplorer zipExplorer;
    private DatiZipAdapterLista adapter;
    private ArchiveEntry currentEntry;
    private ZipPathBar layoutPathManager;
    private ExtractHandler extractHandler;
    private SwipeRefreshLayout swipeLayoutReciclerView, swipeLayoutEmptyView;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentZipExplorer(){}


    /**
     * Metodo factory per creare un'istanza del fragment
     * @param zipFile File compresso
     * @param currentEntryPath Percorso della cartella da visualizzare ottenuto dall'entry del file compresso
     * @return Istanza del fragment
     */
    public static FragmentZipExplorer getInstance(@NonNull File zipFile, String currentEntryPath){
        final FragmentZipExplorer fragment = new FragmentZipExplorer();
        final Bundle bundle = new Bundle();
        bundle.putString(KEY_BUNDLE_FILE_ZIP, zipFile.getAbsolutePath());
        if(currentEntryPath != null){
            bundle.putString(KEY_BUNDLE_ENTRY_PATH, currentEntryPath);
        }
        fragment.setArguments(bundle);
        return fragment;
    }



    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        final Bundle args = getArguments();
        this.zipFile = new File(args.getString(KEY_BUNDLE_FILE_ZIP, null));
        zipExplorer = new ZipExplorer(zipFile);
        this.currentEntry = zipExplorer.getEntryWithPath(args.getString(KEY_BUNDLE_ENTRY_PATH, null));
        titolo = getString(R.string.zip_explorer);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        extractHandler = new ExtractHandler(getActivity(), this);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_files, container, false);
        activityZipViewer = (ActivityZipViewer) getActivity();
        activityZipViewer.setActionBarTitle(titolo);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityZipViewer.getOverflowMenu();

        swipeLayoutReciclerView = v.findViewById(R.id.swipe_layout_recyclerview);
        swipeLayoutReciclerView.setColorSchemeResources(R.color.colorAccent);
        swipeLayoutReciclerView.setOnRefreshListener(this);
        swipeLayoutEmptyView = v.findViewById(R.id.swipe_layout_emptyview);
        swipeLayoutEmptyView.setColorSchemeResources(R.color.colorAccent);
        swipeLayoutEmptyView.setOnRefreshListener(this);

        //Configuro la recycler view
        final RecyclerView recyclerView = v.findViewById(R.id.recycler_view);
        final HorizontalScrollView pathView = v.findViewById(R.id.path_scrollview);

        final LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(getContext());
        recyclerLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerLayoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());


        adapter = new DatiZipAdapterLista(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        layoutPathManager = new ZipPathBar(pathView, zipFile.getName(), this);

        ((FloatingActionButton)v.findViewById(R.id.fab)).hide();

        return v;
    }


    @Override
    public void onStart() { //se si utilizza on resume, il metodo del fragment può essere chiamato più volte
        super.onStart();
        final List<ArchiveEntry> listaEntry = zipExplorer.ls(currentEntry);
        adapter.update(listaEntry);
        layoutPathManager.visualizzaPath(currentEntry);
        if(currentEntry == null && listaEntry.isEmpty()){
            //se l'archivio risulta tutto vuoto non è stato possibile estrarlo
            CustomDialogBuilder.make(getContext(), R.string.archivio_non_valido, CustomDialogBuilder.TYPE_ERROR).show();
        }
    }


    @Override
    public void onDestroy() {
        if(extractHandler != null) {
            extractHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
    }


    @Override
    public void onItemClick(ArchiveEntry entry) {
        if(entry.isDirectory()){
            activityZipViewer.showFragment(FragmentZipExplorer.getInstance(zipFile, entry.getPath()));
        } else {
            new OpenFileFromZipTask(getActivity(), zipFile, entry).execute();
        }
    }


    @Override
    public void onPathItemClick(String pathEntry) {
        activityZipViewer.showFragment(FragmentZipExplorer.getInstance(zipFile, pathEntry));
    }


    @Override
    public void onZipExtractFinished(boolean successs, File zipFile, File destFolder) {
        if(successs){
            ActivityMain.openFileExplorer(getActivity(), destFolder.getParentFile());
            getActivity().finish();
        }
    }


    //per l'estrazione occorre selezionare prima lo storage e poi la cartella
    @Override
    public void onSelectStorage(File storagePath) {
        final FileManager fileManager = new FileManager(getContext());
        fileManager.ottieniStatoRootExplorer();
        final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(getContext(), DialogFileChooserBuilder.TYPE_SELECT_FOLDER);
        fileChooser.setTitle(R.string.seleziona_destinazione);
        fileChooser.setStartFolder(storagePath);
        fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
            @Override
            public void onFileChooserSelected(File selected) {
                final File destFolder = new File(selected, FileUtils.getFileNameWithoutExt(zipFile));
                if(destFolder.exists()){
                    final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                    builder.setType(CustomDialogBuilder.TYPE_WARNING);
                    builder.setMessage(String.format(getString(R.string.sovrascrivi_cartella), destFolder.getName()));
                    builder.setPositiveButton(R.string.sovrascrivi, (dialogInterface, i) -> fileManager.estraiArchivio(zipFile, destFolder, extractHandler));
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.create().show();
                } else {
                    fileManager.estraiArchivio(zipFile, destFolder, extractHandler);
                }
            }

            @Override
            public void onFileChooserCanceled() {}
        });
        fileChooser.create().show();

        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
        new ChiediTreeUriTask(activityZipViewer, storagePath, true).execute();
    }

    @Override
    public void onCancelStorageSelection() {}


    @Override
    public void onRefresh() {
        final List<ArchiveEntry> listaEntry = zipExplorer.ls(currentEntry);
        adapter.update(listaEntry);
        swipeLayoutReciclerView.setRefreshing(false);
        swipeLayoutEmptyView.setRefreshing(false);
    }




    /* MENU */


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_zip, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.estrai_archivio:
                //dialog per la scelta dello storage
                final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
                builder.setTitle(R.string.seleziona_destinazione);
                builder.hideIcon(true);
                builder.setStorageItems(new HomeNavigationManager(getActivity()).listaItemsArchivioLocale());
                builder.setSelectStorageListener(FragmentZipExplorer.this);
                builder.showSelectDialogIfNecessary();
                return true;
            case R.id.proprieta:
                if(getContext() != null) {
                    final FileManager fileManager = new FileManager(getContext());
                    final List<File> files = new ArrayList<>();
                    files.add(zipFile);
                    fileManager.mostraProprietaCategoria(files);
                }
                return true;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
    }


}
