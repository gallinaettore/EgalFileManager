package it.Ettore.egalfilemanager.fragment;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.DialogAutenticazioneLanBuilder;
import it.Ettore.egalfilemanager.lan.AutenticazioneLan;
import it.Ettore.egalfilemanager.lan.ServerSmbAdapter;
import it.Ettore.egalfilemanager.lan.thread.ScanServer;
import it.Ettore.egalfilemanager.lan.thread.SmbAuthenticationTask;
import jcifs.smb.SmbFile;



/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione dei server smb (Condivisioni Lan)
 */
public class FragmentServerLan extends GeneralFragment implements
        ScanServer.ScanServerListener,
        AdapterView.OnItemClickListener,
        SmbAuthenticationTask.AuthenticationTaskListener {

    private static final int CONTEXT_MENU_AUTENTICAZIONE = 1;
    private ActivityMain activityMain;
    private LinearLayout layoutRicerca;
    private ListView listView;
    private ServerSmbAdapter adapter;
    private TextView emptyView;
    private ScanServer scanServer;
    private boolean ricercaInCorso;



    /**
     * Costruttore di base (necessario)
     */
    public FragmentServerLan(){}



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_server_lan, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.rete_locale);

        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        emptyView = v.findViewById(R.id.empty_view);
        emptyView.setVisibility(View.GONE);
        layoutRicerca = v.findViewById(R.id.layout_ricerca);
        listView = v.findViewById(R.id.listview);
        listView.setClickable(true);
        registerForContextMenu(listView);	//registro il menu contestuale
        listView.setOnItemClickListener(this);

        scanServer = new ScanServer(getActivity(), this);
        startScan();

        return v;
    }


    /**
     * Avvia la scansione dei server
     */
    private void startScan(){
        adapter = new ServerSmbAdapter(getContext());
        listView.setAdapter(adapter);
        layoutRicerca.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        scanServer.start();
        ricercaInCorso = true;
        activityMain.invalidateOptionsMenu();
    }


    /**
     * Chiamato ogni volta che un server viene trovato
     * @param ipAddress Indirizzo IP del server
     * @param hostName Nome NETBios del server
     */
    @Override
    public void onServerFound(String ipAddress, String hostName) {
        adapter.addServer(ipAddress, hostName);
    }



    /**
     * Chiamato quando termina la scansione dei server
     */
    @Override
    public void onScanFinished() {
        layoutRicerca.setVisibility(View.GONE);
        if(adapter.getCount() == 0){
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
        ricercaInCorso = false;
        activityMain.invalidateOptionsMenu();
    }


    /**
     * Chiamato quando si clicca su un server
     * @param adapterView /
     * @param view /
     * @param position /
     * @param id /
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final String pathDaAprire = adapter.getItem(position).getPath();
        final AutenticazioneLan autenticazioneLan = AutenticazioneLan.fromPreferences(getContext(), pathDaAprire);
        if(autenticazioneLan == null){
            //nessun dato di autenticazione salvato, mostro la dialog
            final DialogAutenticazioneLanBuilder dialogBuilder = new DialogAutenticazioneLanBuilder(getActivity(), pathDaAprire, false, this);
            dialogBuilder.create().show();
        } else {
            //dati di autenticazione trovati, verifico se sono validi
            new SmbAuthenticationTask(getActivity(), pathDaAprire, autenticazioneLan.getUsername(), autenticazioneLan.getPassword(), this).execute();
        }
    }


    @Override
    public void onStop(){
        super.onStop();
        scanServer.interruptAll();
    }


    /**
     * Chiamato al termine della verifica delle credenziali del server
     * @param result Una delle costanti RESULT di SmbAuthenticationTask
     * @param path Path del server smb
     * @param username Username
     * @param password Password
     */
    @Override
    public void onAuthenticationFinished(int result, @NonNull String path, String username, String password) {
        switch (result){
            case SmbAuthenticationTask.RESULT_AUTHENTICATED:
                activityMain.showFragment(FragmentLanExplorer.getInstance(path, username, password));
                break;
            case SmbAuthenticationTask.RESULT_NON_AUTHENTICATED:
                //errore di autenticazione, username o password errati. Mostro nuovamente la dialog per l'inserimento
                final DialogAutenticazioneLanBuilder dialogBuilder = new DialogAutenticazioneLanBuilder(getActivity(), path, true, this);
                dialogBuilder.create().show();
                break;
            case SmbAuthenticationTask.RESULT_ERROR_CONNECTION:
                ColoredToast.makeText(getContext(), getString(R.string.impossibile_connettersi, path), Toast.LENGTH_LONG).show();
                break;
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(!ricercaInCorso){
            inflater.inflate(R.menu.menu_server_lan, menu);
        }
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aggiorna:
                startScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Chiamato per la creazione del menu contestuale
     * @param menu .
     * @param v .
     * @param menuInfo .
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listview) {
            //menu.setHeaderTitle("Menù contestuale");
            menu.add(Menu.NONE, CONTEXT_MENU_AUTENTICAZIONE, 1, getString(R.string.autenticazione));
        }
    }


    /**
     * Gestisce i click sul menu contestuale
     * @param item .
     * @return .
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final SmbFile smbFile = adapter.getItem(info.position);
        int id = item.getItemId();
        switch (id) {
            case CONTEXT_MENU_AUTENTICAZIONE:
                final DialogAutenticazioneLanBuilder dialogBuilder = new DialogAutenticazioneLanBuilder(getActivity(), smbFile.getPath(), false, this);
                dialogBuilder.create().show();
                return true;
        }
        return false;
    }
}
