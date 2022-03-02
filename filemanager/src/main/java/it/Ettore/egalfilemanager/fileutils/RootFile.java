package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;



/**
 * Classe che astrae un file presente in un percorso root creato dalla lettura dei risultati del comando ls
 */
public class RootFile extends File {
    private boolean isDirectory;
    private long size, data;
    private String collegamento, permessi;


    /**
     *
     * @param parent Cartella parent
     * @param child Nome del file
     */
    private RootFile(File parent, @NonNull String child) {
        super(parent, child);
    }


    /**
     * Crea il file
     * @param parent Cartella parent
     * @param lsLine Riga ottenuta dal comando ls che contiene le informazioni del file
     * @return File. Null se il parent è null, se la stringa ls non contiene i dati necessari o se avviene un errore
     */
    public static RootFile fromLsResult(File parent, String lsLine) {
        if (parent == null || lsLine == null) return null;

        //divido la stringa in token
        final String[] split = lsLine.split(" ");
        if(split.length < 6) return null;

        //vedo se il file è un collegamento
        boolean linked = false;
        for (String token : split) {
            if (token.contains("->") && split[0].startsWith("l")) {
                linked = true;
            }
        }

        //analizzo la stringa per ottenere la dimensione, la data, il nome e il path originale se è un collegamento
        StringBuilder name = new StringBuilder();
        final StringBuilder linkBuilder = new StringBuilder();
        String sizeString = "0";
        String dateString = "";

        int p = getColonPosition(split);
        if(p != -1){
            dateString = split[p - 1] + " | " + split[p];
            sizeString = split[p - 2];
        }
        if (!linked) {
            for (int i = p + 1; i < split.length; i++) {
                name.append(" ").append(split[i]);
            }
            name = new StringBuilder(name.toString().trim());
        } else {
            int q = getLinkPosition(split);
            for (int i = p + 1; i < q; i++) {
                name.append(" ").append(split[i]);
            }
            name = new StringBuilder(name.toString().trim());
            for (int i = q + 1; i < split.length; i++) {
                linkBuilder.append(" ").append(split[i]);
            }
        }
        final String link = linkBuilder.toString().trim();
        if(name.toString().equals(".") || name.toString().equals("..")){
            return null;
        }

        //verifico se è una directory
        boolean isDir = false;
        if(split[0].startsWith("d")){
            isDir = true;
        } else if (linked && new File(link).isDirectory()){
            isDir = true;
        }

        //faccio il parsing di dimensione e data
        long size = 0;
        if(sizeString != null && sizeString.trim().length()> 0){
            try {
                size = Long.parseLong(sizeString);
            } catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
        long lastMod = 0L;
        if(dateString.trim().length() > 0) {
            final ParsePosition pos = new ParsePosition(0);
            @SuppressLint("SimpleDateFormat") final SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd | HH:mm");
            final Date date = simpledateformat.parse(dateString, pos);
            if(date != null) {
                lastMod = date.getTime();
            }
        }

        //creo il file
        final RootFile file = new RootFile(parent, name.toString());
        file.isDirectory = isDir;
        file.size = size;
        file.permessi = split[0].substring(1, 4) + " " + split[0].substring(4, 7) + " " + split[0].substring(7, 10);
        file.collegamento = !link.isEmpty() ? link : null;
        file.data = lastMod;
        return file;
    }


    private static int getColonPosition(String[] array){
        for(int i=0; i<array.length; i++){
            if(array[i].contains(":"))return i;
        }
        return  -1;
    }


    private static int getLinkPosition(String[] array){
        for(int i=0; i<array.length; i++){
            if(array[i].contains("->"))return i;
        }
        return  0;
    }


    /**
     * Verifica se è una directory
     * @return True se è una directory
     */
    @Override
    public boolean isDirectory(){
        return this.isDirectory;
    }


    /**
     * Verifica se è un file
     * @return True se non è una directory
     */
    @Override
    public boolean isFile(){
        return !this.isDirectory;
    }


    /**
     * Restituisce la dimensione del file
     * @return Dimensione del file in bytes
     */
    @Override
    public long length(){
        return this.size;
    }


    /**
     * Restituisce il path del file (se è un collegamento restituisce il path del file originale)
     * @return Canonical path
     */
    @NonNull
    @Override
    public String getCanonicalPath() {
        if(collegamento == null){
            return getAbsolutePath();
        } else {
            return collegamento;
        }
    }


    /**
     * Restituisce il file ottenuto dal path canonico (se è un collegamento viene restituito il file originale)
     * @return Canonical file
     */
    @NonNull
    @Override
    public File getCanonicalFile() {
        return new File(getCanonicalPath());
    }


    /**
     * Verifica se è un file nascosto
     * @return True se è un file nascosto
     */
    @Override
    public boolean isHidden() {
        return getName().startsWith(".") || super.isHidden();
    }


    /**
     * Restituisce la data di ultima modifica
     * @return Data del file
     */
    @Override
    public long lastModified(){
        return this.data;
    }


    /**
     * Verifica l'esistenza del file inviando una richiesta da terminale
     * @return True se il file esiste
     */
    @Override
    public boolean exists(){
        return LocalFileUtils.rootFileExists(this);
    }


    /**
     * Restituisce i permessi Unix del file
     * @return Permessi del file
     */
    public String getPermissions(){
        return this.permessi;
    }

}
