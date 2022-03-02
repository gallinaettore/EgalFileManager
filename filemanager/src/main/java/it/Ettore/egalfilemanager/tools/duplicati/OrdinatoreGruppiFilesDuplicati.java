package it.Ettore.egalfilemanager.tools.duplicati;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



import java.io.File;
import java.util.Collections;
import java.util.List;

import static it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati.OrdinaPer.NOME;
import static it.Ettore.egalfilemanager.tools.duplicati.OrdinatoreGruppiFilesDuplicati.TipoOrdinamento.CRESCENTE;


/**
 * Classe per ordinare i gruppi di files duplicati
 */
public class OrdinatoreGruppiFilesDuplicati {

    private OrdinaPer ordinaPer = NOME;
    private TipoOrdinamento tipoOrdinamento = CRESCENTE;



    /**
     * Restituisce il metodo di ordinamento settato
     * @return Elemento dell'enum ordina per
     */
    public OrdinaPer getOrdinaPer() {
        return ordinaPer;
    }


    /**
     * Setta il metodo di ordinamento
     * @param ordinaPer Elemento dell'enum ordina per
     */
    public void setOrdinaPer(OrdinaPer ordinaPer) {
        this.ordinaPer = ordinaPer;
    }


    /**
     * Restituisce il tipo di ordinamento crescente o decrescente
     * @return Tipo di ordinamento settato
     */
    public TipoOrdinamento getTipoOrdinamento() {
        return tipoOrdinamento;
    }


    /**
     * Imposta il tipo di ordinamento crescente o decrescente
     * @param tipoOrdinamento Tipo di ordinamento
     */
    public void setTipoOrdinamento(TipoOrdinamento tipoOrdinamento) {
        this.tipoOrdinamento = tipoOrdinamento;
    }



    /**
     * Ordina la lista di gruppi
     * @param gruppi Lista da ordinare
     */
    public void ordina(List<List<String>> gruppi){
        if(gruppi == null) return;
        switch (ordinaPer){
            case NOME:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(gruppi, (g1, g2) -> comparaPerNome(g1, g2));
                } else {
                    Collections.sort(gruppi, (g1, g2) -> -comparaPerNome(g1, g2));
                }
                break;
            case ELEMENTI:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(gruppi, (g1, g2) -> comparaPerNumeroElementi(g1, g2));
                } else {
                    Collections.sort(gruppi, (g1, g2) -> -comparaPerNumeroElementi(g1, g2));
                }
                break;
            case DIMENSIONE:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(gruppi, (g1, g2) -> comparaPerDimensione(g1, g2));
                } else {
                    Collections.sort(gruppi, (g1, g2) -> -comparaPerDimensione(g1, g2));
                }
                break;
            default:
                throw new IllegalArgumentException("Valore dell'enum OrdinaPer non gestito");
        }
    }


    /**
     * Compara per nome del primo file presente all'interno del gruppo
     * @param gruppo1 Gruppo da comparare
     * @param gruppo2 Secondo gruppo da comparare
     * @return ordinamento
     */
    private int comparaPerNome(List<String> gruppo1, List<String> gruppo2){
        try {
            final File file1 = new File(gruppo1.get(0));
            final File file2 = new File(gruppo2.get(0));
            return file1.getName().compareToIgnoreCase(file2.getName());
        } catch (Exception ignored){
            return 0;
        }
    }


    /**
     * Compara i gruppi per numero di files presenti al suo interno
     * @param gruppo1 Gruppo da comparare
     * @param gruppo2 Secondo gruppo da comparare
     * @return ordinamento
     */
    private int comparaPerNumeroElementi(List<String> gruppo1, List<String> gruppo2){
        try {
            return Integer.compare(gruppo1.size(), gruppo2.size());
        } catch (Exception ignored){
            return 0;
        }
    }


    /**
     * Compara per dimensione del primo file presente all'interno del gruppo
     * @param gruppo1 Gruppo da comparare
     * @param gruppo2 Secondo gruppo da comparare
     * @return ordinamento
     */
    private int comparaPerDimensione(List<String> gruppo1, List<String> gruppo2){
        try {
            final File file1 = new File(gruppo1.get(0));
            final File file2 = new File(gruppo2.get(0));
            return Long.compare(file1.length(), file2.length());
        } catch (Exception ignored){
            return 0;
        }
    }




    /* ENUMS */


    public enum OrdinaPer {
        NOME, ELEMENTI, DIMENSIONE
    }


    public enum TipoOrdinamento {
        CRESCENTE, DESCRESCENTE
    }


}
