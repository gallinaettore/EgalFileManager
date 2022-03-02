package it.Ettore.egalfilemanager.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.ViewUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.DialogDatiServerFtpBuilder;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.ftp.ServerFtp;
import it.Ettore.egalfilemanager.ftp.ServerFtpAdapter;



/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione dei server ftp
 */
public class FragmentServerFtp extends GeneralFragment implements AdapterView.OnItemClickListener {
    private static final int CONTEXT_MENU_MODIFICA = 1;
    private static final int CONTEXT_MENU_ELIMINA = 2;
    private ActivityMain activityMain;
    private ListView listView;
    private ServerFtpAdapter adapter;
    private TextView emptyView;
    private FloatingActionButton fab;



    /**
     * Costruttore di base (necessario)
     */
    public FragmentServerFtp(){}



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_server_ftp, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.server_ftp);

        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        emptyView = v.findViewById(R.id.empty_view);
        emptyView.setVisibility(View.GONE);
        listView = v.findViewById(R.id.listview);
        listView.setClickable(true);
        registerForContextMenu(listView);	//registro il menu contestuale
        listView.setOnItemClickListener(this);
        fab = v.findViewById(R.id.fab);
        fab.bringToFront(); //compatibilità prelollipop
        fab.setOnClickListener(view -> {
            final DialogDatiServerFtpBuilder builder = new DialogDatiServerFtpBuilder(getContext(), null, (dialogInterface, which) -> {
                if(which == DialogInterface.BUTTON_POSITIVE){
                    mostraServers();
                }
            });
            builder.create().show();
        });

        //imposto i focus per la visualizzazione tv
        final ViewGroup actionBarView = ViewUtils.getActionBarView(getActivity());
        if(actionBarView != null && actionBarView.getChildCount() > 1){
            final View homeButton = actionBarView.getChildAt(1);
            homeButton.setOnFocusChangeListener((view, hasFocus) -> {
                if(hasFocus){
                    if(listView.getVisibility() == View.VISIBLE) {
                        homeButton.setNextFocusDownId(R.id.listview);
                    } else {
                        homeButton.setNextFocusDownId(R.id.fab);
                    }
                }
            });
        }

        mostraServers();

        return v;
    }


    /**
     * Mostra i server salvati
     */
    private void mostraServers(){
        adapter = new ServerFtpAdapter(getContext());
        listView.setAdapter(adapter);
        if(adapter.getCount() == 0){
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            fab.requestFocus();
        } else {
            listView.setVisibility(View.VISIBLE);
            listView.requestFocus();
            emptyView.setVisibility(View.GONE);
        }
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
        final ServerFtp serverFtp = adapter.getItem(position);
        final FtpSession ftpSession = new FtpSession(getContext(), serverFtp);
        ftpSession.connect(ftpClient -> {
            if(ftpClient != null){
                activityMain.setFtpSession(ftpSession);
                activityMain.showFragment(FragmentFtpExplorer.getInstance(null));
            }
        });


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
            menu.add(Menu.NONE, CONTEXT_MENU_MODIFICA, 1, getString(R.string.modifica));
            menu.add(Menu.NONE, CONTEXT_MENU_ELIMINA, 1, getString(R.string.elimina));
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
        final ServerFtp serverFtp = adapter.getItem(info.position);
        int id = item.getItemId();
        switch (id) {
            case CONTEXT_MENU_MODIFICA:
                final DialogDatiServerFtpBuilder builder = new DialogDatiServerFtpBuilder(getContext(), serverFtp, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if(which == DialogInterface.BUTTON_POSITIVE){
                            mostraServers();
                        }
                    }
                });
                builder.create().show();
                return true;
            case CONTEXT_MENU_ELIMINA:
                serverFtp.removeFromPreferences();
                mostraServers();
                return true;
        }
        return false;
    }


}
