package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Classe wrapper per serializzare una lista di files da passare negli intent
 */
public class SerializableFileList implements Serializable, Iterable<File> {
    private final ArrayList<String> fileList;


    public SerializableFileList(){
        this.fileList = new ArrayList<>();
    }


    public SerializableFileList(int initialCapacity){
        this.fileList = new ArrayList<>(initialCapacity);
    }


    /**
     *
     * @param fileList Lista di file con cui creare la lista serializzabile
     * @return Lista serializzabile
     */
    @NotNull
    public static SerializableFileList fromFileList(List<File> fileList){
        final SerializableFileList serializableFileList;
        if(fileList != null){
            serializableFileList = new SerializableFileList(fileList.size());
            for(File file : fileList){
                serializableFileList.addFile(file);
            }
            serializableFileList.trimToSize();
        } else {
            serializableFileList = new SerializableFileList();
        }
        return serializableFileList;
    }


    /**
     *
     * @param pathList Lista di path con cui creare la lista serializzabile
     * @return Lista serializzabile
     */
    public static SerializableFileList fromPathList(List<String> pathList){
        final SerializableFileList serializableFileList;
        if(pathList != null){
            serializableFileList = new SerializableFileList(pathList.size());
            for(String path : pathList){
                serializableFileList.addPath(path);
            }
            serializableFileList.trimToSize();
        } else {
            serializableFileList = new SerializableFileList();
        }
        return serializableFileList;
    }


    /**
     * Aggiunge un file alla lista
     * @param file File da aggiugnere
     * @return True se il file è stato aggiunto correttamente
     */
    public boolean addFile(File file) {
        return file != null && addPath(file.getAbsolutePath());
    }


    /**
     * Aggiunge un file alla lista
     * @param filePath File da aggiugnere
     * @return True se il file è stato aggiunto correttamente
     */
    public boolean addPath(String filePath){
        return filePath != null && this.fileList.add(filePath);
    }


    /**
     * Restituisce il file all'iesima posizione
     * @param index Posizione
     * @return File
     */
    public File fileAt(int index){
        return new File(this.fileList.get(index));
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
    public List<File> toFileList(){
        return FileUtils.listPathToListFile(this.fileList);
    }


    /**
     * Iterator per utilizzare il ciclo foreach su questa classe
     * @return Iterator
     */
    @NonNull
    @Override
    public Iterator<File> iterator() {
        return toFileList().iterator();
    }


    /**
     * Salva la lista su un file (molto utile per evitare i TransactionTooLargeException con liste molto grandi)
     * @param context Context
     * @param fileName Nome del file che sarà usato per la serializzazione (Ogni fragment deve usare un file diverso)
     * @return True se la serializzazione su file avviene con successo. False in caso contrario.
     */
    public boolean saveToFile(Context context, @NonNull String fileName){
        if(context == null) return false;
        final File file = new File(context.getFilesDir(), fileName);
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        boolean success = false;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(this.fileList);
            success = true;
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try{
                oos.close();
            } catch (Exception ignored){}
            try{
                fos.close();
            } catch (Exception ignored){}
        }
        return success;
    }


    /**
     * Crea un un oggetto SerializableFileList a partire da un file salvato
     * @param context Context
     * @param fileName Nome del file usato per la serializzazione (Ogni fragment deve usare un file diverso)
     * @return Instanza di SerializableFileList
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static SerializableFileList fromSavedFile(@NonNull Context context, @NonNull String fileName){
        final File file = new File(context.getFilesDir(), fileName);
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        SerializableFileList serializableFileList = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            serializableFileList = SerializableFileList.fromPathList((List<String>) ois.readObject());
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try{
                ois.close();
            } catch (Exception ignored){}
            try{
                fis.close();
            } catch (Exception ignored){}
        }
        if(serializableFileList == null) {
            serializableFileList = new SerializableFileList();
        }
        return serializableFileList;
    }
}
