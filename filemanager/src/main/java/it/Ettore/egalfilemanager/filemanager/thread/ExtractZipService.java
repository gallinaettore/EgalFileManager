package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;


/**
 * Service per l'estrazione di archivi zip
 */
public class ExtractZipService extends BaseExtractService {
    private int totFiles;
    private long uncompressedSize, ultimoAggiornamento, totWrited;


    /**
     * Costruttore
     */
    public ExtractZipService() {
        super("ExtractZipService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param file Archivio compresso
     * @param destinationFolder Directory di estrazione
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull File file, @NonNull File destinationFolder, @NonNull ExtractHandler handler){
        return createStartIntent(context, ExtractZipService.class, file, destinationFolder, handler);
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        //verifico la correttezza dei dati
        if(getFile() == null || getDestinationFolder() == null){
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation();

        analisi();

        if(uncompressedSize >= getDestinationFolder().getParentFile().getUsableSpace()) {
            sendMessageError(R.string.spazio_insufficiente);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        boolean success = extractZip();
        aggiornaMediaStore();

        if(isRunning()) {
            if (totFiles != 0 && success) {
                //operazione completata con successo
                sendMessageSuccessfully(R.string.files_estratti);
            } else {
                sendMessageError(R.string.files_non_estrati);
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Analizza i files da estrarre
     */
    private void analisi() {
        try {
            ZipFile zipFile;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                zipFile = new ZipFile(getFile(), Charset.forName("iso-8859-1"));
            } else {
                zipFile = new ZipFile(getFile()); //nelle versioni precedenti i files contenenti caratteri speciali non possono essere decompressi
            }
            final Enumeration e = zipFile.entries();
            while (e.hasMoreElements()) {
                if (!isRunning()){
                    sendMessageCanceled();
                    return;
                }
                final ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry != null && !entry.isDirectory()) {
                    totFiles++;
                    uncompressedSize += entry.getSize();
                }
            }
        } catch (Exception ignored) {}
    }


    /**
     * Estrae il zip
     * @return True se il file viene estratto correttamente. False se il file zip è null,se non esiste, se è una directory, se la destinazione è null,
     * se non è possibile creare la directory di destinazione, se avviene un errore in fase di estrazione
     */
    private boolean extractZip(){
        if(getFile() == null || !getFile().exists() || getFile().isDirectory() || getDestinationFolder() == null) return false;

        if(!getDestinationFolder().exists()) {
            boolean cartellaCreata = getFileManager().creaCartella(getDestinationFolder().getParentFile(), getDestinationFolder().getName());
            if(!cartellaCreata){
                return false;
            }
        }

        boolean success = false;
        BufferedInputStream bis = null;
        OutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            ZipFile zipFile;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                zipFile = new ZipFile(getFile(), Charset.forName("iso-8859-1"));
            } else {
                zipFile = new ZipFile(getFile()); //nelle versioni precedenti i files contenenti caratteri speciali non possono essere decompressi
            }
            final Enumeration e = zipFile.entries();

            int BUFFER_SIZE = 1024 * 8;
            ciclo_generale:while (e.hasMoreElements()) {

                final ZipEntry entry = (ZipEntry) e.nextElement();
                if(entry == null) continue; //se non è possibile ottenere il zip entry a causa di un carattere non valido
                final File destinationFilePath = new File(getDestinationFolder(), entry.getName());

                if (entry.isDirectory()) {
                    getFileManager().creaCartella(getDestinationFolder(), entry.getName());
                } else {

                    bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    fos = SAFUtils.getOutputStream(getFileManager().getContext(), destinationFilePath);
                    bos = new BufferedOutputStream(fos, BUFFER_SIZE);

                    int b;
                    byte buffer[] = new byte[BUFFER_SIZE];
                    while ((b = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {

                        if(!isRunning()){
                            getFileManager().cancella(destinationFilePath, true);
                            success = false;
                            sendMessageCanceled();
                            break ciclo_generale;
                        }

                        bos.write(buffer, 0, b);
                        totWrited += b;
                        final long now = System.currentTimeMillis();
                        if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                            sendMessageUpdateProgress(null, (int)(totWrited/1000), (int)(uncompressedSize/1000));
                            ultimoAggiornamento = now;
                        }
                    }

                    bos.flush();
                    success = true;
                    aggiungiAListaInserimentoSuMediaStore(destinationFilePath);
                }
            }
        } catch (Exception e) {
            success = false;
        } finally {
            try {
                bos.close();
            } catch (Exception ignored){}
            try {
                fos.close();
            } catch (Exception ignored){}
            try{
                bis.close();
            } catch (Exception ignored){}
        }

        return success;
    }
}
