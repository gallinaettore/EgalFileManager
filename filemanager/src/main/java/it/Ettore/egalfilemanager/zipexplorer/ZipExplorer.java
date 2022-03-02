package it.Ettore.egalfilemanager.zipexplorer;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.util.Log;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import it.Ettore.androidutilsx.utils.FileUtils;
import junrar.Archive;
import junrar.rarfile.FileHeader;


/**
 * Classe per l'esplorazione di archivi compressi
 */
public class ZipExplorer {
    private final List<ArchiveEntry> listaEntry;


    /**
     *
     * @param file File di un archivio compresso
     */
    public ZipExplorer(File file) {
        this.listaEntry = analizzaArchivio(file);
    }


    /**
     * Analizza l'archivio per ottenere la lista di elementi situati al suo interno
     * @param file File di un archivio compresso
     * @return Lista di elementi. Null se il file è null.
     */
    private List<ArchiveEntry> analizzaArchivio(File file) {
        if(file == null) return null;
        final List<ArchiveEntry> listaEntry = new ArrayList<>();
        final String mime = FileUtils.getMimeType(file);
        if(mime.equals("application/zip") || mime.equals("application/x-zip-compressed") || mime.equals("application/java-archive")) {
            ZipFile zipFile = null;
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    zipFile = new ZipFile(file, Charset.forName("iso-8859-1"));
                } else {
                    zipFile = new ZipFile(file); //nelle versioni precedenti i files contenenti caratteri speciali non possono essere decompressi
                }

                //processo tutti i files
                final Enumeration e = zipFile.entries();
                ArchiveEntry archiveEntry;
                while (e.hasMoreElements()) {
                    final ZipEntry entry = (ZipEntry) e.nextElement();
                    if (entry != null) {
                        archiveEntry = new ArchiveEntry(entry);
                        if(!archiveEntry.isDirectory()) {
                            listaEntry.add(archiveEntry);
                        }
                    }
                }

                //creo la struttura delle cartelle a partire dai files presenti
                final Set<String> pathCartelle = new HashSet<>(); //alcuni zip non hanno le cartelle sotto forma di zip entry (saranno create manualmente)
                for(ArchiveEntry aEntry : listaEntry){
                    final String[] strutturaDirectory = aEntry.getStrutturaDirectory();
                    if(strutturaDirectory != null) {
                        final StringBuilder pathBuilder = new StringBuilder();
                        for (int i = 0; i < strutturaDirectory.length - 1; i++) { //viene saltato l'ultimo valore (nome del file)
                            pathBuilder.append(strutturaDirectory[i]).append("/");
                            if(!pathBuilder.toString().equals("/")) {
                                pathCartelle.add(pathBuilder.toString());
                            }
                        }
                    }
                }

                //aggiungo le cartelle alla lista generale
                for(String pathCartella : pathCartelle){
                    listaEntry.add(new ArchiveEntry(new ZipEntry(pathCartella)));
                }

            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            } finally {
                try {
                    zipFile.close();
                } catch (Exception ignored){}
            }
        } else if (mime.equals("application/rar") || mime.equals("application/x-rar-compressed")){
            Archive rarFile = null;
            try {
                rarFile = new Archive(file);
                FileHeader fileHeader;
                while ((fileHeader = rarFile.nextFileHeader()) != null) {
                    listaEntry.add(new ArchiveEntry(fileHeader));
                }
            } catch (Exception e){
                Log.e(getClass().getSimpleName(), e.getMessage());
            } finally {
                try {
                    rarFile.close();
                } catch (Exception ignored) {}
            }
        }

        return listaEntry;
    }


    /**
     * Effettua la scansione di elementi all'interno di una directory
     * @param entry Directory
     * @return Lista ordinata di elementi trovati
     */
    public List<ArchiveEntry> ls(ArchiveEntry entry){
        final List<ArchiveEntry> entries = new ArrayList<>();
        if(entry == null){
            //directory root
            for(ArchiveEntry currentEntry : this.listaEntry) {
                if(currentEntry.getParent() == null){
                    entries.add(currentEntry);
                }
            }
        } else {
            //sottodirectory
            if(entry.isDirectory()){
                final String path = entry.getPath();
                for(ArchiveEntry currentEntry : this.listaEntry) {
                    if(!currentEntry.getPath().equals(path) && currentEntry.getPath().startsWith(path)){
                        //l'entry si trova all'interno, rimuovo le cartelle genitori
                        final String pathParziale = currentEntry.getPath().substring(path.length());
                        if(!pathParziale.contains("/")){
                            //è un file
                            entries.add(currentEntry);
                        } else {
                            //è una directory
                            final String[] pathSplit = pathParziale.split("/");
                            if(pathSplit.length == 1){
                                //è la sottodirectory principale, e non una sottodirectory della sottodirectory
                                entries.add(currentEntry);
                            }
                        }
                    }
                }
            }
        }
        return ordinaListaEntry(entries);
    }


    /**
     * Restituisce l'elemento che contiene il path specificato
     * @param path Path che deve avere l'elemento
     * @return Elemento trovato. Null se il path è null o se non è stato trovato nessun elemento con quel path.
     */
    public ArchiveEntry getEntryWithPath(String path){
        if(path == null) return null;
        for(ArchiveEntry entry : listaEntry){
            if(entry.getPath().equals(path)){
                return entry;
            }
        }
        return null;
    }



    /**
     * Ordina la lista di elementi
     * @param listaEntry Lista da ordinare
     * @return Lista ordinata
     */
    private List<ArchiveEntry> ordinaListaEntry(List<ArchiveEntry> listaEntry){
        final List<ArchiveEntry> cartelle = new ArrayList<>();
        final List<ArchiveEntry> files = new ArrayList<>();
        for(ArchiveEntry entry : listaEntry){
            if(entry.isDirectory()){
                cartelle.add(entry);
            } else {
                files.add(entry);
            }
        }
        Collections.sort(cartelle);
        Collections.sort(files);
        cartelle.addAll(files);
        return cartelle;
    }

}
