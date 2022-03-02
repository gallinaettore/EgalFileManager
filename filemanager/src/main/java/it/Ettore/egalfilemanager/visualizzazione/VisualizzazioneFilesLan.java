package it.Ettore.egalfilemanager.visualizzazione;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.recycler.DatiFilesLanAdapterGriglia;
import it.Ettore.egalfilemanager.recycler.DatiFilesLanAdapterLista;
import it.Ettore.egalfilemanager.recycler.DatiFilesLanAdapterListaPiccola;
import it.Ettore.egalfilemanager.recycler.DatiFilesLanBaseAdapter;


/**
 * Classe per la gestione della visualizzazione dei files sulla rete locale
 */
public class VisualizzazioneFilesLan extends VisualizzazioneBase {
    private final DatiFilesLanBaseAdapter.OnItemTouchListener onItemTouchListener;


    /**
     *
     * @param recyclerView ReciclerView
     * @param onItemTouchListener Listener da associare alla RecyclerView
     */
    public VisualizzazioneFilesLan(@NonNull RecyclerView recyclerView, DatiFilesLanBaseAdapter.OnItemTouchListener onItemTouchListener){
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
        /*List<SmbFile> backupListaFiles = null;
        if(getRecyclerView().getAdapter() != null){
            backupListaFiles = ((DatiFilesLanBaseAdapter)getRecyclerView().getAdapter()).getListaFiles();
        }*/
        DatiFilesLanBaseAdapter adapter;
        switch (tipoVisualizzazione){
            case VISUALIZZAZIONE_LISTA:
                final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                layoutManager.setOrientation(RecyclerView.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager);
                getRecyclerView().addItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLanAdapterLista(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_LISTA_PICCOLA:
                final LinearLayoutManager layoutManager2 = new LinearLayoutManager(getContext());
                layoutManager2.setOrientation(RecyclerView.VERTICAL);
                getRecyclerView().setLayoutManager(layoutManager2);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLanAdapterListaPiccola(getContext(), onItemTouchListener);
                break;
            case VISUALIZZAZIONE_GRIGLIA:
                int numColonne = calcolaNumColonne();
                final GridLayoutManager layoutManager3 = new GridLayoutManager(getContext(), numColonne, RecyclerView.VERTICAL, false);
                getRecyclerView().setLayoutManager(layoutManager3);
                getRecyclerView().removeItemDecoration(getLineItemDecoration());
                adapter = new DatiFilesLanAdapterGriglia(getContext(), onItemTouchListener);
                break;
            default:
                throw new IllegalArgumentException("Tipo visualizzazione non gestita: " + tipoVisualizzazione);
        }
        getRecyclerView().setAdapter(adapter);
        //non aggiorno l'adapter perche l'analisi di dimensione files potrebbe creare NetworkOnMainThreadException
        //meglio fare un ls() subito dopo

        /*if(backupListaFiles != null) {
            adapter.update(backupListaFiles);
        }*/
        setVisualizzazioneCorrente(tipoVisualizzazione);
    }
}
