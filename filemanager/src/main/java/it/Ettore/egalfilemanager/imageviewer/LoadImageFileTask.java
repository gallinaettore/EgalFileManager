package it.Ettore.egalfilemanager.imageviewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.iconmanager.IconManager;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Task per il caricamento di un'immagine a partire da un file
 */
public class LoadImageFileTask extends AsyncTask<Void, Void, Bitmap> {
    private static final int MAX_LUNGHEZZA = 500;
    private final File file;
    private final LoadImageFileListener listener;
    private boolean isGif, showProgressDialog;
    private ColoredProgressDialog progress;
    private WeakReference<Activity> activity;
    private String progressMessage;


    /**
     *
     * @param file File immagine
     * @param listener Listener eseguito al termine del caricamento
     */
    public LoadImageFileTask(File file, LoadImageFileListener listener){
        this.file = file;
        this.listener = listener;
    }


    /**
     * Settare se si vuole visualizzare un progress dialog durante il caricamento in background
     * @param activity Activity chiamante
     * @param progressMessage Messaggio della progress dialog
     */
    public void setProgressDialog(@NonNull Activity activity, String progressMessage){
        this.showProgressDialog = true;
        this.activity = new WeakReference<>(activity);
        this.progressMessage = progressMessage;
    }


    /**
     * Mostra la progress dialog se impostata
     */
    @Override
    protected void onPreExecute() {
        if(showProgressDialog){
            progress = ColoredProgressDialog.show(activity.get(), null, progressMessage);
            progress.setCancelable(false);
        }
    }


    /**
     * Esegue il caricamento in background
     * @param objects Nessun parametro
     * @return Il bitmap dell'immagine se è un'immagine comune. Null se è una gif, se non è stato possibile decodificare il bitmap, o se il file è null
     */
    @Override
    protected Bitmap doInBackground(Void[] objects) {
        if(file == null){
            return null;
        }

        if(FileUtils.getFileExtension(file).equalsIgnoreCase("gif")){
            //immagine gif
            isGif = true;
            return null;
        } else {
            //immagine normale
            try {
                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                if(options.outWidth * options.outHeight <= 36_000_000) { //risoluzione massima di decodifica 36Mpx
                    // Calculate inSampleSize
                    options.inSampleSize = IconManager.calculateInSampleSize(options, MAX_LUNGHEZZA, MAX_LUNGHEZZA);

                    // Decode bitmap with inSampleSize set
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                    final Matrix matrix = IconManager.getMatrixPerCorrettoOrientamento(file);
                    if (matrix != null) {
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                    return bitmap;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    /**
     * Esecuzione nel thread della UI
     * @param bitmap Bitmap ottenuto dalla decodifica del file
     */
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        try {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
        if(listener != null){
            listener.onLoadImage(bitmap, isGif);
        }
    }


    /**
     * Listener della classe LoadImageFileTask
     */
    @FunctionalInterface
    public interface LoadImageFileListener {

        /**
         * Eseguito al termine del caricamento
         * @param bitmap Il bitmap dell'immagine se è un'immagine comune. Null se è una gif, se non è stato possibile decodificare il bitmap, o se il file è null
         * @param isGif True se l'immagine è una gif
         */
        void onLoadImage(Bitmap bitmap, boolean isGif);
    }
}
