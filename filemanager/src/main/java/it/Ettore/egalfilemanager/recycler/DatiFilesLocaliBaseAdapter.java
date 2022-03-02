package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.ThemeUtils;


/**
 *  Adapter di base per la visualizzazione dei files all'interno della ReciclerView
 */
public abstract class DatiFilesLocaliBaseAdapter extends RecyclerView.Adapter<DatiFilesLocaliBaseViewHolder> implements ListFilter.Filterable, MultiSelectable {
    private final String[] arrayBytes;
    private List<File> listaFiles;
    private final OnItemTouchListener listener;
    private boolean selezioneMultipla, mostraAnteprime;
    private final List<File> selezionati;
    private final SharedPreferences prefs;
    private List<File> backupListaCompleta;
    private Map<String, Long> dimensioniFiles; //creo una mappa per recuperare in seguito le dimensioni dei files e vedere se è il file è stato sostituito con uno nuovo ma con lo stesso nome
    private ThemeUtils themeUtils;

    /**
     *
     * @param context Activity in cui sarà visualizzata la ReciclerView
     * @param listener Listener per la gestione dei tocchi (lungo o corto) di un'item sulla RecyclerView
     */
    DatiFilesLocaliBaseAdapter(@NonNull Context context, OnItemTouchListener listener){
        //setHasStableIds(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.listaFiles = new ArrayList<>();
        this.listener = listener;
        this.selezionati = new ArrayList<>();
        this.arrayBytes = MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS);
        this.themeUtils = new ThemeUtils(context);
    }


    /**
     * Restituisce il numero di elementi all'interno
     * @return Numero di elementi
     */
    @Override
    public int getItemCount() {
        return listaFiles.size();
    }


    /**
     *  Metodo chiamato in fase di creazione del ViewHolder (effettua il bind completo)
     *
     * @param holder ViewHolder in cui verrano visualizzati i dati
     * @param position Posizione all'interno dell'adapter
     */
    @Override
    public void onBindViewHolder(@NonNull DatiFilesLocaliBaseViewHolder holder, int position) {
        holder.bind(position);
    }



    /**
     * Metodo chiamato per il bind parziale del ViewHolder (quando si deve modificare soltanto una parte del ViewHolder)
     * (il bind completo richiederebbe il caricamento delle anteprime)
     *
     * @param viewHolder ViewHolder in cui verrano visualizzati i dati
     * @param position Posizione all'interno dell'adapter
     * @param payloads Il payload è un oggetto in cui sono presenti delle variabili utili per la modifica del ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull DatiFilesLocaliBaseViewHolder viewHolder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            // Perform a full update
            onBindViewHolder(viewHolder, position);
        } else {
            // Perform a partial update
            for (Object payload : payloads) {
                if (payload instanceof PayloadSelezioneMultipla) {
                    //in questo caso utilizzo solo un oggetto booleano nel payload
                    viewHolder.abilitaSelezioneMultipla(((PayloadSelezioneMultipla)payload).modalitaSelezioneMultipla);
                } else if (payload instanceof  PayloadSelezionaTutto){
                    viewHolder.selezionaTutto(((PayloadSelezionaTutto)payload).selezionaTutto);
                }
            }

        }
        //ad ogni aggiornamento (parziale o totale) controllo la corretta visualizzazione visible/gome e checked/unchecked delle checkbox
        viewHolder.modificaStatoCheckbox(this, position);
    }



    /**
     *  Aggiorna la lista corrente con una nuova (solo se è diversa).
     *  Dopo l'aggiornamento disattivare la modalità filtro.
     *
     * @param listaFiles Nuova lista di files
     * @return True se la nuova lista è diversa e la ReciclerView viene aggiornata
     */
    public boolean update(List<File> listaFiles){
        mostraAnteprime = prefs.getBoolean(Costanti.KEY_PREF_MOSTRA_ANTEPRIME, true);
        boolean uguale = listaUguale(listaFiles);

        //salvo le dimensioni dei files per recuperarle in seguito nell'adapter
        //(anche se la nuova lista files è uguale alla precedente le dimensioni potrebbero cambiare perchè il file viene sovrascritto)
        if(!uguale){
            this.dimensioniFiles = new HashMap<>(listaFiles.size()); //istanzio solo se la lista è diversa, per la lista uguale mi interessa ricavare il valore precedente
        }
        boolean dimensioniDiverse = false;
        for(File file : listaFiles){
            long fileSize = file.length();
            Long valorePrecedente = dimensioniFiles.put(file.getPath(), fileSize);
            if(valorePrecedente != null && valorePrecedente != fileSize){
                dimensioniDiverse = true;
            }
        }
        if(dimensioniDiverse){
            uguale = false;
        }

        if(!uguale) {
            this.listaFiles = listaFiles;
            this.backupListaCompleta = null;
            notifyDataSetChanged();
            //notifyItemRangeChanged(0, getItemCount());
        }
        return !uguale;
    }



    /**
     * Verifica se la nuova lista è uguale a quella precedentemente impostata nell'adapter
     * @param nuovaLista Nuova lista di files
     * @return True se la nuova lista contiene gli stessi elementi già presenti nell'adapter
     */
    private boolean listaUguale(List<File> nuovaLista){
        if(listaFiles == null || nuovaLista == null || listaFiles.size() != nuovaLista.size()){
            return false;
        }
        for(int i=0; i < listaFiles.size(); i++){
            if(!listaFiles.get(i).getAbsolutePath().equals(nuovaLista.get(i).getAbsolutePath())){
                return false;
            }
        }
        return true;
    }




    /**
     * Verifica se l'adapter è in modalità selezione multipla
     * @return True se l'adapter è in modalità selezione multipla
     */
    @Override
    public boolean modalitaSelezioneMultipla(){
        return this.selezioneMultipla;
    }



    /**
     *  Disattiva la selezione multipla nell'adapter
     */
    @Override
    public void disattivaSelezioneMultipla(){
        this.selezioneMultipla = false;
        this.selezionati.clear();
        notificaSelezionaTutto(false);
        notificaModalitaSelezioneMultipla();
    }


    /**
     * Restituisce gli elementi selezionati
     * @return Lista dei files selezionati
     */
    public List<File> getElementiSelezionati(){
        return this.selezionati;
    }


    /**
     * Restituisce il numero di elementi selezionati
     * @return Numero di elementi selezionati
     */
    @Override
    public int numElementiSelezionati(){
        return selezionati.size();
    }



    /**
     * Seleziona o deseleziona tutti i files presenti nell'adapter
     * @param selezionaTutto True seleziona. False deseleziona.
     */
    @Override
    public void selezionaTutto(boolean selezionaTutto){
        if(selezionaTutto){
            this.selezionati.clear();
            this.selezionati.addAll(listaFiles);
        } else {
            this.selezionati.clear();
        }
        notificaSelezionaTutto(selezionaTutto);
    }


    /**
     * Lista di files da visualizzare nella RecyclerView
     * @return lista di files
     */
    public List<File> getListaFiles(){
        return this.listaFiles;
    }



    /**
     * Verifica se è impostata la modalità mostra anteprime
     * @return True se è impostata la modalità mostra anteprime
     */
    boolean mostraAnteprime(){
        return this.mostraAnteprime;
    }



    /**
     * Listener per la gestione dei tocchi sugli items
     * @return Listener di gestione tocchi
     */
    OnItemTouchListener getListener(){
        return this.listener;
    }



    /**
     *  Disattiva la modalità selezione multipla se è già attivata, la attiva se invece è disattivata.
     */
    void toggleSelezioneMultipla(){
        selezioneMultipla = !selezioneMultipla;
    }



    /**
     * Array contenti le unità di misura dei bytes localizzati (B, KB, MB, GB...)
     * @return Array con le unità di misura
     */
    String[] getArrayBytes() {
        return arrayBytes;
    }



    /**
     *  Abilitando/disabilitando la selezione multipla occorre notificare di modificare solo una parte della view holder
     *  (visualizzare o no la checkbox)
     */
    void notificaModalitaSelezioneMultipla(){
        final PayloadSelezioneMultipla payload = new PayloadSelezioneMultipla();
        payload.modalitaSelezioneMultipla = selezioneMultipla;
        notifyItemRangeChanged(0, getItemCount(), payload);
    }


    /**
     * Notifica all'adapter di selezionare tutti gli elementi
     * @param selezionaTutto True per selezionare. False per deselezionare.
     */
    private void notificaSelezionaTutto(boolean selezionaTutto){
        final PayloadSelezionaTutto payload = new PayloadSelezionaTutto();
        payload.selezionaTutto = selezionaTutto;
        notifyItemRangeChanged(0, getItemCount(), payload);
    }


    /**
     * Abilita/disabilita la modalità filtro files nell'adapter
     * @param filterMode True per abilitare la modalità filtro
     */
    @Override
    public void setFilterMode(boolean filterMode){
        if(filterMode){
            backupListaCompleta = new ArrayList<>(listaFiles);
        } else {
            if(backupListaCompleta != null) {
                listaFiles = new ArrayList<>(backupListaCompleta);
                backupListaCompleta = null;
            }
        }
    }



    /**
     * Mostra solo i files il cui nome che contiene la strimga query
     * @param query Stringa di filtraggio files
     */
    @Override
    public void filter(String query){
        if(backupListaCompleta == null) return;
        final List<File> listaFiltrata;
        if(query == null || query.isEmpty()){
            listaFiltrata = new ArrayList<>(backupListaCompleta);
        } else {
            listaFiltrata = new ArrayList<>();
            for(File file : backupListaCompleta){
                if(file.getName().toLowerCase().contains(query.toLowerCase())){
                    listaFiltrata.add(file);
                }
            }
        }
        listaFiles = listaFiltrata;
        notifyDataSetChanged();
    }



    /**
     * Verifica se l'adapter è vuoto
     * @return True se l'adapter non contiene files
     */
    public boolean isEmpty(){
        return getItemCount() == 0;
    }


    /**
     * Theme Utils usato per la colorazione del testo
     * @return Theme utils
     */
    ThemeUtils getThemeUtils() {
        return themeUtils;
    }


    /**
     *  Payload che contiene il booleano per la selezione multipla
     */
    private static class PayloadSelezioneMultipla {
        boolean modalitaSelezioneMultipla;
    }



    /**
     *  Payload che contiene il booleano per selezionare/deselezionare tutti i files
     */
    private static class PayloadSelezionaTutto {
        boolean selezionaTutto;
    }


    /**
     *  Interfaccia per la gestione dei tocchi sugli items
     */
    public interface OnItemTouchListener {
        void onItemClick(File file);
        void onItemLongClick(File file);
    }
}
