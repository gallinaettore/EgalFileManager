package it.Ettore.egalfilemanager.tools.ricercafiles;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Classe wrapper che contiene tutti i dati necessari ad effettuare una ricerca di files
 */
public class ParametriRicerca implements Serializable {
    public static final int DIMENSIONI_NON_SETTATA = 0;
    public static final int DIMENSIONI_TUTTE = 1;
    public static final int DIMENSIONI_MINORI = 2;
    public static final int DIMENSIONI_MAGGIORI = 3;
    private String query;
    private List<File> percorsiDiRicerca;
    private boolean ricercaImmagini = true, ricercaVideo = true, ricercaAudio = true, ricercaAltri = true, caseSensistive = true;
    private int tipoDimensione = DIMENSIONI_NON_SETTATA;
    private long dimensione;


    /**
     *
     * @param query Testo da cercare
     * @param percorsiDiRicerca Percorsi in cui cercare i files
     */
    public ParametriRicerca(String query, List<File> percorsiDiRicerca) {
        setQuery(query);
        setPercorsiDiRicerca(percorsiDiRicerca);
    }


    /**
     * Restituisce la query settata
     * @return Testo della query
     */
    public String getQuery() {
        return query;
    }


    /**
     * Setta la query per la ricerca
     * @param query Testo della query
     */
    public void setQuery(String query) {
        this.query = query;
    }


    /**
     * Restituisce i percorsi di ricerca settati
     * @return Lista con i percorsi di ricerca
     */
    public List<File> getPercorsiDiRicerca() {
        return percorsiDiRicerca;
    }


    /**
     * Setta una lista di percorsi in cui ricercare
     * @param percorsiDiRicerca Percorsi in cui ricercare
     */
    public void setPercorsiDiRicerca(List<File> percorsiDiRicerca) {
        this.percorsiDiRicerca = percorsiDiRicerca;
    }


    /**
     * Restituisce la query opportunamente modificata da utilizzare come regex per la verifica corrispondenza dei files
     * @return Testo regex
     */
    public String getRegex(){
        return query.replace(".", "\\.").replace("*", ".+").replace("?", ".{1}").replace("(", "\\(").replace(")", "\\)")
                .replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
    }


    /**
     * Imposta quale tipo di file ricercare
     * @param ricercaImmagini Ricerca le immagini
     * @param ricercaVideo Ricerca i video
     * @param ricercaAudio Ricerca i files audio
     * @param ricercaAltri Ricerca tutte le altre tipologie di files restanti
     */
    public void setTipiFiles(boolean ricercaImmagini, boolean ricercaVideo, boolean ricercaAudio, boolean ricercaAltri){
        this.ricercaImmagini = ricercaImmagini;
        this.ricercaVideo = ricercaVideo;
        this.ricercaAudio = ricercaAudio;
        this.ricercaAltri = ricercaAltri;
    }


    /**
     * Verifica se è stata impostata una ricerca di immagini
     * @return True se è stata impostata una ricerca di immagini
     */
    public boolean ricercaImmagini() {
        return ricercaImmagini;
    }


    /**
     * Verifica se è stata impostata una ricerca di video
     * @return True se è stata impostata una ricerca di video
     */
    public boolean ricercaVideo() {
        return ricercaVideo;
    }


    /**
     * Verifica se è stata impostata una ricerca di files audio
     * @return True se è stata impostata una ricerca di files audio
     */
    public boolean ricercaAudio() {
        return ricercaAudio;
    }


    /**
     * Verifica se è stata impostata una ricerca di altri tipi di files
     * @return True se è stata impostata una ricerca di altri tipi di files
     */
    public boolean ricercaAltri() {
        return ricercaAltri;
    }


    /**
     * Verifica se è possibile ignorare la ricerca per tipo (cerca qualsiasi tipo di file)
     * @return True cerca qualsiasi tipo di file. False è stato impostato uno o più tipi specifici.
     */
    public boolean ricercaTutto(){
        return ricercaImmagini && ricercaVideo && ricercaAudio && ricercaAltri;
    }


    /**
     * Verifica se è stato settato almeno un tipo di file
     * @return True se è stato settato almeno un tipo di file. False se non è stato settato nessun tipo di file (non sarà possibile effettuare la ricerca).
     */
    private boolean tipiFileSettati(){
        return ricercaImmagini || ricercaVideo || ricercaAudio || ricercaAltri;
    }


    /**
     * Verifica che tutti i dati inseriti siano validi per la ricerca
     * @return True se la query è valida, se i percorsi di ricerca sono validi, se è stato settato almeno un tipo di file e se è stato settato un tipo di dimensione.
     */
    public boolean isValid(){
        return query != null && !query.isEmpty() && percorsiDiRicerca != null && !percorsiDiRicerca.isEmpty() && tipiFileSettati() && tipoDimensione != DIMENSIONI_NON_SETTATA;
    }


    /**
     * Verifica se la ricerca deve essere case sentitive
     * @return True se è case sensitive
     */
    public boolean isCaseSensistive(){
        return caseSensistive;
    }


    /**
     * Setta la modalità case sensitive
     * @param caseSensistive True se è case sensitive
     */
    public void setCaseSensistive(boolean caseSensistive) {
        this.caseSensistive = caseSensistive;
    }


    /**
     * Setta la modalità di ricerca per dimensione (tutto, minore di, maggiore di)
     * @param tipoDimensione Utilizzare una delle costanti DIMENSIONI di questa classe
     * @param dimensione Dimensione del file
     * @param indiceUnitaMisura Indice dell'unità di misura (0=bytes, 1=kB, 2=MB, 3=GB...)
     */
    public void setDimensione(int tipoDimensione, long dimensione, int indiceUnitaMisura){
        this.tipoDimensione = tipoDimensione;
        this.dimensione = dimensione * (long)Math.pow(1024, indiceUnitaMisura);
    }


    /**
     * Restituisce la modalità di ricerca per dimensione settata
     * @return Modalità di ricerca per dimensione settata. Utilizzate le costanti DIMENSIONI di questa classe.
     */
    public int getTipoDimensione() {
        return tipoDimensione;
    }


    /**
     * Restituisce la dimensione settata per la ricerca per dimensione
     * @return Dimensione in bytes
     */
    public long getDimensione() {
        return dimensione;
    }
}
