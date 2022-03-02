package it.Ettore.egalfilemanager.lan.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.RootFileOutputStream;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.lan.OrdinatoreFilesLan;
import it.Ettore.egalfilemanager.lan.SmbFileUtils;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mount.MountUtils;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


/**
 * Service per la copia di files su locale da server smb
 */
public class SmbDownloadService extends CopyService {
    private static final String KEYBUNDLE_USER = "user";
    private static final String KEYBUNDLE_PWD = "pwd";

    private FileManager fileManager;
    private long ultimoAggiornamento;
    private List<File> filesDaAggiungereSuMediaStore;
    private MountUtils mountUtils;


    /**
     * Costruttore di default (obbligatorio)
     */
    public SmbDownloadService(){
        super("SmbDownloadService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param user Username del server smb
     * @param pwd Password del server smb
     * @param destinazione Cartella locale di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<SmbFile> listaFiles, String user, String pwd, File destinazione, long totSize, int totFiles,
                                           Map<SmbFile, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler){
        final ArrayList<String> listaPaths = SmbFileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<SmbFile, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getPath(), entry.getValue());
        }
        final Intent intent = makeStartIntent(context, SmbDownloadService.class, listaPaths, destinazione.getAbsolutePath(), azioniPathsGiaPresenti, handler, totSize, totFiles, false);
        intent.putExtra(KEYBUNDLE_USER, user);
        intent.putExtra(KEYBUNDLE_PWD, pwd);
        return intent;
    }



    /**
     * Esecuzione copia in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();
        mountUtils = new MountUtils(this);
        filesDaAggiungereSuMediaStore = new ArrayList<>();

        final List<String> listaPaths = getListaPathDaCopiare();
        final String user = intent.getStringExtra(KEYBUNDLE_USER);
        final String pwd = intent.getStringExtra(KEYBUNDLE_PWD);
        final NtlmPasswordAuthentication auth = SmbFileUtils.createAuth(user, pwd);
        List<SmbFile> listaFiles = SmbFileUtils.listPathToListFile(listaPaths, auth);
        final File destinazione = getPathDestinazione() != null ? new File(getPathDestinazione()) : null;

        //verifico la correttezza dei dati
        if(listaFiles == null || destinazione == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
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
            final SmbFile inputFile = listaFiles.get(i);
            copiaRicorsiva(inputFile, destinazione, getAzioniPathsGiaPresenti());
        }

        //aggiorno il mediastore
        final StoragesUtils storagesUtils = new StoragesUtils(this);
        if(!filesDaAggiungereSuMediaStore.isEmpty() && storagesUtils.isOnSdCard(destinazione)){
            try {
                sendTextMessage(getString(R.string.aggiornamento_media_library));
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.addFilesToMediaLibrary(filesDaAggiungereSuMediaStore, new MediaScannerUtil.MediaScannerListener() {
                    @Override
                    public void onScanCompleted() {
                        sendMessageMediaScannerFinished();
                    }
                });
            } catch (Exception ignored){}
        }

        //al termine della copia ripristino tutti i mountpoints che erano stati messi in RW a RO
        mountUtils.ripristinaRo();

        //notifico che la copia è terminata
        if(isRunning()){
            sendMessageCopyFinished();
        }
    }


    @Override
    protected int getTipoCopia() {
        return COPY_SMB_TO_LOCAL;
    }



    /**
     * Copia ricorsiva dei file. Non sarà possibile effettuare download su percorsi root perchè su questi non è possibile aprire uno stream
     * @param inputFile File di origine
     * @param outputFolder File di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     */
    private void copiaRicorsiva(SmbFile inputFile, File outputFolder, Map<String, Integer> azioniFilesGiaPresenti){

        if(!SmbFileUtils.isDirectory(inputFile)) {
            //file

            //imposto i mountpoint come riscrivibili se necessario
            mountUtils.montaInRwSeNecessario(outputFolder);

            incrementaIndiceFile();
            long fileSize;
            try {
                fileSize = inputFile.length();
            } catch (SmbException e) {
                e.printStackTrace();
                aggiungiPathNonProcessato(inputFile.getPath());
                return;
            }

            File outputFile = null;
            //se azione == null
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getPath());
            if(azione == null){
                //il file non è ancora presente
                outputFile = new File(outputFolder, inputFile.getName());
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                outputFile = new File(outputFolder, inputFile.getName());
                fileManager.cancella(outputFile, false); //non uso i permessi di root perchè nel download non è possibile scrivere su questi percorsi
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                outputFile = LocalFileUtils.rinominaFilePerEvitareSovrascrittura(new File(outputFolder, inputFile.getName()));
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            final String inputFileName = inputFile.getName();
            final String inputFileParent = inputFile.getParent();
            final String outputFileParent = outputFile.getParent();
            sendMessageUpdateFile(inputFileName, inputFileParent, outputFileParent, fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                in = inputFile.getInputStream();
                out = SAFUtils.getOutputStream(this, outputFile);
                if(out == null && fileManager.haPermessiRoot()){
                    out = new RootFileOutputStream(outputFile);
                }

                long bytesWrited = 0L;
                byte[] buffer;
                if(out instanceof RootFileOutputStream){
                    buffer = new byte[1024];
                } else {
                    buffer = new byte[1024 * 32];
                }
                int read;

                // Transfer bytes from in to out
                while ((read = in.read(buffer)) > 0) {
                    if (!isRunning()) {
                        //cancello il file parziale se si annulla l'operazione
                        try {
                            out.close();
                        } catch (Exception ignored) {}
                        try {
                            in.close();
                        } catch (Exception ignored) {}
                        fileManager.cancella(outputFile, false);
                        mountUtils.ripristinaRo();
                        sendMessageCanceled();
                        return;
                    }
                    out.write(buffer, 0, read);
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
                } catch (Exception ignored) {}
                try {
                    in.close();
                } catch (Exception ignored) {}
            }

            //verifico l'esistenza del file copiato
            if(success){
                aggiungiPathProcessato(inputFile.getPath());
                filesDaAggiungereSuMediaStore.add(outputFile);
            } else {
                aggiungiPathNonProcessato(inputFile.getPath());
                //cancella file incompleto
                if(fileManager.fileExists(outputFile)){
                    fileManager.cancella(outputFile, false);
                }
            }

        } else {
            //cartella
            final String dirName = inputFile.getName();
            final File nuovaDir = new File(outputFolder, dirName);

            if(!fileManager.fileExists(nuovaDir)){
                fileManager.creaCartella(outputFolder, nuovaDir.getName());
            }

            try {
                final SmbFile[] arrayFiles = inputFile.listFiles();
                if(arrayFiles != null){
                    List<SmbFile> listaFiles = new ArrayList<>(Arrays.asList(arrayFiles));
                    listaFiles = OrdinatoreFilesLan.ordinaPerNome(listaFiles);
                    for (SmbFile f : listaFiles) {
                        if(!isRunning()) {
                            mountUtils.ripristinaRo();
                            sendMessageCanceled();
                            return;
                        }
                        copiaRicorsiva(f, nuovaDir, azioniFilesGiaPresenti);
                    }
                }
            } catch (SmbException e) {
                e.printStackTrace();
            }
        }
    }

}
