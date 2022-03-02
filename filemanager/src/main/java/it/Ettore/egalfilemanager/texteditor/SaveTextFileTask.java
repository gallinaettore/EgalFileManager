package it.Ettore.egalfilemanager.texteditor;

import android.app.Activity;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomProgressDialog;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ROOT_EXPLORER;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Task per il salvataggio di testo su file
 */
public class SaveTextFileTask extends AsyncTask <Void, Void, Boolean> {
    private final WeakReference<Activity> activity;
    private final File file;
    private final String text;
    private final SaveTextFileListener listener;
    private ColoredProgressDialog dialog;


    /**
     *
     * @param activity Activity chiamante
     * @param file File su cui salvare il testo
     * @param text Testo da salvare
     * @param listener Listener eseguito al termine del salvataggio
     */
    public SaveTextFileTask(@NonNull Activity activity, File file, String text, SaveTextFileListener listener){
        this.activity = new WeakReference<>(activity);
        this.file = file;
        this.text = text;
        this.listener = listener;
    }


    @Override
    protected void onPreExecute() {
        dialog = new CustomProgressDialog(activity.get());
        dialog.setTitle(file.getName());
        dialog.setMessage(R.string.salvataggio_in_corso);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
        LockScreenOrientation.lock(activity.get());
    }


    @Override
    protected Boolean doInBackground(Void... voids) {
        if(activity.get() == null || activity.get().isFinishing() || file == null || text == null){
            return false;
        }

        boolean success = false;
        final OutputStream os = SAFUtils.getOutputStream(activity.get(), file);
        if(os != null){
            //salvataggio normale
            PrintWriter printWriter = null;
            try {
                printWriter = new PrintWriter(os);
                printWriter.print(text);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if ( printWriter != null ) {
                    printWriter.close();
                }
            }
        } else {
            //se lo stream è null provo con i permessi di root, se sono stati concessi all'app
            boolean isRoot = PreferenceManager.getDefaultSharedPreferences(activity.get()).getBoolean(KEY_PREF_ROOT_EXPLORER, false);
            if(isRoot) {
                final File tempFile = new File(activity.get().getCacheDir(), file.getName());
                PrintWriter printWriter = null;
                boolean tempCreato = false;
                try {
                    printWriter = new PrintWriter(tempFile);
                    printWriter.print(text);
                    tempCreato = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (printWriter != null) {
                        printWriter.close();
                    }
                }
                if(tempCreato){
                    final FileManager fileManager = new FileManager(activity.get());
                    fileManager.setPermessiRoot(true);
                    success = fileManager.copiaFileComeRoot(tempFile, file, true);
                    fileManager.cancella(tempFile, false);
                }
            }
        }

        return success;
    }


    @Override
    protected void onPostExecute(Boolean success) {
        LockScreenOrientation.unlock(activity.get());
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
        if(activity.get() != null && !activity.get().isFinishing()){
            if(success){
                ColoredToast.makeText(activity.get(), R.string.file_salvato, Toast.LENGTH_LONG).show();
            } else {
                ColoredToast.makeText(activity.get(), R.string.errore_salvataggio, Toast.LENGTH_LONG).show();
            }
            if(listener != null){
                listener.onFileSaved(file, success);
            }
        }
    }


    /**
     * Listener del salvataggio
     */
    public interface SaveTextFileListener {

        /**
         * Chiamato al termine del salvataggio del file
         * @param savedFile File salvato
         * @param success True se il file è stato salvato correttamente
         */
        void onFileSaved(File savedFile, boolean success);
    }
}
