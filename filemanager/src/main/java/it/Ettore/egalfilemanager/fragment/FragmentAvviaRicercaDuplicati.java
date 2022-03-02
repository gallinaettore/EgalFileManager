package it.Ettore.egalfilemanager.fragment;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import it.Ettore.androidutilsx.ext.ViewsExtKt;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.tools.duplicati.CercaFilesDuplicatiService;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la gestione della ricerca dei files duplicati
 */
public class FragmentAvviaRicercaDuplicati extends GeneralFragment {
    private Button buttonRicerca, buttonAnnulla;
    private TextView textViewTrovati;
    private LinearLayout cercaLayout, progressLayout;
    private Handler handler;
    private ActivityMain activityMain;
    private boolean fragmentIsVisible;


    /**
     * Cotruttore di default (necessario)
     */
    public FragmentAvviaRicercaDuplicati(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        handler = new MostraProgressoHandler(this);

        final View v = inflater.inflate(R.layout.fragment_avvia_ricerca_duplicati, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.trova_files_duplicati);

        cercaLayout = v.findViewById(R.id.cerca_layout);
        progressLayout = v.findViewById(R.id.progress_layout);
        textViewTrovati = v.findViewById(R.id.textview_trovati);

        final Spinner spinnerStorage = v.findViewById(R.id.spinner_storage);
        final List<HomeItem> storages = new HomeNavigationManager(getActivity()).listaItemsSdCards();
        final String[] storageNames = new String[storages.size()+1];
        storageNames[0] = getString(R.string.tutti);
        for(int i=0; i < storages.size(); i++){
            storageNames[i+1] = storages.get(i).titolo;
        }
        ViewsExtKt.popola(spinnerStorage, storageNames);

        buttonRicerca = v.findViewById(R.id.button_avvia_ricerca);
        buttonRicerca.setOnClickListener(view -> {
            final SerializableFileList serializableFileList = new SerializableFileList();
            if(spinnerStorage.getSelectedItemPosition() == 0){
                //tutti gli storages
                for (HomeItem storage : storages){
                    serializableFileList.addFile(storage.startDirectory);
                }
            } else {
                //singolo storage
                final HomeItem storage = storages.get(spinnerStorage.getSelectedItemPosition()-1);
                serializableFileList.addFile(storage.startDirectory);
            }
            final Intent serviceIntent = CercaFilesDuplicatiService.createStartIntent(getContext(), serializableFileList, handler);
            ContextCompat.startForegroundService(activityMain, serviceIntent);
            viewModalitaRicerca(true);
            aggiorna(0, false, false);
        });


        buttonAnnulla = v.findViewById(R.id.button_annulla);
        buttonAnnulla.setOnClickListener(view -> {
            activityMain.startService(CercaFilesDuplicatiService.createStopIntent(getContext()));
            viewModalitaRicerca(false);
        });


        return v;
    }


    @Override
    public void onStart() {
        super.onStart();
        fragmentIsVisible = true;
        viewModalitaRicerca(CercaFilesDuplicatiService.isRunning());
        new Handler().postDelayed(() ->{
            if(buttonRicerca.getVisibility() == View.VISIBLE){
                buttonRicerca.requestFocus();
            } else {
                buttonAnnulla.requestFocus();
            }
        }, 200);
    }


    @Override
    public void onStop() {
        super.onStop();
        fragmentIsVisible = false;
    }


    /**
     * Modifica la visualizzazione delle views
     * @param modRicerca True per la fase di ricerca, mostra la progressbar. False per fase iniziale, mostra il pulsante di avvio
     */
    private void viewModalitaRicerca(boolean modRicerca){
        if(modRicerca){
            progressLayout.setVisibility(View.VISIBLE);
            buttonAnnulla.setVisibility(View.VISIBLE);
            cercaLayout.setVisibility(View.GONE);
        } else {
            progressLayout.setVisibility(View.GONE);
            buttonAnnulla.setVisibility(View.GONE);
            cercaLayout.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Aggiorna la view dopo aver ottenuto i dati dal service
     * @param numTrovati Numero di gruppi di files trovati fino ad ora
     * @param finish True se la ricerca è terminata, False se la ricerca sta ancora continuando
     * @param canceled True se la ricerca è stata annullata
     */
    private void aggiorna(int numTrovati, boolean finish, boolean canceled){
        if(canceled){
            //ricerca annullata
            viewModalitaRicerca(false);
        } else {
            //aggiornamento dati ricerca
            this.textViewTrovati.setText(getString(R.string.duplicati_trovati, String.valueOf(numTrovati)));
            if (finish) {
                //ricerca terminata
                if(fragmentIsVisible){
                    //rimuovo la notifica mostrata (ed evito il suono) se il fragment è visibile
                    final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(Costanti.NOTIF_ID_RICERCA_DUPLICATI_TERMINATA);
                }
                activityMain.showFragment(new FragmentMostraFilesDuplicati());
            }
        }
    }






    /**
     *  Handler per la ricezione di messaggi da parte del service
     */
    private static class MostraProgressoHandler extends Handler {
        private final WeakReference<FragmentAvviaRicercaDuplicati> fragment;


        /**
         * @param fragment Fragment corrente
         */
        private MostraProgressoHandler(@NonNull FragmentAvviaRicercaDuplicati fragment){
            this.fragment = new WeakReference<>(fragment);
        }


        /**
         * Chiamanto quando si riceve un messaggio
         * @param msg Messaggio ricevuto
         */
        @Override
        public void handleMessage(Message msg) {
            final Bundle data = msg.getData();
            int duplicatiTrovati = data.getInt(CercaFilesDuplicatiService.KEYBUNDLE_DUPLICATI_TROVATI);
            boolean ricercaFinita = data.getBoolean(CercaFilesDuplicatiService.KEYBUNDLE_FINISH, false);
            boolean canceled = data.getBoolean(CercaFilesDuplicatiService.KEYBUNDLE_CANCELED, false);
            if(fragment.get() != null){
                try {
                    fragment.get().aggiorna(duplicatiTrovati, ricercaFinita, canceled);
                } catch (IllegalStateException ignored){} //fragment non più presente
            }
        }
    }
}
