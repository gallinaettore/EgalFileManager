package it.Ettore.egalfilemanager.copyutils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.thread.BaseProgressHandler;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import it.Ettore.egalfilemanager.view.ViewCopy;

import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_BYTES_COPIATI_FILE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_CANCELLA_ORIGINE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_DIMENSIONE_FILE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_FILES_COPIATI;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_INDICE_FILE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_MESSAGGIO;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_NOME_FILE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_PATH_DESTINAZIONE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_PATH_PARENT;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_SUCCESS;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_TIPO_COPIA;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_TOT_BYTES_COPIATI;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_TOT_FILES;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.KEYBUNDLE_TOT_SIZE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_COPY_CANCELED;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_COPY_FINISHED;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_MEDIA_SCANNER_FINISHED;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_MESSAGE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_START_COPY;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_UPDATE_FILE;
import static it.Ettore.egalfilemanager.lan.thread.SmbDownloadService.WHAT_UPDATE_PROGRESS;


/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Handler per mostrare i dati ricevuti dal service di copia
 */
public class CopyHandler extends BaseProgressHandler {
    private WeakReference<ViewCopy> viewCopy;
    private WeakReference<AlertDialog> copyDialog;
    private final WeakReference<CopyHandlerListener> copyListener;
    private WeakReference<MediaScannerUtil.MediaScannerListener> mediaScannerListener;
    private PowerManager.WakeLock wakeLock;


    /**
     *
     * @param activity Activity chiamante
     * @param copyListener Listener chiamato al termine del processo di copia
     */
    public CopyHandler(@NonNull Activity activity, CopyHandlerListener copyListener){
        super(activity);
        this.copyListener = new WeakReference<>(copyListener);
    }


    /**
     * Imposta il listener del media scanner, chiamato quando termina la scansione dei files aggiunti
     * @param mediaScannerListener Listener del media scanner
     */
    public void setMediaScannerListener(MediaScannerUtil.MediaScannerListener mediaScannerListener){
        this.mediaScannerListener = new WeakReference<>(mediaScannerListener);
    }


    /**
     * Crea e mostra la dialog di copia
     * @param cancellaOrigine True se in modalità taglia, False se in modalità copia
     * @param totFiles Numero totale di files da copiare
     * @param totSize Dimensione totale in bytes dei files da copiare
     */
    @SuppressLint("WakelockTimeout")
    private void showCopyDialog(boolean cancellaOrigine, int totFiles, long totSize){
        final CustomDialogBuilder builder = new CustomDialogBuilder(getActivity().get());
        builder.hideIcon(true);
        if (!cancellaOrigine) {
            builder.setTitle(R.string.copia);
        } else {
            builder.setTitle(R.string.spostamento);
        }
        builder.setCancelable(false);
        viewCopy = new WeakReference<>(new ViewCopy(getActivity().get()));
        viewCopy.get().setIndeterminate(true);
        viewCopy.get().setTotFiles(totFiles);
        viewCopy.get().setTotSize(totSize);
        builder.setView(viewCopy.get());
        builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            CopyService.interrompi();
            if (copyDialog.get() != null && copyDialog.get().isShowing()) {
                copyDialog.get().dismiss();
            }
        });
        copyDialog = new WeakReference<>(builder.create());
        copyDialog.get().show();

        LockScreenOrientation.lock(getActivity().get());
        final PowerManager powerManager = (PowerManager) getActivity().get().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire(); //se si vuole impostare il timeout a 10 minuti = 600000
    }


    /**
     * Chiamato quando si riceve un messaggio da parte del service
     * @param msg Messaggio ricevuto
     */
    @Override
    public void handleMessage(Message msg) {
        if(getActivity().get() == null || getActivity().get().isFinishing()){
            return;
        }
        final Bundle data = msg.getData();
        switch (msg.what) {
            case WHAT_START_COPY:
                //inviato quando inizia la copia, creo la dialog
                final boolean cancellaOrigine = data.getBoolean(KEYBUNDLE_CANCELLA_ORIGINE);
                final int totFiles = data.getInt(KEYBUNDLE_TOT_FILES);
                final long totSize = data.getLong(KEYBUNDLE_TOT_SIZE);
                showCopyDialog(cancellaOrigine, totFiles, totSize);
                break;
            case WHAT_UPDATE_FILE:
                //inviato quando copio un nuovo file, aggiorno la dialog con i nuovi riferimenti
                if(viewCopy.get() != null) {
                    final String nomeFile = data.getString(KEYBUNDLE_NOME_FILE);
                    final String parent = data.getString(KEYBUNDLE_PATH_PARENT);
                    final String destinazione = data.getString(KEYBUNDLE_PATH_DESTINAZIONE);
                    final long fileSize = data.getLong(KEYBUNDLE_DIMENSIONE_FILE);
                    final int indiceFile = data.getInt(KEYBUNDLE_INDICE_FILE);
                    viewCopy.get().mostraFileCorrente(nomeFile, parent);
                    viewCopy.get().mostraDestinazione(destinazione);
                    viewCopy.get().setFileSize(fileSize);
                    viewCopy.get().mostraIndiceFile(indiceFile);
                }
                break;
            case WHAT_UPDATE_PROGRESS:
                //inviato durante la copia, aggiorno la dialog con i progressi
                if(viewCopy.get() != null) {
                    final long bytesWrited = data.getLong(KEYBUNDLE_BYTES_COPIATI_FILE);
                    final long totWrited = data.getLong(KEYBUNDLE_TOT_BYTES_COPIATI);
                    viewCopy.get().setIndeterminate(false);
                    viewCopy.get().mostraProgressoFile(bytesWrited);
                    viewCopy.get().mostraProgressoTotale(totWrited);
                }
                break;
            case WHAT_MESSAGE:
                //inviato quando la dialog deve mostrare un messaggio, aggiorno la dialog con il messaggio
                if(viewCopy.get() != null) {
                    final String text = data.getString(KEYBUNDLE_MESSAGGIO);
                    viewCopy.get().mostraMessaggio(text);
                }
                break;
            case WHAT_COPY_CANCELED:
                //inviato quando viene annullata la copia, chiudo la dialog e mostro il toast
                dismissCopyDialog();
                ColoredToast.makeText(getActivity().get(), R.string.operazione_annulata, Toast.LENGTH_LONG).show();
                final String pathDestinazione = data.getString(KEYBUNDLE_PATH_DESTINAZIONE);
                final int tipoCopia = data.getInt(KEYBUNDLE_TIPO_COPIA);
                final ArrayList<String> pathsCopiati = data.getStringArrayList(KEYBUNDLE_FILES_COPIATI);
                if(copyListener != null && copyListener.get() != null){
                    copyListener.get().onCopyServiceFinished(false, pathDestinazione, pathsCopiati, tipoCopia);
                }
                break;
            case WHAT_COPY_FINISHED:
                //inviato al termine della copia, chiudo la dialog ed eseguo il listener
                dismissCopyDialog();
                final String messaggio = data.getString(KEYBUNDLE_MESSAGGIO);
                final boolean success = data.getBoolean(KEYBUNDLE_SUCCESS);
                final String pathDestinazione2 = data.getString(KEYBUNDLE_PATH_DESTINAZIONE);
                final int tipoCopia2 = data.getInt(KEYBUNDLE_TIPO_COPIA);
                final ArrayList<String> pathsCopiati2 = data.getStringArrayList(KEYBUNDLE_FILES_COPIATI);
                if(success){
                    final String successMessage = String.format(messaggio, String.valueOf(pathsCopiati2.size()));
                    if(!isActivityDestroyed()) {
                        ColoredToast.makeText(getActivity().get(), successMessage, Toast.LENGTH_LONG).show();
                    } else {
                        mostraNotifica(successMessage);
                    }
                } else {
                    if(!isActivityDestroyed()){
                        CustomDialogBuilder.make(getActivity().get(), messaggio, CustomDialogBuilder.TYPE_ERROR).show();
                    } else {
                        mostraNotifica(messaggio);
                    }
                }
                if(copyListener != null && copyListener.get() != null){
                    copyListener.get().onCopyServiceFinished(success, pathDestinazione2, pathsCopiati2, tipoCopia2);
                }
                break;
            case WHAT_MEDIA_SCANNER_FINISHED:
                //inviato quando il media scanner ha finito la scansione, eseguo il listener del media scanner
                if(mediaScannerListener != null && mediaScannerListener.get() != null){
                    mediaScannerListener.get().onScanCompleted();
                }
                break;
        }
    }


    /**
     * Chiude la dialog e rilascia l'orientamento. Chiamarlo sempre nell'onDestroy di un fragment che usa questo handler
     */
    @Override
    public void dismissProgressDialogOnDestroy(){
        super.dismissProgressDialogOnDestroy();
        dismissCopyDialog();
    }


    /**
     * Chiude la dialog e rilascia il wakelock.
     */
    private void dismissCopyDialog(){
        LockScreenOrientation.unlock(getActivity().get());
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        try {
            if (copyDialog != null && copyDialog.get() != null && copyDialog.get().isShowing()) {
                copyDialog.get().dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
    }


    /**
     * Restituisce il listener di copia impostato
     * @return Listener di copia
     */
    public CopyHandlerListener getCopyListener(){
        if(copyListener != null){
            return copyListener.get();
        } else {
            return null;
        }
    }
}
