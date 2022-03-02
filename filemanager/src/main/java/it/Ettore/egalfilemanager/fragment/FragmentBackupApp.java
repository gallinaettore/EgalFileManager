package it.Ettore.egalfilemanager.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.thread.CopiaSingoloFileHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.tools.backupapp.AppInfo;
import it.Ettore.egalfilemanager.tools.backupapp.AppInfosThread;
import it.Ettore.egalfilemanager.tools.backupapp.AppsAdapter;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment che mostra le app installate per effettuarne il backup
 */
public class FragmentBackupApp extends GeneralFragment implements AppInfosThread.AppInfosListener {
    private static final int CONTEXT_MENU_APP_INFO = 1;
    private static final int CONTEXT_MENU_BACKUP = 2;
    private ActivityMain activityMain;
    private ListView listView;
    private ProgressBar progressBar;
    private AppsAdapter adapter;
    private int appType = AppInfosThread.TYPE_USER;
    private AppInfosThread thread;
    private boolean ricercaTerminata = false;
    private ListFilter filter;
    private CopiaSingoloFileHandler copiaSingoloFileHandler;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentBackupApp(){}


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //creo l'adapter qui in modo da evitare che ogni volta che la view viene creata (visualizata a ascermo) ricrei l'adapter perdendo il suo contenuto
        adapter = new AppsAdapter(getContext(), new ArrayList<>());
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        copiaSingoloFileHandler = new CopiaSingoloFileHandler(getActivity(), null);

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_backup_app, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.tool_backup_app);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        listView = v.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);	//registro il menu contestuale
        progressBar = v.findViewById(R.id.progressBar);

        //non posso salvare la lista appinfo nel bundle quindi alla rotazione effettua nuovamente la ricerca
        ricercaApp();

        return v;
    }


    @Override
    public void onStart() {
        super.onStart();
        new Handler().postDelayed(() -> listView.requestFocus(), 200);
    }


    /**
     * Effettua la ricerca delle app installate
     */
    private void ricercaApp(){
        listView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        thread = new AppInfosThread(getActivity(), appType, this);
        thread.start();
        ricercaTerminata = false;
    }


    /**
     * Chiamato al termine della ricerca
     * @param listaInfo Lista contenente le informazioni delle app installate
     */
    @Override
    public void onAppInfosObtained(List<AppInfo> listaInfo) {
        ricercaTerminata = true;
        activityMain.invalidateOptionsMenu();

        progressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        listView.requestFocus();
        adapter.update(listaInfo);

        final String title = appType == AppInfosThread.TYPE_SYSTEM ? getString(R.string.app_sistema) : getString(R.string.app_utente);
        activityMain.setActionBarTitle(String.format("%s (%s)", title, String.valueOf(listaInfo.size())));
    }


    @Override
    public void onStop(){
        super.onStop();
        //chiudo la search view
        if(filter != null) {
            filter.chiudiSearchView();
        }
    }


    @Override
    public void onDestroy() {
        if(thread != null){
            thread.interrompi();
        }
        if(copiaSingoloFileHandler != null) {
            copiaSingoloFileHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
    }



    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        if(ricercaTerminata) {
            inflater.inflate(R.menu.menu_backup_app, menu);
            final MenuItem itemAppUser = menu.findItem(R.id.mostra_app_utente);
            itemAppUser.setVisible(appType == AppInfosThread.TYPE_SYSTEM);
            final MenuItem itemAppSystem = menu.findItem(R.id.mostra_app_sistema);
            itemAppSystem.setVisible(appType == AppInfosThread.TYPE_USER);

            //filtro
            final MenuItem searchItem = menu.findItem(R.id.filtro);
            filter = new ListFilter((SearchView) searchItem.getActionView());
            filter.configuraSearchView(adapter);
        }
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filtro:
                return true;
            case R.id.mostra_app_utente:
                appType = AppInfosThread.TYPE_USER;
                activityMain.setActionBarTitle(R.string.app_utente);
                ricercaApp();
                return true;
            case R.id.mostra_app_sistema:
                appType = AppInfosThread.TYPE_SYSTEM;
                activityMain.setActionBarTitle(R.string.app_sistema);
                ricercaApp();
                return true;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
    }


    /**
     * Chiamato per la creazione del menu contestuale
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list_view) {
            //menu.setHeaderTitle("Menù contestuale");
            menu.add(Menu.NONE, CONTEXT_MENU_APP_INFO, 1, getString(R.string.app_info));
            menu.add(Menu.NONE, CONTEXT_MENU_BACKUP, 1, getString(R.string.app_backup));
        }
    }


    /**
     * Gestisce i click sul menu contestuale
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final AppInfo appInfo = adapter.getItem(info.position);
        int id = item.getItemId();
        switch (id) {
            case CONTEXT_MENU_APP_INFO:
                final DialogInfoBuilder dialogInfoBuilder = new DialogInfoBuilder(getContext(), R.string.app_info, appInfo.toMap(getContext()));
                dialogInfoBuilder.create().show();
                return true;
            case CONTEXT_MENU_BACKUP:
                final File apk = appInfo.apk;
                //dialog per la scelta dello storage
                final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
                builder.setTitle(R.string.seleziona_destinazione);
                builder.hideIcon(true);
                builder.setStorageItems(new HomeNavigationManager(getActivity()).listaItemsArchivioLocale());
                builder.setCancelable(false);
                builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
                    @Override
                    public void onSelectStorage(File storagePath) {
                        //dopo aver selezionato lo storage, seleziono la destinazione
                        final FileManager fileManager = new FileManager(getContext());
                        fileManager.ottieniStatoRootExplorer();
                        final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(getContext(), DialogFileChooserBuilder.TYPE_SAVE_FILE);
                        fileChooser.setTitle(R.string.seleziona_destinazione);
                        fileChooser.setCancelable(false);
                        fileChooser.setStartFolder(storagePath);
                        fileChooser.setFileName(appInfo.name + " " + appInfo.versionName + ".apk");
                        fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                            @Override
                            public void onFileChooserSelected(final File selected) {
                                fileManager.copiaSingoloFile(apk, selected, copiaSingoloFileHandler);
                            }

                            @Override
                            public void onFileChooserCanceled() { }
                        });
                        fileChooser.create().show();

                        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                        new ChiediTreeUriTask(activityMain, storagePath, true).execute();
                    }

                    @Override
                    public void onCancelStorageSelection() {}
                });
                builder.showSelectDialogIfNecessary();
                return true;
        }
        return false;
    }
}
