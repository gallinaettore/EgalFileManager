package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Interfaccia per gli adapter in cui sarà possibile effettuare la selezione multipla
 */
public interface MultiSelectable {

    /**
     * Verifica se l'adapter è in modalità selezione multipla
     * @return True se l'adapter è in modalità selezione multipla
     */
    boolean modalitaSelezioneMultipla();


    /**
     *  Disattiva la selezione multipla nell'adapter
     */
    void disattivaSelezioneMultipla();


    /**
     * Restituisce il numero di elementi selezionati
     * @return Numero di elementi selezionati
     */
    int numElementiSelezionati();


    /**
     * Seleziona o deseleziona tutti i files presenti nell'adapter
     * @param selezionaTutto True seleziona. False deseleziona.
     */
    void selezionaTutto(boolean selezionaTutto);
}
