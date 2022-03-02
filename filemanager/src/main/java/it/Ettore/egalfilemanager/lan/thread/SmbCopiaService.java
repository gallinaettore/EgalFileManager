package it.Ettore.egalfilemanager.lan.thread;

import android.content.Context;
import android.content.Intent;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.lan.OrdinatoreFilesLan;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la copia di files da percorsi smb dentro altri percorsi smb
 */
public class SmbCopiaService extends CopyService {
    private static final String KEYBUNDLE_USER_SORGENTE = "user_sorgente";
    private static final String KEYBUNDLE_PWD_SORGENTE = "pwd_sorgente";
    private static final String KEYBUNDLE_USER_DESTINAZIONE = "user_destinazione";
    private static final String KEYBUNDLE_PWD_DESTINAZIONE = "pwd_destinazione";

    private long ultimoAggiornamento;



    /**
     * Costruttore di default (obbligatorio)
     */
    public SmbCopiaService(){
        super("SmbCopiaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param userSorgente Username del server smb che ospita i files da copiare
     * @param pwdSorgente Password del server smb che ospita i files da copiare
     * @param destinazione Cartella SMB di destinazione
     * @param userDestinazione Username del server smb di destinazione
     * @param pwdDestinazione Password del server smb di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<SmbFile> listaFiles, String userSorgente, String pwdSorgente, SmbFile destinazione, String userDestinazione,
                                           String pwdDestinazione, long totSize, int totFiles, Map<SmbFile, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler){
        final ArrayList<String> listaPaths = SmbFileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<SmbFile, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getPath(), entry.getValue());
        }
        final Intent intent = makeStartIntent(context, SmbCopiaService.class, listaPaths, destinazione.getPath(), azioniPathsGiaPresenti, handler, totSize, totFiles, false);
        intent.putExtra(KEYBUNDLE_USER_SORGENTE, userSorgente);
        intent.putExtra(KEYBUNDLE_PWD_SORGENTE, pwdSorgente);
        intent.putExtra(KEYBUNDLE_USER_DESTINAZIONE, userDestinazione);
        intent.putExtra(KEYBUNDLE_PWD_DESTINAZIONE, pwdDestinazione);
        return intent;
    }


    /**
     * Esecuzione copia in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        final List<String> listaPaths = getListaPathDaCopiare();
        final String userSorgente = intent.getStringExtra(KEYBUNDLE_USER_SORGENTE);
        final String pwdSorgente = intent.getStringExtra(KEYBUNDLE_PWD_SORGENTE);
        final NtlmPasswordAuthentication authSorgente = SmbFileUtils.createAuth(userSorgente, pwdSorgente);
        List<SmbFile> listaFiles = SmbFileUtils.listPathToListFile(listaPaths, authSorgente);

        //verifico la correttezza dei dati
        if(listaFiles == null || getPathDestinazione() == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
            sendMessageCanceled();
            return;
        }

        //notifico di avviare la dialog di copia
        sendMessageStartCopy();

        //copio i files
        listaFiles = OrdinatoreFilesLan.ordinaPerNome(listaFiles);
        for (int i=0; i < listaFiles.size(); i++) {
            if (!isRunning()){
                sendMessageCanceled();
                return;
            }
            SmbFile inputFile = listaFiles.get(i);
            try {
                final String userDestinazione = intent.getStringExtra(KEYBUNDLE_USER_DESTINAZIONE);
                final String pwdDestinazione = intent.getStringExtra(KEYBUNDLE_PWD_DESTINAZIONE);
                final NtlmPasswordAuthentication authDestinazione = SmbFileUtils.createAuth(userDestinazione, pwdDestinazione);
                final SmbFile cartellaDestinazione = new SmbFile(getPathDestinazione(), authDestinazione);
                copiaRicorsiva(inputFile, cartellaDestinazione, getAzioniPathsGiaPresenti(), authDestinazione);
            } catch (Exception e) {
                e.printStackTrace();
                aggiungiPathNonProcessato(inputFile.getPath());
            }
        }

        //notifico che la copia è terminata
        if(isRunning()){
            sendMessageCopyFinished();
        }
    }


    @Override
    protected int getTipoCopia() {
        return COPY_SMB_TO_SMB;
    }


    /**
     * Copia ricorsiva dei file
     * @param inputFile File di origine
     * @param outputFolder File di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     */
    private void copiaRicorsiva(SmbFile inputFile, SmbFile outputFolder, Map<String, Integer> azioniFilesGiaPresenti, NtlmPasswordAuthentication authDestinazione){

        if(!SmbFileUtils.isDirectory(inputFile)) {
            //file
            incrementaIndiceFile();
            long fileSize = 0L;
            try {
                fileSize = inputFile.length();
            } catch (SmbException e) {
                e.printStackTrace();
            }

            SmbFile outputFile = null;
            //se azione == null
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getPath());
            if(azione == null){
                //il file non è ancora presente
                try {
                    outputFile = new SmbFile(outputFolder + inputFile.getName(), authDestinazione);
                } catch (MalformedURLException ignored) {}
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                try {
                    outputFile = new SmbFile(outputFolder + inputFile.getName(), authDestinazione);
                    outputFile.delete();
                } catch (Exception ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                try {
                    final SmbFile possibileOutFile = new SmbFile(outputFolder + inputFile.getName(), authDestinazione);
                    outputFile = SmbFileUtils.rinominaFilePerEvitareSovrascrittura(possibileOutFile, authDestinazione);
                } catch (MalformedURLException ignored) {}
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            sendMessageUpdateFile(inputFile.getName(), inputFile.getParent(), outputFile.getParent(), fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                in = inputFile.getInputStream();
                out = outputFile.getOutputStream();

                long bytesWrited = 0L;
                byte[] buf = new byte[1024 * 32];
                int read;
                // Transfer bytes from in to out
                while ((read = in.read(buf)) > 0) {
                    if (!isRunning()) {
                        //cancello il file parziale se si annulla l'operazione
                        try {
                            out.close();
                        } catch (Exception ignored) {}
                        try {
                            in.close();
                        } catch (Exception ignored) {}
                        outputFile.delete();
                        sendMessageCanceled();
                        return;
                    }
                    out.write(buf, 0, read);
                    bytesWrited += read;
                    incrementaTotWrited(read);

                    final long now = System.currentTimeMillis();
                    if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                        sendMessageUpdateProgress(bytesWrited);
                        ultimoAggiornamento = now;
                    }

                }

                out.flush();
                success = bytesWrited == fileSize;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
                try {
                    in.close();
                } catch (Exception ignored) {}
            }

            //verifico l'esistenza del file copiato
            try {
                if(success && outputFile.exists()){
                    aggiungiPathProcessato(inputFile.getPath());
                } else {
                    aggiungiPathNonProcessato(inputFile.getPath());
                    //cancella file incompleto
                    if(outputFile.exists()){
                        outputFile.delete();
                    }
                }
            } catch (SmbException e) {
                e.printStackTrace();
                aggiungiPathNonProcessato(inputFile.getPath());
            }

        } else {
            //cartella
            SmbFile nuovaDir = null;
            if (inputFile.getParent().equals(outputFolder.getPath())) {
                //se il copia/incolla avviene nella stessa cartella duplico la cartella
                nuovaDir = SmbFileUtils.rinominaFilePerEvitareSovrascrittura(inputFile, authDestinazione);
            } else {
                //copia/incolla normale
                try {
                    nuovaDir = new SmbFile(outputFolder + inputFile.getName(), authDestinazione);
                } catch (MalformedURLException ignored) {}
            }

            try {
                if(!nuovaDir.exists()){
                    nuovaDir.mkdir();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                final SmbFile[] arrayFiles = inputFile.listFiles();
                if(arrayFiles != null && nuovaDir != null){
                    List<SmbFile> listaFiles = new ArrayList<>(Arrays.asList(arrayFiles));
                    listaFiles = OrdinatoreFilesLan.ordinaPerNome(listaFiles);
                    for (SmbFile f : listaFiles) {
                        if(!isRunning()) {
                            sendMessageCanceled();
                            return;
                        }
                        copiaRicorsiva(f, nuovaDir, azioniFilesGiaPresenti, authDestinazione);
                    }
                }
            } catch (SmbException e) {
                e.printStackTrace();
            }
        }
    }
}
