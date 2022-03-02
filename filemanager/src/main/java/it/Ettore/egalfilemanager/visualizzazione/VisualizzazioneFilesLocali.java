package it.Ettore.egalfilemanager.visualizzazione;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliAdapterAnteprima;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliAdapterGriglia;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliAdapterLista;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliAdapterListaPiccola;
import it.Ettore.egalfilemanager.recycler.DatiFilesLocaliBaseAdapter;



/**
 * Classe per la gestione della visualizzazione dei files sul dispositivo
 */
public class VisualizzazioneFilesLocali extends VisualizzazioneBase {
    private final DatiFilesLocaliBaseAdapter.OnItemTouchListener onItemTouchListener;


    /**
     *
     * @param recyclerView ReciclerView
     * @param onItemTouchListener Listener da associare alla RecyclerView
     */
    public VisualizzazioneFilesLocali(@NonNull RecyclerView recyclerView, DatiFilesLocaliBaseAdapter.OnItemTouchListener onItemTouchListener){
        super(recyclerView);
        this.onItemTouchListener = onItemTouchListener;
    }


    /**
     * Modifica il tipo di visualizzazione aggiornando il layout della ReciclerView
     * @param tipoVisualizzazione Tipo di visualizzazione espresso dalla costanti VISUALIZZAZIONE di questa classe
     */
    @Override
    public void aggiornaVisualizzazione(int tipoVisualizzazione) {
        if(tipoVisualizzazione == getVisualizzazioneCorrente()) return;
        List<File> backupListaFiles = null;
        if(getRecyclerView().getAdapter() != null){
            backupListaFiles = ((DatiFilesLocaliBaseAdapter)getRecyclerView().getAdapter()).getListaFiles();
        }
        DatiFilesLocaliBaseAdapter adapter;
        switch (tipoVisualizzazione){
            case VISUALIZZAZIONE_LISTA:
                final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                layoutManager.setOrientation(RecyclerView.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager);
                getRecyclerView().addItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLocaliAdapterLista(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_LISTA_PICCOLA:
                final LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
                layoutManager2.setOrientation(LinearLayoutManager.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager2);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLocaliAdapterListaPiccola(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_GRIGLIA:
                int numColonne = calcolaNumColonne();
                final GridLayoutManager layoutManager3 = new GridLayoutManager(getContext(), numColonne, RecyclerView.VERTICAL, false);
                getRecyclerView().setLayoutManager(layoutManager3);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLocaliAdapterGriglia(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_ANTEPRIMA:
                numColonne = calcolaNumColonne() / 2;
                final GridLayoutManager layoutManager4 = new GridLayoutManager(getContext(), numColonne, RecyclerView.VERTICAL, false);
                getRecyclerView().setLayoutManager(layoutManager4);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLocaliAdapterAnteprima(getContext(), onItemTouchListener);
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
