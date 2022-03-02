package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.LockScreenOrientation;
import it.Ettore.egalfilemanager.NotificationChannelManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.CustomProgressDialog;

import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_RICERCA_DUPLICATI_TERMINATA;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_OPERAZIONI;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_CLASSE;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_DIALOG_CURRENT_PROGRESS;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_DIALOG_MAX_PROGRESS;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_DIALOG_MESSAGE;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.KEYBUNDLE_DIALOG_TITLE;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_CANCEL_BUTTON_PRESSED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_CANCELED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_ERROR;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_FINISHED;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_START;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_OPERATION_SUCCESSFULLY;
import static it.Ettore.egalfilemanager.filemanager.thread.BaseProgressService.WHAT_UPDATE_PROGRESS;


/**
 * Classe handler generica che mette in comunicazione un service con una progress dialog
 */
public abstract class BaseProgressHandler extends Handler {
    private WeakReference<Activity> activity;
    private CustomProgressDialog progressDialog;
    private boolean activityDestroyed;
    private ColoredProgressDialog dialogCompletamentoOperazione;


    /**
     *
     * @param activity Activity
     */
    public BaseProgressHandler(@NonNull Activity activity) {
        this.activity = new WeakReference<>(activity);
        this.progressDialog = new CustomProgressDialog(activity);
    }


    /**
     * Mostra la progress dialog
     * @param title Titolo della dialog
     * @param message Messaggio della dialog
     * @param classService Classe del servizio in cui notificare la pressione del tasto annulla
     */
    private void showDialog(String title, String message, Class<? extends BaseProgressService> classService){
        if(activity.get() != null && !activity.get().isFinishing()) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.addCancelButton((DialogInterface dialogInterface, int i) -> {
                if(activity.get() != null && BaseProgressService.isRunning()) {
                    if(classService != null) {
                        activity.get().startService(BaseProgressService.makeStopIntent(activity.get(), classService));
                    }
                }
            });
            progressDialog.show();
        }
        if(activity.get() != null){
            LockScreenOrientation.lock(activity.get());
        }
    }


    /**
     * Mostra una notifica (da usare al posto dei toast o della dialogs quando l'activity è stata distrutta)
     * @param text Testo della notifica (visualizzato come titolo)
     */
    protected void mostraNotifica(String text){
        new NotificationChannelManager(activity.get()).creaChannelOperazioni();
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(activity.get(), NOTIF_CHANNEL_ID_OPERAZIONI)
                .setSmallIcon(R.drawable.ic_status_bar)
                .setContentTitle(activity.get().getString(R.string.operazione_terminata))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{100L, 100L, 200L, 200L})
                .setColor(ContextCompat.getColor(activity.get(), R.color.colorAccent));
        final NotificationManager notificationManager = (NotificationManager) activity.get().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID_RICERCA_DUPLICATI_TERMINATA, notifBuilder.build());
    }


    /**
     * Chiude la dialog e rilascia l'orientamento.
     */
    private void dismissProgressDialog(){
        LockScreenOrientation.unlock(activity.get());
        try {
            if (activity.get() != null && !activity.get().isFinishing() && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
    }


    /**
     * Chiude la dialog e rilascia l'orientamento. Chiamarlo sempre nell'onDestroy di un fragment che usa questo handler
     */
    public void dismissProgressDialogOnDestroy(){
        this.activityDestroyed = true;
        dismissProgressDialog();
        dismissDialogCompletamentoOperazione();
    }


    /**
     * Chiude la dialog di completamento operazione
     */
    private void dismissDialogCompletamentoOperazione(){
        try {
            if (dialogCompletamentoOperazione != null && dialogCompletamentoOperazione.isShowing()) {
                dialogCompletamentoOperazione.dismiss();
            }
        } catch (final IllegalArgumentException ignored) {}
    }


    /**
     * Restituisce la classe del service in base al nome
     * @param className Nome della classe del service
     * @return Classe del service. Null in caso di errore
     */
    @Nullable
    private Class<? extends BaseProgressService> getServiceClassFromName(String className){
        try {
            final Class<?> cls = Class.forName(className);
            return cls.asSubclass(BaseProgressService.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Verifica se l'activity è stata distrutta
     * @return True se l'activity è distrutta
     */
    protected boolean isActivityDestroyed(){
        return this.activityDestroyed;
    }


    /**
     * Restituisce un weak reference dell'activity
     * @return Activity impostata
     */
    protected WeakReference<Activity> getActivity(){
        return this.activity;
    }


    /**
     * Chiamato quando si riceve un messaggio da parte del service
     * @param msg Messaggio ricevuto
     */
    @Override
    public void handleMessage(Message msg) {
        if(activity.get() == null || activity.get().isFinishing()){
            return;
        }
        final Bundle data = msg.getData();
        final String dialogMessage = data.getString(KEYBUNDLE_DIALOG_MESSAGE);
        switch (msg.what) {
            case WHAT_OPERATION_START:
                //inviato quando inizia un'operazione, creo la dialog
                final String serviceClassName = data.getString(KEYBUNDLE_CLASSE);
                showDialog(data.getString(KEYBUNDLE_DIALOG_TITLE), dialogMessage, getServiceClassFromName(serviceClassName));
                break;
            case WHAT_OPERATION_CANCELED:
                //inviato quando viene annullata l'operazione, chiudo la dialog e mostro il toast
                dismissProgressDialog();
                ColoredToast.makeText(activity.get(), R.string.operazione_annulata, Toast.LENGTH_LONG).show();
                break;
            case WHAT_OPERATION_ERROR:
                //inviato quando l'operazione termina con errori
                dismissProgressDialog();
                dismissDialogCompletamentoOperazione();
                if(!activityDestroyed){
                    CustomDialogBuilder.make(activity.get(), dialogMessage, CustomDialogBuilder.TYPE_ERROR).show();
                } else {
                    mostraNotifica(dialogMessage);
                }
                break;
            case WHAT_OPERATION_SUCCESSFULLY:
                //inviato quando l'operazione termina con successo
                dismissProgressDialog();
                dismissDialogCompletamentoOperazione();
                if(dialogMessage != null){
                    if(!activityDestroyed) {
                        ColoredToast.makeText(activity.get(), dialogMessage, Toast.LENGTH_LONG).show();
                    } else {
                        mostraNotifica(dialogMessage);
                    }
                }
                break;
            case WHAT_UPDATE_PROGRESS:
                //inviato durante l'operazione, aggiorno la dialog con i progressi
                if(activity.get() != null && !activity.get().isFinishing() && progressDialog.isShowing()) {
                    if (dialogMessage != null) {
                        progressDialog.setMessage(dialogMessage);
                    }
                    int progress = data.getInt(KEYBUNDLE_DIALOG_CURRENT_PROGRESS, 0);
                    int max = data.getInt(KEYBUNDLE_DIALOG_MAX_PROGRESS, 0);
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(max);
                    progressDialog.setProgress(progress);
                }
                break;
            /*case WHAT_REMOVE_CANCEL_BUTTON:
                //inviato quando si vuole nascondere il tasto "annulla" perchè l'operazione non è annullabile
                if(activity.get() != null && !activity.get().isFinishing() && progressDialog.isShowing()) {
                    progressDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(View.GONE);
                }
                break;*/
            case WHAT_CANCEL_BUTTON_PRESSED:
                //inviato quando viene premuto il tasto "annulla" dalla notifica
                if(activity.get() != null && !activity.get().isFinishing() && !activityDestroyed && dialogCompletamentoOperazione == null) {
                    //mostro la dialog solo se è null (se non è già stata mostrata)
                    dialogCompletamentoOperazione = ColoredProgressDialog.show(activity.get(), null, activity.get().getString(R.string.annullamento_operazione), true, false);
                }
                break;
            case WHAT_OPERATION_FINISHED:
                dismissDialogCompletamentoOperazione();
                break;
        }
    }
}
