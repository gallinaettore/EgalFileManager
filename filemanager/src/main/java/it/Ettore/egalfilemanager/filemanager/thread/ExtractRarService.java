package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import junrar.Archive;
import junrar.rarfile.FileHeader;


/**
 * Service per l'estrazione di archivi rar
 */
public class ExtractRarService extends BaseExtractService {
    private int totFiles;
    private long uncompressedSize;


    /**
     * Costruttore
     */
    public ExtractRarService() {
        super("ExtractRarService");
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
        return createStartIntent(context, ExtractRarService.class, file, destinationFolder, handler);
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        //verifico la correttezza dei dati
        if (getFile() == null || getDestinationFolder() == null) {
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation();

        analisi();

        if(uncompressedSize >= getDestinationFolder().getParentFile().getUsableSpace()){
            sendMessageError(R.string.spazio_insufficiente);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        boolean success = extractRar();
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
    private void analisi(){
        try {
            final Archive rarFile = new Archive(getFile());
            FileHeader fileHeader;
            while ((fileHeader = rarFile.nextFileHeader()) != null) {
                if (!isRunning()){
                    sendMessageCanceled();
                    return;
                }
                if(!fileHeader.isDirectory()){
                    totFiles++;
                    uncompressedSize += fileHeader.getUnpSize();
                }
            }
        } catch (Exception ignored){}
    }


    /**
     * Estrae il rar
     * @return True se l'estrazione va a buon fine. False se il file rar è null, se il file non esiste, se il file è una cartella, se la destinazione è null,
     * se non è possibile creare la cartella di destinazione, se il rar è criptato, se si verifica un errore in fase di estrazione
     */
    private boolean extractRar() {
        if (getFile() == null || !getFile().exists() || getFile().isDirectory() || getDestinationFolder() == null)
            return false;

        if (!getDestinationFolder().exists()) {
            boolean cartellaCreata = getFileManager().creaCartella(getDestinationFolder().getParentFile(), getDestinationFolder().getName());
            if (!cartellaCreata) {
                return false;
            }
        }

        boolean success = false;
        try {
            final Archive rarFile = new Archive(getFile());

            if (rarFile != null) {
                if (rarFile.isEncrypted()) {
                    Log.e(getClass().getSimpleName(), "Archive is encrypted cannot extreact");
                    return false;
                }

                FileHeader fileHeader;
                int count = 0;
                while ((fileHeader = rarFile.nextFileHeader()) != null) {

                    if (!isRunning()){
                        sendMessageCanceled();
                        return false;
                    }

                    if (fileHeader.isEncrypted()) {
                        Log.e(getClass().getSimpleName(), "file is encrypted cannot extract: " + fileHeader.getFileNameString());
                        continue;
                    }
                    OutputStream stream = null;
                    try {
                        if (fileHeader.isDirectory()) {
                            createDirectoryFromRar(fileHeader, getDestinationFolder());
                        } else {
                            count++;
                            //crea pure la struttura delle directory
                            final File f = createFileFromRar(fileHeader, getDestinationFolder());
                            stream = SAFUtils.getOutputStream(getFileManager().getContext(), f);
                            rarFile.extractFile(fileHeader, stream);
                            success = true;
                            sendMessageUpdateProgress(null, count, totFiles);
                            aggiungiAListaInserimentoSuMediaStore(f);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        success = false;
                    } finally {
                        try{
                            stream.close();
                        } catch (Exception ignored){}
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        return success;
    }


    /**
     * L'estrazione del rar avviene sempre dai files nelle directory più interne. Prima di estrarre il file occorre creare la directory o la struttura directories che lo contiene
     * @param fileHeader Dati del file da estrarre
     * @param destination Cartella di destinazione
     */
    private void createDirectoryFromRar(FileHeader fileHeader, File destination) {
        File directory;
        if (fileHeader.isDirectory() && fileHeader.isUnicode()) {
            final String strutturaSubDirectories = fileHeader.getFileNameW().replace("\\", "/");
            directory = new File(destination, strutturaSubDirectories);
            if (!directory.exists()) {
                getFileManager().creaCartella(directory.getParentFile(), fileHeader.getFileNameW());
            }
        } else if (fileHeader.isDirectory() && !fileHeader.isUnicode()) {
            final String strutturaSubDirectories = fileHeader.getFileNameString().replace("\\", "/");
            directory = new File(destination, strutturaSubDirectories);
            if (!directory.exists()) {
                getFileManager().creaCartella(directory.getParentFile(), fileHeader.getFileNameString());
            }
        }
    }


    /**
     * Crea il file
     * @param fileHeader Dati del file da estrarre
     * @param destination File di destinazione
     * @return File
     */
    private File createFileFromRar(FileHeader fileHeader, File destination) {
        File f;
        String name;
        if (fileHeader.isFileHeader() && fileHeader.isUnicode()) {
            name = fileHeader.getFileNameW();
        } else {
            name = fileHeader.getFileNameString();
        }
        f = new File(destination, name);
        if (!f.exists()) {
            f = makeFileFromRar(destination, name);
        }
        return f;
    }


    /**
     * Crea il file con tutta la struttura delle cartelle
     * @param destination Percorso di destinazione
     * @param name Nome del file
     * @return File creato. Null se nessun file è stato creato
     */
    private File makeFileFromRar(File destination, String name) {
        final String[] dirs = name.split("\\\\");
        if (dirs == null) {
            return null;
        }
        File path = destination;
        int size = dirs.length;
        if (size == 1) {
            return new File(destination, name);
        } else if (size > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                final File cartellaCorrente = new File(path, dirs[i]);
                if(!cartellaCorrente.exists()) {
                    getFileManager().creaCartella(path, dirs[i]);
                }
                path = cartellaCorrente;
            }
            final String fileName = dirs[dirs.length - 1];
            final File f = new File(path, fileName);
            getFileManager().creaFile(f);
            return f;
        } else {
            return null;
        }
    }
}
