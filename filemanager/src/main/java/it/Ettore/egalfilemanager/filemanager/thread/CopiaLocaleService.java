package it.Ettore.egalfilemanager.filemanager.thread;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.copyutils.SovrascritturaFiles;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.RootFileInputStream;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mount.MountUtils;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la copia di files locali
 */
public class CopiaLocaleService extends CopyService {
    private FileManager fileManager;
    private long ultimoAggiornamento;
    private MountUtils mountUtils;
    private List<File> filesDaCancellareDaMediaStore, filesDaAggiungereSuMediaStore;
    private StoragesUtils storagesUtils;




    /**
     * Costruttore di default (obbligatorio)
     */
    public CopiaLocaleService(){
        super("CopiaLocaleService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da copiare
     * @param destinazione Cartella locale di destinazione
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param azioniFilesGiaPresenti Map contenente le associazioni files-azione da eseguire
     * @param handler Handler che permette al service di comunicare con la UI
     * @param cancellaOrigine True se in modalità Tadlia. False se in modalità copia.
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, List<File> listaFiles, File destinazione, long totSize, int totFiles,
                                           Map<File, Integer> azioniFilesGiaPresenti, @NonNull CopyHandler handler, boolean cancellaOrigine){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        final HashMap<String, Integer> azioniPathsGiaPresenti = new HashMap<>(azioniFilesGiaPresenti.size());
        for(Map.Entry<File, Integer> entry : azioniFilesGiaPresenti.entrySet()){
            azioniPathsGiaPresenti.put(entry.getKey().getAbsolutePath(), entry.getValue());
        }
        return makeStartIntent(context, CopiaLocaleService.class, listaPaths, destinazione.getAbsolutePath(), azioniPathsGiaPresenti, handler, totSize, totFiles, cancellaOrigine);
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
        storagesUtils = new StoragesUtils(this);
        filesDaCancellareDaMediaStore = new ArrayList<>();
        filesDaAggiungereSuMediaStore = new ArrayList<>();

        final List<String> listaPaths = getListaPathDaCopiare();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);
        listaFiles = LocalFileUtils.fileListToRootFileList(this, listaFiles);

        //verifico la correttezza dei dati
        if(listaFiles == null || getPathDestinazione() == null || listaFiles.isEmpty() || tutteAzioniIgnoraFiles()){
            sendMessageCanceled();
            return;
        }

        //notifico di avviare la dialog di copia
        sendMessageStartCopy();

        //copio i files
        final File destinazione = new File(getPathDestinazione());
        listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);
        for (File inputFile : listaFiles) {
            if (!isRunning()){
                sendMessageCanceled();
                return;
            }
            final File destFile = new File(destinazione, inputFile.getName());
            if (inputFile.equals(destFile) && isCancellaOrigine()) {
                //se il taglia incolla avviene nella stessa cartella non faccio niente
                sendMessageCopyFinished();
                return;
            }
            copiaRicorsiva(inputFile, destinazione, getAzioniPathsGiaPresenti());
        }

        //aggiorno il mediastore
        if(!filesDaAggiungereSuMediaStore.isEmpty() && new StoragesUtils(this).isOnSdCard(destinazione)){
            try {
                sendTextMessage(getString(R.string.aggiornamento_media_library));
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.removeFilesFromMediaLibrary(filesDaCancellareDaMediaStore);
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
        return COPY_LOCAL_TO_LOCAL;
    }


    /**
     * Copia ricorsiva dei file
     * @param inputFile File di origine
     * @param outputFolder File di destinazione
     * @param azioniFilesGiaPresenti Map che contiene per ogni path l'azione da utilizzare (sovrascrivi, rinomina, ignora)
     */
    private void copiaRicorsiva(File inputFile, File outputFolder, Map<String, Integer> azioniFilesGiaPresenti){

        if(inputFile.isFile()) {
            //imposto i mountpoint come riscrivibili se necessario
            mountUtils.montaInRwSeNecessario(outputFolder);
            if(isCancellaOrigine()){
                mountUtils.montaInRwSeNecessario(inputFile);
            }

            //file
            incrementaIndiceFile();
            long fileSize = inputFile.length();

            File outputFile = null;
            //se azione == null
            final Integer azione = azioniFilesGiaPresenti.get(inputFile.getAbsolutePath());
            if(azione == null){
                //il file non è ancora presente
                outputFile = new File(outputFolder, inputFile.getName());
            } else if(azione == SovrascritturaFiles.AZIONE_SOVRASCRIVI){
                outputFile = new File(outputFolder, inputFile.getName());
                fileManager.cancella(outputFile, false);
            } else if (azione == SovrascritturaFiles.AZIONE_RINOMINA){
                //se è un file già esistente ed è stato scelto di rinominarlo
                outputFile = LocalFileUtils.rinominaFilePerEvitareSovrascrittura(fileManager, new File(outputFolder, inputFile.getName()));
            } else if (azione == SovrascritturaFiles.AZIONE_IGNORA){
                //se è un file già esistente ed è stato scelto di mantenere quello già presente, non copia niente
                return;
            }

            sendMessageUpdateFile(inputFile.getName(), inputFile.getParent(), outputFile.getParent(), fileSize);

            InputStream in = null;
            OutputStream out = null;
            boolean success = false;

            try {
                if(storagesUtils.isOnRootPath(inputFile)){
                    in = new RootFileInputStream(inputFile);
                } else {
                    in = new FileInputStream(inputFile);
                }
                out = SAFUtils.getOutputStream(this, outputFile); //non utilizzo il RootFileOutputStream perchè non è molto veloce, meglio usare il classico metodo di copia file root

                if(out != null) {
                    //permessi di scrittura sul file
                    long bytesWrited = 0L;
                    byte[] buffer;
                    if(fileSize < 500_000){ //buffer piccolo per files sotto i 500Kb
                        buffer = new byte[1024 * 8];
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

                    sendMessageUpdateProgress(bytesWrited); //opzionale ma evita di far vedere progress del file sempre vicino allo zero quando copia files piccoli
                    out.flush();
                    success = bytesWrited == fileSize;
                } else {
                    //provo con i permessi di root
                    if(fileManager.haPermessiRoot()) {
                        sendMessageUpdateProgress(0);
                        long bytesWrited = 0L;
                        success = fileManager.copiaFileComeRoot(inputFile, outputFile, false);
                        if(success){
                            bytesWrited = fileSize;
                            incrementaTotWrited(fileSize);
                        }
                        sendMessageUpdateProgress(bytesWrited);
                    }
                }

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
                aggiungiPathProcessato(inputFile.getAbsolutePath());
                filesDaAggiungereSuMediaStore.add(outputFile);
                //funzione "taglia"
                if(isCancellaOrigine()){
                    fileManager.cancella(inputFile, false);
                    filesDaCancellareDaMediaStore.add(inputFile);
                }
            } else {
                aggiungiPathNonProcessato(inputFile.getAbsolutePath());
                //cancella file incompleto
                if(outputFile.exists()){
                    fileManager.cancella(outputFile, false);
                }
            }

        } else {
            //cartella

            File nuovaDir;
            if (inputFile.getParentFile().equals(outputFolder)) {
                //se il copia/incolla avviene nella stessa cartella duplico la cartella
                nuovaDir = LocalFileUtils.rinominaFilePerEvitareSovrascrittura(fileManager, inputFile);
            } else {
                //copia/incolla normale
                final String dirName = inputFile.getName();
                nuovaDir = new File(outputFolder, dirName);
            }

            if(!fileManager.fileExists(nuovaDir)){
                fileManager.creaCartella(outputFolder, nuovaDir.getName()); //utilizzando il file manager posso usare anche i permessi di root
            }

            List<File> listaFile = fileManager.ls(inputFile);
            if(listaFile != null) {
                listaFile = OrdinatoreFiles.ordinaPerNome(listaFile);
                for (File f : listaFile) {
                    if(!isRunning()) {
                        mountUtils.ripristinaRo();
                        sendMessageCanceled();
                        return;
                    }
                    copiaRicorsiva(f, nuovaDir, azioniFilesGiaPresenti);
                }
            }
            //dopo aver finito di copiare tutti i files al suo interno, se c'è la funzione taglia, la cartella dovrebbe essere già vuota
            if(isCancellaOrigine()){
                List<File> listaFileVuota = fileManager.ls(inputFile);
                if(listaFileVuota != null && listaFileVuota.isEmpty()){
                    //mi accerto che la cartella sia davvero vuota
                    fileManager.cancella(inputFile, false);
                }
            }
        }
    }

}
