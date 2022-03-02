package it.Ettore.egalfilemanager.musicplayer;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;


/**
 * Classe per la gestione dei files playlist
 */
public class PlaylistManager {


    /**
     * Ottiene una lista di files leggendo una playlist m3u
     * @param m3uFile File m3u
     * @return Lista di files. Lista vuota in caso di errore
     */
    public static List<File> parseM3uPlaylist(File m3uFile){
        List<File> listaFiles = new ArrayList<>();
        if(m3uFile == null) return listaFiles;
        if(m3uFile.length() > 500_000) return listaFiles;

        final String EXTENDED_INFO_TAG = "#EXTM3U";
        final String RECORD_TAG = "^[#][E|e][X|x][T|t][I|i][N|n][F|f].*";

        FileInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new FileInputStream(m3uFile);
            reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!(line.equalsIgnoreCase(EXTENDED_INFO_TAG) || line.trim().equals(""))) {
                    if (!line.matches(RECORD_TAG)) {
                        line = line.replaceAll("^(.*?),", "");
                        if(line.startsWith("/")){
                            //percorso assoluto
                            listaFiles.add(new File(line));
                        } else {
                            //percorso relativo
                            listaFiles.add(new File(m3uFile.getParent(), line));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception ignored){}
            try {
                fis.close();
            } catch (Exception ignored){}
        }
        return listaFiles;
    }


    /**
     * Salva la lista di files in un file m3u.
     * Se i files audio hanno lo stesso percorso del file m3u vengono salvati con il percorso relativo, altrimenti vengono salvati con il percorso assoluto
     * @param context Context
     * @param playlist Lista di files audio
     * @param fileM3u File in cui salvare la playlist
     * @return True se la playlist è stata salvata con successo. False in caso di errore
     */
    public static boolean salvaPlaylistM3u(@NonNull Context context, List<File> playlist, File fileM3u){
        if(playlist == null || fileM3u == null) return false;

        //verifico se tutti i files hanno lo stesso path della playlist m3u
        final List<File> listaDaVerificare = new ArrayList<>(playlist);
        listaDaVerificare.add(fileM3u);
        boolean filesHannoLoStessoPath = FileUtils.filesHasSamePath(listaDaVerificare);

        //salvo i dati
        boolean success;
        final OutputStream os = SAFUtils.getOutputStream(context, fileM3u);
        if(os == null) return false;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(os));
            for (File file : playlist) {
                if(filesHannoLoStessoPath){
                    //salvo il percorso relativo del file
                    bw.write(file.getName());
                } else {
                    //se hanno percorsi diversi salvo il percorso assoluto
                    bw.write(file.getAbsolutePath());
                }
                bw.newLine();
            }
            success = true;
        } catch (IOException e){
            e.printStackTrace();
            success = false;
        } finally {
            try {
                bw.close();
            } catch (Exception ignored){}
            try {
                os.close();
            } catch (Exception ignored){}
        }
        return success;
    }

}
