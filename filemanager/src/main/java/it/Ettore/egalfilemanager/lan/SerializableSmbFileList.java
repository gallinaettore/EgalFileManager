package it.Ettore.egalfilemanager.lan;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import jcifs.smb.SmbFile;


/**
 * Classe wrapper per serializzare una lista di files smb da passare negli intent
 * Tutti i files devono avere stessa user e password
 */
public class SerializableSmbFileList implements Serializable, Iterable<SmbFile> {
    private final ArrayList<String> fileList;
    private final String smbUser, smbPassword;


    /**
     *
     * @param smbUser User del server smb che ospita i files
     * @param smbPassword Password del server smb che ospita i files
     */
    public SerializableSmbFileList(String smbUser, String smbPassword){
        this.fileList = new ArrayList<>();
        this.smbUser = smbUser;
        this.smbPassword = smbPassword;
    }


    /**
     *
     * @param smbUser User del server smb che ospita i files
     * @param smbPassword Password del server smb che ospita i files
     * @param initialCapacity Capacità iniziale della lista
     */
    public SerializableSmbFileList(String smbUser, String smbPassword, int initialCapacity){
        this.fileList = new ArrayList<>(initialCapacity);
        this.smbUser = smbUser;
        this.smbPassword = smbPassword;
    }


    /**
     *
     * @param fileList Lista di file con cui creare la lista serializzabile
     * @return Lista serializzabile
     */
    public static SerializableSmbFileList fromFileList(List<SmbFile> fileList, String smbUser, String smbPassword){
        final SerializableSmbFileList serializableFileList;
        if(fileList != null){
            serializableFileList = new SerializableSmbFileList(smbUser, smbPassword, fileList.size());
            for(SmbFile file : fileList){
                serializableFileList.addFile(file);
            }
            serializableFileList.trimToSize();
        } else {
            serializableFileList = new SerializableSmbFileList(smbUser, smbPassword);
        }
        return serializableFileList;
    }


    /**
     *
     * @param pathList Lista di path con cui creare la lista serializzabile
     * @return Lista serializzabile
     */
    public static SerializableSmbFileList fromPathList(List<String> pathList, String smbUser, String smbPassword){
        final SerializableSmbFileList serializableFileList;
        if(pathList != null){
            serializableFileList = new SerializableSmbFileList(smbUser, smbPassword, pathList.size());
            for(String path : pathList){
                serializableFileList.addPath(path);
            }
            serializableFileList.trimToSize();
        } else {
            serializableFileList = new SerializableSmbFileList(smbUser, smbPassword);
        }
        return serializableFileList;
    }


    /**
     * Aggiunge un file alla lista, deve avere stessa user e password specificato nel costruttore
     * @param file File da aggiugnere
     * @return True se il file è stato aggiunto correttamente
     */
    public boolean addFile(SmbFile file) {
        return file != null && addPath(file.getPath());
    }


    /**
     * Aggiunge un file alla lista, deve avere stessa user e password specificato nel costruttore
     * @param filePath File da aggiugnere
     * @return True se il file è stato aggiunto correttamente
     */
    public boolean addPath(String filePath){
        return filePath != null && this.fileList.add(filePath);
    }


    /**
     * Restituisce il file all'iesima posizione
     * @param index Posizione
     * @return File, null in caso di errore
     */
    public SmbFile fileAt(int index){
        try {
            return new SmbFile(this.fileList.get(index), SmbFileUtils.createAuth(smbUser, smbPassword));
        } catch (MalformedURLException e) {
            return null;
        }
    }


    /**
     * Restituisce la dimensione della lista
     * @return Dimensione della lista
     */
    public int size(){
        return this.fileList.size();
    }


    /**
     * Restituisce un booleano per indicare se la lista è vuota
     * @return True se la lista è vuota
     */
    public boolean isEmpty(){
        return this.fileList.isEmpty();
    }


    /**
     * Adatta la dimensione della lista al numero effettivo di elementi
     */
    public void trimToSize(){
        this.fileList.trimToSize();
    }


    /**
     * Restituisce un ArrayList di file
     * @return ArrayList di file
     */
    public List<SmbFile> toFileList(){
        return SmbFileUtils.listPathToListFile(this.fileList, SmbFileUtils.createAuth(smbUser, smbPassword));
    }


    /**
     * Iterator per utilizzare il ciclo foreach su questa classe
     * @return Iterator
     */
    @NonNull
    @Override
    public Iterator<SmbFile> iterator() {
        return toFileList().iterator();
    }
}
