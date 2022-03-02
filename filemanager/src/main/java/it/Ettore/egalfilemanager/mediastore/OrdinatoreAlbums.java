package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ORDINA_ALBUMS_PER;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TIPO_ORDINAMENTO_ALBUMS;
import static it.Ettore.egalfilemanager.mediastore.OrdinatoreAlbums.OrdinaPer.NOME;
import static it.Ettore.egalfilemanager.mediastore.OrdinatoreAlbums.TipoOrdinamento.CRESCENTE;


/**
 * Classe di utilità per ordinare gli albums
 */
public class OrdinatoreAlbums {
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
     * Salva nelle preferences lo stato corrente dell'ordinamento
     */
    public void salvaStatoOrdinamento(@NonNull SharedPreferences prefs){
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PREF_ORDINA_ALBUMS_PER, ordinaPer.ordinal());
        editor.putInt(KEY_PREF_TIPO_ORDINAMENTO_ALBUMS, tipoOrdinamento.ordinal());
        editor.apply();
    }


    /**
     * Ottiene lo stato dell'ordinamento salvato nelle preferences
     */
    public void ottieniStatoOrdinamento(@NonNull SharedPreferences prefs){
        try {
            int ordinalOrdinaPer = prefs.getInt(KEY_PREF_ORDINA_ALBUMS_PER, 0);
            final OrdinaPer ordinaPer = OrdinaPer.values()[ordinalOrdinaPer];
            int ordinalTipoOrdinamento = prefs.getInt(KEY_PREF_TIPO_ORDINAMENTO_ALBUMS, 0);
            final TipoOrdinamento tipoOrdinamento = TipoOrdinamento.values()[ordinalTipoOrdinamento];
            setOrdinaPer(ordinaPer);
            setTipoOrdinamento(tipoOrdinamento);
        } catch (ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }



    /**
     * Ordina la lista di album
     * @param albums Lista da ordinare
     * @return Lista ordinata
     */
    public List<Album> ordinaAlbums(List<Album> albums){
        if(albums == null) return null;
        switch (ordinaPer){
            case NOME:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(albums, (a1, a2) -> comparaPerNome(a1, a2));
                    return albums;
                } else {
                    Collections.sort(albums, (a1, a2) -> -comparaPerNome(a1, a2));
                    return albums;
                }
            case ELEMENTI:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(albums, (a1, a2) -> comparaPerNumeroElementi(a1, a2));
                    return albums;
                } else {
                    Collections.sort(albums, (a1, a2) -> -comparaPerNumeroElementi(a1, a2));
                    return albums;
                }
            case PERCORSO:
                if(tipoOrdinamento == CRESCENTE){
                    Collections.sort(albums, (a1, a2) -> comparaPerPercorso(a1, a2));
                    return albums;
                } else {
                    Collections.sort(albums, (a1, a2) -> -comparaPerPercorso(a1, a2));
                    return albums;
                }
            default:
                throw new IllegalArgumentException("Valore dell'enum OrdinaPer non gestito");
        }
    }



    /**
     * Compara i files per nome in ordine crescente. Se i nomi sono uguali compara per percorso.
     */
    private int comparaPerNome(Album album1, Album album2) {
        if(album1.getNome() == null || album2.getNome() == null) return 0;
        int result = album1.getNome().toLowerCase().compareTo(album2.getNome().toLowerCase());
        if(result == 0){
            if(album1.getPath() != null && album2 != null && album2.getPath() != null) {
                return album1.getPath().compareTo(album2.getPath());
            } else {
                return result;
            }
        } else {
            return result;
        }
    }


    /**
     * Compara i files per numero di elementi in ordine crescente. Se il numero di elementi è uguale compara per nome.
     */
    private int comparaPerNumeroElementi(Album album1, Album album2) {
        int result = Integer.compare(album1.size(), album2.size());
        if(result == 0){
            if(album1.getNome() == null || album2.getNome() == null){
                return 0;
            } else {
                return album1.getNome().toLowerCase().compareTo(album2.getNome().toLowerCase());
            }
        } else {
            return result;
        }
    }



    /**
     * Compara i files per percorso in ordine crescente.
     */
    private int comparaPerPercorso(Album album1, Album album2) {
        if(album1.getPath() != null && album2 != null && album2.getPath() != null) {
            return album1.getPath().compareTo(album2.getPath());
        } else {
            return 0;
        }
    }




    /* ENUMS */


    public enum OrdinaPer {
        NOME, ELEMENTI, PERCORSO
    }


    public enum TipoOrdinamento {
        CRESCENTE, DESCRESCENTE
    }
}
