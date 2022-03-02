package it.Ettore.egalfilemanager.visualizzazione;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;


/**
 * Classe generica per la gestione della visualizzazione dei files
 */
public abstract class VisualizzazioneBase {
    public static final int VISUALIZZAZIONE_LISTA = 0;
    public static final int VISUALIZZAZIONE_LISTA_PICCOLA = 1;
    public static final int VISUALIZZAZIONE_GRIGLIA = 2;
    public static final int VISUALIZZAZIONE_ANTEPRIMA = 3;
    private int visualizzazioneCorrente = -1;
    private final RecyclerView recyclerView;
    private final LineItemDecoration lineItemDecoration;



    /**
     *
     * @param recyclerView ReciclerView in cui saranno visualizzati i file
     */
    protected VisualizzazioneBase(@NonNull RecyclerView recyclerView){
        this.recyclerView = recyclerView;
        this.lineItemDecoration = new LineItemDecoration();
    }



    /**
     * Restituisce il tipo di visualizzazione corrente
     * @return Tipo di visualizzazione espresso dalla costanti VISUALIZZAZIONE di questa classe
     */
    public int getVisualizzazioneCorrente() {
        return visualizzazioneCorrente;
    }



    /**
     * Modifica il tipo di visualizzazione aggiornando il layout della ReciclerView
     * @param tipoVisualizzazione Tipo di visualizzazione espresso dalla costanti VISUALIZZAZIONE di questa classe
     */
    public abstract void aggiornaVisualizzazione(int tipoVisualizzazione);


    /**
     * Restituisce la RecyclerView impostata
     * @return RecyclerView impostata
     */
    protected RecyclerView getRecyclerView() {
        return recyclerView;
    }


    /**
     * Restituisce il line item decoration creato per la visualizzazione della riga nella listview
     * @return Line item decoration
     */
    protected LineItemDecoration getLineItemDecoration() {
        return lineItemDecoration;
    }


    /**
     * Restituisce il Context passato alla RecyclerView
     * @return Context
     */
    protected Context getContext(){
        return getRecyclerView().getContext();
    }


    /**
     * Imposta la visualizzazione corrente
     * @param visualizzazioneCorrente Intero da scegliere tra le variabili VISUALIZZAZIONE di questa classe
     */
    protected void setVisualizzazioneCorrente(int visualizzazioneCorrente){
        this.visualizzazioneCorrente = visualizzazioneCorrente;
    }


    /**
     * Calcola il numero delle colonne da utilizzare nella visualizzazione anteprima
     * @return Numero di colonne di visualizzare
     */
    protected int calcolaNumColonne(){
        int density = recyclerView.getContext().getResources().getDisplayMetrics().densityDpi;
        int colonneDefault;
        switch(density) {
            case DisplayMetrics.DENSITY_LOW:
            case DisplayMetrics.DENSITY_MEDIUM:
            case DisplayMetrics.DENSITY_HIGH:
                colonneDefault = 4;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_TV:
                colonneDefault = 5;
                break;
            default:
                colonneDefault = 5;
        }
        float maxColonne = (float)colonneDefault * moltiplicatoreDimensione() * moltiplicatoreOrientamento();
        return Math.round(maxColonne);
    }



    /**
     * Numero utilizzato come moltiplicatore per adattare meglio il numero di colonne alla grandezza del display
     * @return Moltiplicatore
     */
    private float moltiplicatoreDimensione(){
        final Configuration conf = recyclerView.getContext().getResources().getConfiguration();
        int screenLayout = conf.screenLayout;
        if((screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL){
            return 0.75f;
        } else if((screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL){
            return 1.0f;
        } else if((screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE){
            return 1.5f;
        } else if((screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE){
            return 1.8f;
        } else {
            return 1;
        }
    }



    /**
     * Numero utilizzato come moltiplicatore per adattare meglio il numero di colonne all'orientamento del display
     * @return Moltiplicatore
     */
    private float moltiplicatoreOrientamento() {
        final Configuration conf = recyclerView.getContext().getResources().getConfiguration();
        int orientation = conf.orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return 1.0f;
            case Configuration.ORIENTATION_LANDSCAPE:
                return 1.6f;
            default:
                return 1.0f;
        }
    }


    /**
     * Restituisce il nome del tipo di visualizzazione da usare su crashlytics
     * @param tipoVisualizzazione TipoVisualizzazione
     * @return Nome comprensibile
     */
    public static String getNomeTipoVisualizzazione(int tipoVisualizzazione){
        switch (tipoVisualizzazione){
            case VISUALIZZAZIONE_LISTA:
                return "lista";
            case VISUALIZZAZIONE_LISTA_PICCOLA:
                return "lista piccola";
            case VISUALIZZAZIONE_GRIGLIA:
                return "griglia";
            case VISUALIZZAZIONE_ANTEPRIMA:
                return "anteprima";
            default:
                return "";
        }
    }
}
