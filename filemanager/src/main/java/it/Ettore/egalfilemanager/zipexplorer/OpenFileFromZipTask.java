package it.Ettore.egalfilemanager.zipexplorer;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.zip.ZipFile;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import junrar.Archive;
import junrar.rarfile.FileHeader;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task che estrae un file all'interno dell'archivio (come file temporaneo) per poi aprirlo
 */
public class OpenFileFromZipTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private final File archiveFile, destinationFile;
    private final ArchiveEntry entry;
    private final FileOpener fileOpener;
    private ColoredProgressDialog progress;


    /**
     *
     * @param activity Activity chiamante
     * @param archiveFile File archivio che contiene l'elemento
     * @param entry Elemento (file) da aprire
     */
    public OpenFileFromZipTask(@NonNull Activity activity, File archiveFile, ArchiveEntry entry){
        this.activity = new WeakReference<>(activity);
        this.archiveFile = archiveFile;
        this.entry = entry;
        this.fileOpener = new FileOpener(activity);
        this.destinationFile = entry == null ? null : new File(activity.getCacheDir(), entry.getName());
    }


    /**
     * Mostra la progress dialog
     */
    @Override
    protected void onPreExecute(){
        if(entry != null) {
            progress = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.estrazione_in_corso, entry.getName()));
            progress.setCancelable(false);
        }
    }


    /**
     * Estrazione in background
     * @param params Nessun parametro
     * @return True se l'estrazione avviene con successo
     */
    @Override
    protected Boolean doInBackground(Void... params) {
        if(archiveFile == null || destinationFile == null) return false;
        boolean success = false;
        int BUFFER_SIZE = 1024 * 8;

        if(entry.isZip()){
            BufferedInputStream bis = null;
            OutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                ZipFile zipFile;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    zipFile = new ZipFile(archiveFile, Charset.forName("iso-8859-1"));
                } else {
                    zipFile = new ZipFile(archiveFile); //nelle versioni precedenti i files contenenti caratteri speciali non possono essere decompressi
                }

                bis = new BufferedInputStream(zipFile.getInputStream(entry.getZipEntry()));
                fos = new FileOutputStream(destinationFile);
                bos = new BufferedOutputStream(fos, BUFFER_SIZE);

                int b;
                byte buffer[] = new byte[BUFFER_SIZE];
                while ((b = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
                    bos.write(buffer, 0, b);
                }

                bos.flush();
                success = true;

            } catch (Exception e) {
                e.printStackTrace();
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
        } else if (entry.isRar()){

            try {
                final Archive rarFile = new Archive(archiveFile);
                final FileHeader fileHeader = entry.getRarFileHeader(rarFile);
                OutputStream stream = null;
                try {
                    stream = new FileOutputStream(destinationFile);
                    rarFile.extractFile(fileHeader, stream);
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try{
                        stream.close();
                    } catch (Exception ignored){}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return success;
    }


    /**
     * Chiude la progress dialog e apre il file se l'estrazione è avvenuto con successo
     * @param success True se l'estrazione è avvenuto con successo
     */
    @Override
    protected void onPostExecute(Boolean success){
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}

        if(activity.get() != null && !activity.get().isFinishing()){
            if(success){
                fileOpener.openFile(destinationFile);
            } else {
                CustomDialogBuilder.make(activity.get(), R.string.impossibile_completare_operazione, CustomDialogBuilder.TYPE_ERROR).show();
            }
        }
    }
}
