package it.Ettore.egalfilemanager.visualizzazione;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.recycler.DatiFilesFtpAdapterGriglia;
import it.Ettore.egalfilemanager.recycler.DatiFilesFtpAdapterLista;
import it.Ettore.egalfilemanager.recycler.DatiFilesFtpAdapterListaPiccola;
import it.Ettore.egalfilemanager.recycler.DatiFilesFtpBaseAdapter;


/**
 * Classe per la gestione della visualizzazione dei files su server FTP
 */
public class VisualizzazioneFilesFtp extends VisualizzazioneBase {
    private final DatiFilesFtpBaseAdapter.OnItemTouchListener onItemTouchListener;


    /**
     *
     * @param recyclerView ReciclerView
     * @param onItemTouchListener Listener da associare alla RecyclerView
     */
    public VisualizzazioneFilesFtp(@NonNull RecyclerView recyclerView, DatiFilesFtpBaseAdapter.OnItemTouchListener onItemTouchListener){
        super(recyclerView);
        this.onItemTouchListener = onItemTouchListener;
    }


    /**
     * Modifica il tipo di visualizzazione aggiornando il layout della ReciclerView
     * @param tipoVisualizzazione Tipo di visualizzazione espresso dalla costanti VISUALIZZAZIONE di questa classe
     */
    @Override
    public void aggiornaVisualizzazione(int tipoVisualizzazione){
        if(tipoVisualizzazione == VISUALIZZAZIONE_ANTEPRIMA){
            //anteprima non disponibile
            tipoVisualizzazione = VISUALIZZAZIONE_LISTA;
        }
        if(tipoVisualizzazione == getVisualizzazioneCorrente()) return;
        List<FtpElement> backupListaFiles = null;
        if(getRecyclerView().getAdapter() != null){
            backupListaFiles = ((DatiFilesFtpBaseAdapter)getRecyclerView().getAdapter()).getListaFiles();
        }
        DatiFilesFtpBaseAdapter adapter;
        switch (tipoVisualizzazione){
            case VISUALIZZAZIONE_LISTA:
                final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                layoutManager.setOrientation(RecyclerView.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager);
                getRecyclerView().addItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesFtpAdapterLista(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_LISTA_PICCOLA:
                final LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
                layoutManager2.setOrientation(RecyclerView.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager2);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesFtpAdapterListaPiccola(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_GRIGLIA:
                int numColonne = calcolaNumColonne();
                final GridLayoutManager layoutManager3 = new GridLayoutManager(getContext(), numColonne, RecyclerView.VERTICAL, false);
                getRecyclerView().setLayoutManager(layoutManager3);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesFtpAdapterGriglia(getContext(), onItemTouchListener);
                break;
            default:
                throw new IllegalArgumentException("Tipo visualizzazione non gestita: " + tipoVisualizzazione);
        }
        getRecyclerView().setAdapter(adapter);
        if(backupListaFiles != null) {
            adapter.update(backupListaFiles);
        }
        setVisualizzazioneCorrente(tipoVisualizzazione);
    }
}
