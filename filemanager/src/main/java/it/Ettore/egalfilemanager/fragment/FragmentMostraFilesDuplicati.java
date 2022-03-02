package it.Ettore.egalfilemanager.fragment;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogOrdinaGruppiDuplicatiBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.tools.duplicati.DuplicatiDaCancellareAdapter;
import it.Ettore.egalfilemanager.tools.duplicati.GruppiAdapter;
import it.Ettore.egalfilemanager.tools.duplicati.GruppiListSerializer;
import it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione di files duplicati trovati all'interno del dispositivo
 */
public class FragmentMostraFilesDuplicati extends GeneralFragment implements AdapterView.OnItemClickListener {
    private TextView emptyView;
    private ListView listview;
    private FileManager fileManager;
    private GruppiAdapter gruppiAdapter;
    private ActivityMain activityMain;
    private OrdinatoreGruppiFilesDuplicati ordinatoreGruppi;

    private EliminaHandler eliminaHandler;



    /**
     * Costruttore di default
     */
    public FragmentMostraFilesDuplicati(){}



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_files_duplicati, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.trova_files_duplicati);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        listview = v.findViewById(R.id.listview);
        emptyView = v.findViewById(R.id.empty_view);

        fileManager = new FileManager(getContext());
        fileManager.ottieniStatoRootExplorer();

        final GruppiListSerializer listSerializer = new GruppiListSerializer(getContext());
        final List<List<String>> listaGruppi = listSerializer.deserialize();
        ordinatoreGruppi = new OrdinatoreGruppiFilesDuplicati();
        ordinatoreGruppi.ordina(listaGruppi);
        mostraDati(listaGruppi);

        return v;
    }


    @Override
    public void onStart() {
        super.onStart();
        //rimuovo l'eventuale notifica mostrata (se ancora presente) solo dopo aver visualizzato il fragment e dopo 1 secondo (il tempo di riprodurre il suono)
        new Handler().postDelayed(() -> {
            if(getContext() != null) {
                final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(Costanti.NOTIF_ID_RICERCA_DUPLICATI_TERMINATA);
            }
        }, 1000);
    }


    @Override
    public void onDestroy(){
        if(eliminaHandler != null){
            eliminaHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
    }


    /**
     * Visualizza i dati sul fragment al termine della ricerca
     * @param listaGruppi Lista che contiene gruppi di files duplicati. Ogni gruppo contiene una lista con i path dei files uguali
     */
    private void mostraDati(List<List<String>> listaGruppi){
        if(listaGruppi == null || listaGruppi.isEmpty()){
            emptyView.setVisibility(View.VISIBLE);
            listview.setVisibility(View.GONE);
        } else {
            activityMain.setActionBarTitle(String.format("%s (%s)", getString(R.string.trova_files_duplicati), String.valueOf(listaGruppi.size())));
            emptyView.setVisibility(View.GONE);
            listview.setVisibility(View.VISIBLE);
            listview.requestFocus();
            gruppiAdapter = new GruppiAdapter(getContext(), listaGruppi);
            listview.setAdapter(gruppiAdapter);
            listview.setOnItemClickListener(this);
        }
    }



    /**
     * Chiamato quando si clicca su un elemento della listview
     * @param adapterView .
     * @param view .
     * @param position .
     * @param l .
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
        final List<String> paths = gruppiAdapter.getItem(position);
        final CustomDialogBuilder dBuilder = new CustomDialogBuilder(getContext());
        final View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_cancella_files_duplicati, null);
        final ListView dialogListView = dialogView.findViewById(R.id.listview);
        final DuplicatiDaCancellareAdapter duplicatiDaCancellareAdapter = new DuplicatiDaCancellareAdapter(getContext(), paths);
        dialogListView.setAdapter(duplicatiDaCancellareAdapter);
        dialogListView.setItemsCanFocus(true);
        dBuilder.setView(dialogView);
        dBuilder.setPositiveButton(R.string.cancella_selezionati, (dialogInterface, i) -> {
            final List<File> daCancellare = duplicatiDaCancellareAdapter.getSelectedFiles();
            if(daCancellare.isEmpty()){
                ColoredToast.makeText(getContext(), R.string.nessun_elemento_selezionato, Toast.LENGTH_LONG).show();
            } else {
                eliminaHandler = new EliminaHandler(getActivity(), (boolean success, List<File> deletedFiles) -> {
                    //rimuovo i files eliminati dalla visualizzazione dell'adapter
                    gruppiAdapter.removeFilesAt(position, daCancellare);
                    activityMain.setActionBarTitle(String.format("%s (%s)", getString(R.string.trova_files_duplicati), String.valueOf(gruppiAdapter.getCount())));
                    if (gruppiAdapter.getCount() == 0) {
                        emptyView.setVisibility(View.VISIBLE);
                        listview.setVisibility(View.GONE);
                    }
                });
                fileManager.elimina(daCancellare, eliminaHandler);
            }
        });
        dBuilder.setNegativeButton(android.R.string.cancel, null);
        dBuilder.create().show();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_files_duplicati, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aggiorna:
                activityMain.showFragment(new FragmentAvviaRicercaDuplicati());
                return true;
            case R.id.ordina:
                if(gruppiAdapter != null && gruppiAdapter.getGruppi() != null && !gruppiAdapter.getGruppi().isEmpty()) {
                    final DialogOrdinaGruppiDuplicatiBuilder ordinaBuilder = new DialogOrdinaGruppiDuplicatiBuilder(getContext(), ordinatoreGruppi, (dialogInterface, i) -> {
                        ordinatoreGruppi.ordina(gruppiAdapter.getGruppi());
                        gruppiAdapter.notifyDataSetChanged();
                        listview.scrollTo(0, 0);
                    });
                    ordinaBuilder.create().show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //se l'adapter è stato modificato lo salvo su file in modo da recuperare i nuovi dati all'eventuale rotazione del dispositivo
        if(gruppiAdapter != null && gruppiAdapter.adapterHasChanged()) {
            final GruppiListSerializer listSerializer = new GruppiListSerializer(getContext());
            listSerializer.serialize(gruppiAdapter.getGruppi());
        }
    }
}
