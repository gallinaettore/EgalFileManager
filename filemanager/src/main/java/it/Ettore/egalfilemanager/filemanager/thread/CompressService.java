package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.RootFileInputStream;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mount.MountUtils;


/**
 * Servizio che esegue la compressione in archivio zip
 */
public class CompressService extends BaseProgressService {
    private FileManager fileManager;
    private MountUtils mountUtils;
    private String directoryGenerale;
    private long totalSize, totWrited, ultimoAggiornamento;
    private int errors;
    private File destinationZipFile, originalDestinationZipFile;


    /**
     * Costruttore di default (obbligatorio)
     */
    public CompressService(){
        super("CompressService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param destinationZipFile File zip in cui saranno inseriti i files
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<File> listaFiles, @NonNull File destinationZipFile, @NonNull CompressHandler handler){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        final Intent intent = makeStartBaseIntent(context, CompressService.class, listaPaths, handler);
        intent.putExtra(KEYBUNDLE_PATH_DESTINAZIONE, destinationZipFile.getAbsolutePath());
        return intent;
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();
        mountUtils = new MountUtils(this);

        final String pathDestinazione = intent.getStringExtra(KEYBUNDLE_PATH_DESTINAZIONE);


        final List<String> listaPaths = getListaPaths();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);
        //listaFiles = LocalFileUtils.fileListToRootFileList(this, listaFiles);

        //verifico la correttezza dei dati
        if(listaFiles == null || pathDestinazione == null || listaFiles.isEmpty()){
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        destinationZipFile = new File(pathDestinazione);
        final String dialogTitle = getString(R.string.compressione);
        final String dialogMessage = String.format(getString(R.string.compressione_in_corso), destinationZipFile.getName());

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(dialogTitle, dialogMessage);


        //conto quanti files da comprimere ci sono in totale
        directoryGenerale = listaFiles.get(0).getParent();
        for(File file : listaFiles){
            analisi(file);
        }

        //verifico che ci sia spazio sufficiente
        final File destinationFolder = destinationZipFile.getParentFile();
        if(totalSize >= fileManager.getFreeSpace(destinationFolder)){
            errors++;
            sendMessageError(R.string.spazio_insufficiente);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //verifico se la destinazione è riscrivibile, altrimenti creo un file temporaneo nella cache
        if(fileManager.getStoragesUtils().isOnRootPath(destinationFolder)){
            //imposto i mountpoint come riscrivibili se necessario
            mountUtils.montaInRwSeNecessario(destinationFolder);
            originalDestinationZipFile = destinationZipFile;
            destinationZipFile = new File(getCacheDir(), destinationZipFile.getName());
        }

        //comprimo i files
        OutputStream dest = null;
        ZipOutputStream out = null;
        try {
            dest = SAFUtils.getOutputStream(this, destinationZipFile);
            out = new ZipOutputStream(new BufferedOutputStream(dest));
            for(File file : listaFiles){
                if (!isRunning()){
                    mountUtils.ripristinaRo();
                    sendMessageCanceled();
                    sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
                    return;
                }
                addToZip(file, out);
            }
        } catch (Exception e) {
            errors++;
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception ignored) {}
            try {
                dest.close();
            } catch (Exception ignored){}
        }

        //se presente copio il file temporaneo nella directory di destinazione
        if(originalDestinationZipFile != null){
            boolean success = fileManager.copiaFileComeRoot(destinationZipFile, originalDestinationZipFile, true);
            if(!success){
                errors++;
            }
            fileManager.cancella(destinationZipFile, true);
            destinationZipFile = originalDestinationZipFile;
        }

        //aggiorno il mediastore
        try {
            if(errors == 0 && new StoragesUtils(this).isOnSdCard(destinationZipFile)){
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.addFileToMediaLibrary(destinationZipFile, null);
            }
        } catch (Exception ignored){}

        //al termine della copia ripristino tutti i mountpoints che erano stati messi in RW a RO
        mountUtils.ripristinaRo();

        if(isRunning()) {
            if (errors == 0) {
                //compressione completata con successo
                sendMessageSuccessfully(R.string.files_compressi);
            } else {
                sendMessageError(R.string.files_non_compressi);
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Notifica all'handler che l'operazione è stata annullata e cancella il file incompleto
     */
    @Override
    protected void sendMessageCanceled() {
        super.sendMessageCanceled();
        if(destinationZipFile != null) {
            fileManager.cancella(destinationZipFile, true);
        }
    }


    /**
     * Analizza ricorsivamente lo spazio occupato da files e cartelle
     * @param file File o cartella da analizzare
     */
    private void analisi(File file){
        if(!isRunning()) {
            sendMessageCanceled();
            return;
        }
        if(!file.isDirectory()){
            totalSize += file.length();
        } else {
            final List<File> listaFiles = fileManager.ls(file);
            if(listaFiles != null){
                for(File f : listaFiles){
                    analisi(f);
                }
            }
        }
    }


    /**
     * Aggiunge il file corrente al zip di destinazione
     * @param file File da aggiungere
     * @param out OutputStream aperto sullo zip di destinazione
     */
    private void addToZip(File file, ZipOutputStream out){
        InputStream in = null;
        BufferedInputStream bis = null;
        try {
            //out.setMethod(ZipOutputStream.DEFLATED);
            final int BUFFER = 1024 * 8;
            byte data[] = new byte[BUFFER];

            final String pathAssoluto = file.getAbsolutePath();
            final String pathRelativo = pathAssoluto.substring(directoryGenerale.length());
            ZipEntry zipEntry;
            if(file.isDirectory()){
                zipEntry = new ZipEntry(pathRelativo + "/");
            } else {
                zipEntry = new ZipEntry(pathRelativo);
            }
            out.putNextEntry(zipEntry);

            if(!file.isDirectory()) {

                if(fileManager.getStoragesUtils().isOnRootPath(file)){
                    in = new RootFileInputStream(file);
                } else {
                    in = new FileInputStream(file);
                }
                bis = new BufferedInputStream(in, BUFFER);

                int bytes;
                while ((bytes = bis.read(data, 0, BUFFER)) != -1) {
                    if(!isRunning()){
                        try {
                            bis.close();
                        } catch (Exception ignored){}
                        try {
                            in.close();
                        } catch (Exception ignored){}
                        mountUtils.ripristinaRo();
                        sendMessageCanceled();
                        return;
                    }

                    out.write(data, 0, bytes);
                    totWrited += bytes;

                    final long now = System.currentTimeMillis();
                    if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                        sendMessageUpdateProgress((int)(totWrited/1000), (int)(totalSize/1000));
                        ultimoAggiornamento = now;
                    }
                }
            } else {
                final List<File> listaFiles = fileManager.ls(file);
                if(listaFiles != null){
                    for(File f : listaFiles){
                        if(!isRunning()){
                            mountUtils.ripristinaRo();
                            sendMessageCanceled();
                            return;
                        }
                        addToZip(f, out);
                    }
                }
            }

        } catch(Exception e) {
            errors++;
            e.printStackTrace();
        } finally {
            try {
                bis.close();
            } catch (Exception ignored){}
            try {
                in.close();
            } catch (Exception ignored){}
        }
    }


    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener(){
        final CompressHandler.ListenerData dati = new CompressHandler.ListenerData();
        dati.destinationZipFile = destinationZipFile;
        return dati;
    }
}
