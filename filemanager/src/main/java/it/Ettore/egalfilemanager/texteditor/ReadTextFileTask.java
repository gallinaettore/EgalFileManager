package it.Ettore.egalfilemanager.texteditor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomProgressDialog;
import it.Ettore.egalfilemanager.fileutils.RootFileInputStream;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Task per la lettura del file di testo. L'EditTextviene aggiornata al termine
 * La lettura è veloce (per questo il task non è annullabile), impiega più tempo invece il setText dell'editText
 */
public class ReadTextFileTask extends AsyncTask <Void, Void, Boolean> {
    private static final long MAX_FILE_SIZE = 1_000_000L; //max 1Mb
    private static final int MAX_CHARS = 1_000_000; //numero massimo di caratteri leggibili dall'app
    private final File file;
    private final WeakReference<Activity> activity;
    private final WeakReference<ReadTextFileListener> listener;
    private final WeakReference<EditText> editText;
    private CustomProgressDialog dialog;
    private final StringBuilder sb;


    /**
     *
     * @param activity Activity
     * @param file File di testo da leggere
     * @param editText EditText in cui visualizzare il testo
     * @param listener Listener eseguito alla fine della lettura
     */
    public ReadTextFileTask(@NonNull Activity activity, @NonNull File file, @NonNull EditText editText, ReadTextFileListener listener){
        this.activity = new WeakReference<>(activity);
        this.file = file;
        this.listener = new WeakReference<>(listener);
        this.editText = new WeakReference<>(editText);
        this.sb = new StringBuilder();
    }


    /**
     * Mostro la dialog
     */
    @Override
    protected void onPreExecute(){
        dialog = new CustomProgressDialog(activity.get());
        dialog.setTitle(R.string.lettura);
        dialog.setMessage(file.getName());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();
        LockScreenOrientation.lock(activity.get());
    }


    /**
     * Esegue la lettura in background
     * @param voids Nessun parametro
     * @return True se la lettura va a buon fine
     */
    @Override
    protected Boolean doInBackground(Void... voids) {
        boolean success = false;
        if(file != null){

            InputStream fstream = null;
            InputStreamReader isr = null;
            BufferedReader br = null;

            if(file.length() > MAX_FILE_SIZE){
                return false;
            }

            try {
                //Stream di lettura
                if(new StoragesUtils(activity.get()).isOnRootPath(file)){
                    fstream = new RootFileInputStream(file);
                } else {
                    fstream = new FileInputStream(file);
                }
                isr = new InputStreamReader(fstream, "UTF-8");
                br = new BufferedReader(isr);

                String strLine;
                while ((strLine = br.readLine()) != null) {
                    if (sb.length() > MAX_CHARS) {
                        //leggo un numero massimo di caratteri, per evitare out of memory
                        throw new InterruptedException();
                    }
                    sb.append(strLine).append("\n");
                }
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //Chiudo gli stream
                try {
                    if (br != null) br.close();
                } catch (IOException ignored) { }
                try {
                    if (isr != null) isr.close();
                } catch (IOException ignored) { }
                try {
                    if (fstream != null) fstream.close();
                } catch (IOException ignored) { }
            }
        }
        return success;
    }




    /**
     * Esegue il listener al completamento della lettura
     * @param succcess True se la lettura va a buon fine
     */
    @Override
    protected void onPostExecute(Boolean succcess) {
        super.onPostExecute(succcess);

        if(succcess && editText.get() != null){
            //visualizzo il testo nell'edittext (operazione lunga), al termine chiudo la dialog e rimuovo il listener
            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    LockScreenOrientation.unlock(activity.get());
                    dismissDialog();
                    editText.get().removeTextChangedListener(this);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            };
            editText.get().addTextChangedListener(textWatcher);
            editText.get().setText(sb.toString());
        } else {
            LockScreenOrientation.unlock(activity.get());
            dismissDialog();
        }

        if(listener != null && listener.get() != null){
            listener.get().onReadFile(file, succcess);
        }
    }


    public void dismissDialog(){
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
    }


    /**
     * Listener del task lettura file di testo
     */
    @FunctionalInterface
    public interface ReadTextFileListener {

        /**
         * Chiamato al termine della lettura
         * @param file File letto
         * @param success True se la lettura è andata a buon fine
         */
        void onReadFile(File file, boolean success);
    }
}
