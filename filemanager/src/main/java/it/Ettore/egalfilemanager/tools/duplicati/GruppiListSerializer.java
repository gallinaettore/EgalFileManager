package it.Ettore.egalfilemanager.tools.duplicati;
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
import java.util.List;

import androidx.annotation.NonNull;


/**
 * Classe di utilità per salvare su file i gruppi di files duplicati
 */
public class GruppiListSerializer {
    private final File file;


    /**
     * @param context Context chiamante
     */
    public GruppiListSerializer(@NonNull Context context){
        this.file = new File(context.getFilesDir(), "lista_files_duplicati.ser");
    }


    /**
     * Salva il contenuto della lista su file
     * @param lists Lista di gruppi
     */
    public void serialize(List<List<String>> lists){
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(lists);
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
    }


    /**
     * Ottiene la lista salvata
     * @return Lista di gruppi salvata su file. Null se non è possibile leggere il file.
     */
    @SuppressWarnings("unchecked")
    public List<List<String>> deserialize(){
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        List<List<String>> lists = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
             lists = (List<List<String>>) ois.readObject();
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
        return lists;
    }
}
