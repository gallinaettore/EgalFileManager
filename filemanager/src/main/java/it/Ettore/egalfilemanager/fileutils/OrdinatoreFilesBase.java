package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_MOSTRA_NASCOSTI;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ORDINA_FILES_PER;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_ORDINAMENTO_FILES;
import static it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase.OrdinaPer.NOME;
import static it.Ettore.egalfilemanager.fileutils.OrdinatoreFilesBase.TipoOrdinamento.CRESCENTE;


/**
 * Classe generica dell'ordinatore files
 */
public class OrdinatoreFilesBase {
    private SharedPreferences prefs;
    private boolean mostraNascosti;
    private OrdinaPer ordinaPer = NOME;
    private TipoOrdinamento tipoOrdinamento = CRESCENTE;


    /**
     *
     * @param context Context chiamante da cui ricavare le preferences
     */
    protected OrdinatoreFilesBase(@NonNull Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    /**
     *
     * @param prefs Preferences in cui salvare o caricare le impostazioni
     */
    protected OrdinatoreFilesBase(@NonNull SharedPreferences prefs){
        this.prefs = prefs;
    }


    /**
     * Costruttore solo per uso interno. Quando si estende la classe dichiararlo privato.
     */
    protected OrdinatoreFilesBase(){}


    /**
     * Imposta la modalità di visualizzazione: se mostrare i file nascosti o meno
     * @param mostraNascosti True per mostrare tutti i files
     */
    public void setMostraNascosti(boolean mostraNascosti) {
        this.mostraNascosti = mostraNascosti;
    }


    /**
     * Restituisce se la modalità mostra nascosti è attiva
     * @return True se saranno mostrati i files nascosti
     */
    protected boolean mostraNascosti(){
        return this.mostraNascosti;
    }


    /**
     * Restituisce il metodo di ordinamento settato
     * @return Elemento dell'enum ordina per
     */
    public OrdinatoreFiles.OrdinaPer getOrdinaPer() {
        return ordinaPer;
    }


    /**
     * Setta il metodo di ordinamento
     * @param ordinaPer Elemento dell'enum ordina per
     */
    public void setOrdinaPer(OrdinatoreFiles.OrdinaPer ordinaPer) {
        this.ordinaPer = ordinaPer;
    }


    /**
     * Restituisce il tipo di ordinamento crescente o decrescente
     * @return Tipo di ordinamento settato
     */
    public OrdinatoreFiles.TipoOrdinamento getTipoOrdinamento() {
        return tipoOrdinamento;
    }


    /**
     * Imposta il tipo di ordinamento crescente o decrescente
     * @param tipoOrdinamento Tipo di ordinamento
     */
    public void setTipoOrdinamento(OrdinatoreFiles.TipoOrdinamento tipoOrdinamento) {
        this.tipoOrdinamento = tipoOrdinamento;
    }


    /**
     * Salva nelle preferences lo stato corrente dell'ordinamento
     */
    public void salvaStatoOrdinamento(){
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PREF_ORDINA_FILES_PER, ordinaPer.ordinal());
        editor.putInt(KEY_PREF_TIPO_ORDINAMENTO_FILES, tipoOrdinamento.ordinal());
        editor.apply();
    }


    /**
     * Ottiene lo stato dell'ordinamento salvato nelle preferences
     */
    public void ottieniStatoOrdinamento(){
        try {
            int ordinalOrdinaPer = prefs.getInt(KEY_PREF_ORDINA_FILES_PER, 0);
            final OrdinatoreFiles.OrdinaPer ordinaPer = OrdinatoreFiles.OrdinaPer.values()[ordinalOrdinaPer];
            int ordinalTipoOrdinamento = prefs.getInt(KEY_PREF_TIPO_ORDINAMENTO_FILES, 0);
            final OrdinatoreFiles.TipoOrdinamento tipoOrdinamento = OrdinatoreFiles.TipoOrdinamento.values()[ordinalTipoOrdinamento];
            setOrdinaPer(ordinaPer);
            setTipoOrdinamento(tipoOrdinamento);
        } catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }


    /**
     * Ottiene lo stato "mostra files nascosti" impostato
     */
    public void ottieniStatoMostraNascosti(){
        boolean mostraNascosti = prefs.getBoolean(KEY_PREF_MOSTRA_NASCOSTI, false);
        setMostraNascosti(mostraNascosti);
    }





    /* ENUMS */


    public enum OrdinaPer {
        NOME, DIMENSIONE, DATA, TIPO
    }


    public enum TipoOrdinamento {
        CRESCENTE, DESCRESCENTE
    }
}
