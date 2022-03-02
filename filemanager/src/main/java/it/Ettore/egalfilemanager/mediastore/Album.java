package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/




import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Classe che indentifica una raccolta di elementi (Album di immagini, video, audio...)
 */
public class Album {
    private final long id;
    private final int mediaType;
    private final String nome;
    private final List<String> elementi;


    /**
     *
     * @param id Id dell'album
     * @param nome Nome dell'album
     * @param mediaType Costante della classe MediaUtils
     */
    public Album(long id, String nome, int mediaType){
        this.id = id;
        this.nome = nome;
        this.mediaType = mediaType;
        this.elementi = new ArrayList<>();
    }


    /**
     * Aggiunge un elemento all'album
     * @param pathElemento Path dell'elemento da aggiungere
     */
    public void addElement(String pathElemento){
        elementi.add(pathElemento);
    }


    /**
     * Aggiunge una lista di elementi all'album
     * @param pathElementi Lista di path di elementi
     */
    public void addElements(List<String> pathElementi){
        elementi.addAll(pathElementi);
    }


    /**
     * Restituisce il nome dell'album
     * @return Nome dell'album
     */
    public String getNome() {
        if(nome != null) {
            return nome;
        } else {
            return "";
        }
    }


    /**
     * Restituisce il path dell'album, ricavato dal path del primo file
     * @return Path dell'album per gli album di immagini, video e audio. Null per altri tipi di album perchè al loro interno possono contenere files che hanno path diversi
     */
    public String getPath(){
        if(mediaType == MediaUtils.MEDIA_TYPE_IMAGE || mediaType == MediaUtils.MEDIA_TYPE_VIDEO || mediaType == MediaUtils.MEDIA_TYPE_AUDIO) {
            if(size() > 0) {
                return getPrimoFile().getParent();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * Lista degli elementi presenti nell'album
     * @return Lista elementi
     */
    public List<String> getElementi() {
        return elementi;
    }


    /**
     * Dimensione dell'album
     * @return Numero di elementi presenti nell'album
     */
    public int size(){
        return elementi.size();
    }


    /**
     * Restituisce il primo file dell'album solitamente utilizzato per l'analisi o per l'anteprima
     * @return Primo elemento. Null se non ci sono elementi nell'album
     */
    public File getPrimoFile(){
        if(size() > 0) {
            return new File(elementi.get(0));
        } else {
            return null;
        }
    }


    /**
     * Restituisce l'id dell'album
     * @return Id dell'album
     */
    public long getId() {
        return id;
    }


    /**
     * Tipo di media che l'album contiene
     * @return Costante della classe MediaUtils
     */
    public int getMediaType() {
        return mediaType;
    }


}
