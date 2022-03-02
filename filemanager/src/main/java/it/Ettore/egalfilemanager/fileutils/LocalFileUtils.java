package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.RootUtils;
import it.Ettore.egalfilemanager.filemanager.FileManager;


/**
 * Classe con metodi di utilità per la gestione di files locali (root e non)
 */
public class LocalFileUtils {


    /**
     * Converte una lista di files (comuni) in lista di RootFile (contenente le informazioni dei files su percorsi root)
     * @param context Context chiamante
     * @param files Lista di files comuni
     * @return Lista di files root, i files su percorsi normali saranno files comuni
     */
    public static List<File> fileListToRootFileList(@NonNull Context context, List<File> files){
        if(files == null) return null;
        final Set<File> cartelleParent = new HashSet<>(); //cartelle parent che contengo i files root
        final StoragesUtils storagesUtils = new StoragesUtils(context);
        for(File file : files){
            if(storagesUtils.isOnRootPath(file)) {
                cartelleParent.add(file.getParentFile());
            }
        }
        //analizzo tutte le cartelle parent
        final Map<String, File> mapRootFiles = new HashMap<>();
        final FileManager fileManager = new FileManager(context);
        fileManager.setPermessiRoot(true);
        for(File parent : cartelleParent){
            final List<File> listaRootFiles = fileManager.lsComeRoot(parent);
            for(File rootFile : listaRootFiles){
                mapRootFiles.put(rootFile.getAbsolutePath(), rootFile);
            }
        }
        //creo una lista con i files root
        final List<File> newRootList = new ArrayList<>(files.size());
        for(File file : files){
            final File rootFile = mapRootFiles.get(file.getAbsolutePath());
            if(rootFile != null){
                newRootList.add(rootFile);
            } else {
                newRootList.add(file);
            }
        }
        return newRootList;
    }


    /**
     * Verifica l'esistenza del file inviando una richiesta da terminale
     * @return True se il file esiste
     */
    public static boolean rootFileExists(File file) {
        if (file == null) return false;
        final String existsCommand = "if [ -e \"" + file.getAbsolutePath() + "\" ] ; then echo \"1\" ; else echo \"0\" ; fi";
        final List<String> results = RootUtils.sendCommands(true, existsCommand);
        return results.size() == 1 && results.get(0).equals("1");
    }


    /**
     * Verifica se un file che si trova su un percorso root è una cartella
     * Operazione abbastanza dispendiosa perchè viene fatto ls della cartella genitore da terminale
     * @param context Context
     * @param file File da verificare
     * @return True se è una cartella. False se è un file o se è null
     */
    public static boolean rootFileIsDirectory(@NonNull Context context, File file){
        if(file == null) return false;
        final List<File> files = new ArrayList<>(1);
        files.add(file);
        final List<File> rootFiles = fileListToRootFileList(context, files);
        return rootFiles.get(0).isDirectory();
    }


    /**
     * Se il file di destinazione non esiste è possibile utilizzare il file di destinazione, altrimenti ritorno un file rinominato del tipo "Filedestinazione (1).ext"
     *
     * @param fileDestinazioneOriginale File originale di destinazione
     * @return Nuovo File di destinazione
     */
    public static File rinominaFilePerEvitareSovrascrittura(File fileDestinazioneOriginale){
        if(fileDestinazioneOriginale == null) return fileDestinazioneOriginale;
        final String nomeOriginale = FileUtils.getFileNameWithoutExt(fileDestinazioneOriginale);
        final String ext = FileUtils.getFileExtension(fileDestinazioneOriginale);
        for(int i = 1; i < 10000; i++){
            String nuovoNomeFile;
            if(ext != null && !ext.isEmpty()){
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d).%s", nomeOriginale, i, ext);
            } else {
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d)", nomeOriginale, i);
            }
            final File nuovoFileDestinazione = new File(fileDestinazioneOriginale.getParent(), nuovoNomeFile);
            if(!nuovoFileDestinazione.exists()) return nuovoFileDestinazione;
        }
        return null;
    }


    /**
     * Se il file di destinazione non esiste è possibile utilizzare il file di destinazione, altrimenti ritorna un file rinominato del tipo "Filedestinazione (1).ext"
     * Usare questo metodo quando c'è possibilità di lavorare con files che si trovano in percorsi root
     *
     * @param fileManager File Manager per controllare l'esistenza dei files root
     * @param fileDestinazioneOriginale File originale di destinazione
     * @return Nuovo File di destinazione
     */
    public static File rinominaFilePerEvitareSovrascrittura(FileManager fileManager, File fileDestinazioneOriginale){
        if(fileDestinazioneOriginale == null) return fileDestinazioneOriginale;
        final String nomeOriginale = FileUtils.getFileNameWithoutExt(fileDestinazioneOriginale);
        final String ext = FileUtils.getFileExtension(fileDestinazioneOriginale);
        for(int i = 1; i < 10_000; i++){
            String nuovoNomeFile;
            if(ext != null && !ext.isEmpty()){
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d).%s", nomeOriginale, i, ext);
            } else {
                nuovoNomeFile = String.format(Locale.ENGLISH, "%s (%d)", nomeOriginale, i);
            }
            final File nuovoFileDestinazione = new File(fileDestinazioneOriginale.getParent(), nuovoNomeFile);
            if(!fileManager.fileExists(nuovoFileDestinazione)) return nuovoFileDestinazione;
        }
        return null;
    }
}
