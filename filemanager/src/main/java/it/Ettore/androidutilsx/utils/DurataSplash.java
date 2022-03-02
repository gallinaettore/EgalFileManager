package it.Ettore.androidutilsx.utils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Classe per la gestione della durata delle Splash Screen
 */
public class DurataSplash {
    private final long maxDurataSplash;
    private final long onCreateTime;


    /**
     * Costruttore da chiamare come prima operazione nell'onCreate della Splash Screen (viene salvato il timestamp dell'apertura dell'activity)
     * @param maxDurataSplash Durata massima della Splash Screen espressa in millisecondi
     */
    public DurataSplash(long maxDurataSplash){
        this.maxDurataSplash = maxDurataSplash;
        this.onCreateTime = System.currentTimeMillis();
    }


    /**
     * Calcolo il tempo di visualizzazione che ancora resta alla Splash Screen dopo aver effettuato le sue operazioni
     * @return Tempo rimanente in millisecondi
     */
    public long getDelay(){
        final long now = System.currentTimeMillis();
        final long tempoTrascorso = now - onCreateTime;
        if(tempoTrascorso > maxDurataSplash){
            return 0;
        } else {
            return maxDurataSplash - tempoTrascorso;
        }
    }
}
