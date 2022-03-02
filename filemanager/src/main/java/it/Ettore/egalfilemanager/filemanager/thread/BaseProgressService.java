package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.NotificationChannelManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.copyutils.FileDataWrapper;

import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_BACKGROUND;


/**
 * Classe generica per servizi che eseguono operazioni con progress dialog
 */
public abstract class BaseProgressService extends IntentService {
    private static final String ACTION_STOP_SERVICE = "action_stop_service";
    protected static final int FREQ_AGGIORNAMENTO = 500;

    public static final String KEYBUNDLE_LISTENER_DATA = "listener_data";
    protected static final String KEYBUNDLE_DIALOG_TITLE = "dialog_title";
    protected static final String KEYBUNDLE_DIALOG_MESSAGE = "dialog_message";
    protected static final String KEYBUNDLE_DIALOG_CURRENT_PROGRESS = "dialog_current_progress";
    protected static final String KEYBUNDLE_DIALOG_MAX_PROGRESS = "dialog_max_progress";
    protected static final String KEYBUNDLE_CLASSE = "classe";

    private static final String KEYBUNDLE_MESSENGER = "messenger";
    private static final String KEYBUNDLE_FILE_DATA_WRAPPER = "file_data_wrapper";
    protected static final String KEYBUNDLE_PATH_DESTINAZIONE = "path_destinazione";

    public static final int WHAT_OPERATION_CANCELED = 1;
    public static final int WHAT_OPERATION_START = 2;
    public static final int WHAT_OPERATION_SUCCESSFULLY = 3;
    public static final int WHAT_OPERATION_ERROR = 4;
    public static final int WHAT_UPDATE_PROGRESS = 5;
    public static final int WHAT_MEDIA_SCANNER_FINISHED = 6;
    public static final int WHAT_CANCEL_BUTTON_PRESSED = 7;
    public static final int WHAT_OPERATION_FINISHED = 8;

    private static boolean isRunning;
    private NotificationCompat.Builder notif;
    private Messenger messenger;
    private List<String> pathFiles;


    /**
     * Costruttore
     * @param name Nome del service
     */
    public BaseProgressService(String name) {
        super(name);
    }


    /**
     * Crea un intent con i parametri generali (da configurare nella sottoclasse)
     * @param context Context chiamante
     * @param cls Classe del servizio
     * @param listaPaths Lista con i paths dei files da copiare
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent generale
     */
    protected static Intent makeStartBaseIntent(@NonNull Context context, @NonNull Class<? extends BaseProgressService> cls, ArrayList<String> listaPaths, @NonNull BaseProgressHandler handler) {
        final Intent intent = new Intent(context, cls);
        final FileDataWrapper fileDataWrapper = new FileDataWrapper(); //immagazzino i dati grandi in un oggetto serializzabile
        fileDataWrapper.listaPaths = listaPaths;
        intent.putExtra(KEYBUNDLE_FILE_DATA_WRAPPER, fileDataWrapper);
        intent.putExtra(KEYBUNDLE_MESSENGER, new Messenger(handler));
        return intent;
    }


    /**
     * Utilità per creare l'intent per fermare il service
     * @param context Context chiamante
     * @param cls Classe del servizio
     * @return Intent per fermare il service
     */
    public static Intent makeStopIntent(@NonNull Context context, @NonNull Class<? extends BaseProgressService> cls){
        final Intent stopIntent = new Intent(context, cls);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        return stopIntent;
    }


    /**
     * Restituisce un boolean per verificare lo stato di avviamento del service
     * @return True se il service è avviato
     */
    public static boolean isRunning(){
        return isRunning;
    }


    /**
     * Interrompe il servizio
     */
    /*public static void interrompi(){
        isRunning = false;
    }*/


    @Override
    public void onCreate(){
        super.onCreate();

        //creo la notifica
        new NotificationChannelManager(this).creaChannelBackground();
        notif = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_BACKGROUND)
                .setSmallIcon(R.drawable.ic_status_bar)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle())
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        //intent per fermare la notifica
        final Intent stopIntent = makeStopIntent(this, getClass());
        final PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notif.addAction(android.R.drawable.ic_input_delete, getString(android.R.string.cancel), servicePendingIntent);

        //intent mediante il quale cliccando sulla notifica si mostra l'activity (se non è più in primo piano)
        final Intent activityIntent = new Intent(getApplicationContext(), ActivityMain.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(getApplicationContext(), new Random().nextInt(), activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.setContentIntent(activityPendingIntent);

        //mostro la notifica con startforeground per evitare che il service venga eliminato da Android
        startForeground(NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO, notif.build());
    }


    /**
     * Chiamato quando il service viene avviato
     * @param intent .
     * @param flags .
     * @param startId .
     * @return .
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //l'action STOP SERVICE viene passata dal pending intent della notifica quando si desidera interrompere il service
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            //stopForeground(true);
            isRunning = false;
            sendMessageCancelButtonPressed();
            sendMessageCanceled();
            return Service.START_NOT_STICKY;
        }

        if(!isRunning) {
            //se il servizio era stato errestato in una sessione precedente, riavvio la notifica
            startForeground(NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO, notif.build());
        }

        //operazioni all'avvio standard del service
        isRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Ottiene i dati passati all'intent e inizializza le variabili
     * @param intent Intent
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onHandleIntent(@Nullable Intent intent) {
        messenger = intent.getParcelableExtra(KEYBUNDLE_MESSENGER);
        final FileDataWrapper fileDataWrapper = (FileDataWrapper)intent.getSerializableExtra(KEYBUNDLE_FILE_DATA_WRAPPER);
        pathFiles = fileDataWrapper.listaPaths;
    }


    /**
     * Notifica all'handler che l'operazione è stata annullata
     */
    protected void sendMessageCanceled(){
        isRunning = false; //non rimuovere
        final Message message = Message.obtain(null, WHAT_OPERATION_CANCELED);
        final Bundle bundle = new Bundle();
        bundle.putSerializable(KEYBUNDLE_LISTENER_DATA, creaDatiPerListener());
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica all'handler che l'operazione è iniziata
     * @param dialogTitle Titolo della dialog
     * @param dialogMessage Messaggio della dialog, o null
     */
    protected void sendMessageStartOperation(@NonNull String dialogTitle, String dialogMessage){
        //aggiorno la dialog
        final Message message = Message.obtain(null, WHAT_OPERATION_START);
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_DIALOG_TITLE, dialogTitle);
        bundle.putString(KEYBUNDLE_DIALOG_MESSAGE, dialogMessage != null ? dialogMessage : "");
        bundle.putString(KEYBUNDLE_CLASSE, getClass().getCanonicalName()); //passo la classe del service corrente
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //aggiorno la notifica
        notif.setContentTitle(dialogTitle);
        notif.setContentText(dialogMessage != null ? dialogMessage : "");
        startForeground(NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO, notif.build());
    }


    /**
     * Notifica all'handler che l'operazione è iniziata
     * @param dialogTitle Titolo della dialog
     * @param dialogMessage Messaggio della dialog
     */
    protected void sendMessageStartOperation(@StringRes int dialogTitle, @StringRes int dialogMessage){
        sendMessageStartOperation(getString(dialogTitle), getString(dialogMessage));
    }


    /**
     * Notifica all'handler che l'operazione non è andata a buon fine
     * @param errorMessage Messaggio di errore da visualizzare nella dialog
     */
    protected void sendMessageError(String errorMessage){
        //aggiorno la dialog
        final Message message = Message.obtain(null, WHAT_OPERATION_ERROR);
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_DIALOG_MESSAGE, errorMessage);
        bundle.putSerializable(KEYBUNDLE_LISTENER_DATA, creaDatiPerListener());
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //aggiorno la notifica
        isRunning = false;
        stopForeground(true); //rimuove la notifica se l'app viene chiusa e rimane solo il service
    }


    /**
     * Notifica all'handler che l'operazione non è andata a buon fine
     * @param errorMessage Messaggio di errore da visualizzare nella dialog
     */
    protected void sendMessageError(@StringRes int errorMessage){
        sendMessageError(getString(errorMessage));
    }


    /**
     * Notifica all'handler che l'operazione è andata a buon fine
     * @param dialogMessage Messaggio da visualizzare nel toast di completamento. Null se non si vuole visualizzare il toast
     */
    protected void sendMessageSuccessfully(String dialogMessage){
        //aggiorno la dialog
        final Message message = Message.obtain(null, WHAT_OPERATION_SUCCESSFULLY);
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_DIALOG_MESSAGE, dialogMessage);
        bundle.putSerializable(KEYBUNDLE_LISTENER_DATA, creaDatiPerListener());
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //aggiorno la notifica
        isRunning = false;
        stopForeground(true); //rimuove la notifica se l'app viene chiusa e rimane solo il service
    }


    /**
     * Notifica all'handler che l'operazione è andata a buon fine
     * @param dialogMessage Messaggio da visualizzare nel toast di completamento. Null se non si vuole visualizzare il toast
     */
    protected void sendMessageSuccessfully(@StringRes int dialogMessage){
        sendMessageSuccessfully(getString(dialogMessage));
    }


    /**
     * Notifica all'handler il progresso dell'operazione
     * @param dialogMessage testo da visualizzare nella dialog (se null viene visualizzato il testo impostato all'avvio della dialog)
     * @param progress progresso corrente
     * @param max progresso massimo
     */
    protected void sendMessageUpdateProgress(String dialogMessage, int progress, int max){
        if(!isRunning){
            return;
        }
        //aggiorno la dialog
        final Message message = Message.obtain(null, WHAT_UPDATE_PROGRESS);
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_DIALOG_MESSAGE, dialogMessage);
        bundle.putInt(KEYBUNDLE_DIALOG_CURRENT_PROGRESS, progress);
        bundle.putInt(KEYBUNDLE_DIALOG_MAX_PROGRESS, max);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //aggiorno la notifica
        try {
            int percent = progress * 100 / max;
            notif.setContentText(String.valueOf(percent) + "%");
            notif.setProgress(max, progress, false);
        } catch (ArithmeticException ignored){}
        startForeground(NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO, notif.build());
    }


    /**
     * Notifica all'handler il progresso dell'operazione
     * @param dialogMessage testo da visualizzare nella dialog (se null viene visualizzato il testo impostato all'avvio della dialog)
     * @param progress progresso corrente
     * @param max progresso massimo
     */
    protected void sendMessageUpdateProgress(@StringRes int dialogMessage, int progress, int max){
        sendMessageUpdateProgress(getString(dialogMessage), progress, max);
    }


    /**
     * Notifica all'handler il progresso dell'operazione
     * @param progress progresso corrente
     * @param max progresso massimo
     */
    protected void sendMessageUpdateProgress(int progress, int max){
        sendMessageUpdateProgress(null, progress, max);
    }


    /**
     * Notifica all'handler che il media scanner ha finito
     */
    protected void sendMessageMediaScannerFinished(){
        final Message message = Message.obtain(null, WHAT_MEDIA_SCANNER_FINISHED);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica all'handler che è stato premuto il tasto annulla
     */
    private void sendMessageCancelButtonPressed(){
        final Message message = Message.obtain(null, WHAT_CANCEL_BUTTON_PRESSED);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        notif.setContentText(getString(R.string.annullamento_operazione));
        notif.mActions.clear();
        startForeground(NOTIF_ID_OPERAZIONE_GENERICA_IN_CORSO, notif.build());
    }


    /**
     * Notifica all'handler che tutte le operazioni sono terminate
     * Chiamare come ultima operazione del service e sempre prima di un return all'interno di un onHandleIntent
     */
    protected void sendMessageOperationFinished(){
        final Message message = Message.obtain(null, WHAT_OPERATION_FINISHED);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopForeground(true);
    }


    /**
     * Restituisce la lista di path passati tramite intent
     * @return Lista di path dei files
     */
    protected List<String> getListaPaths(){
        return pathFiles;
    }


    /**
     * Implementare questo metodo per restituire un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener. Null se non ci sono dati da passare
     */
    protected abstract Serializable creaDatiPerListener();
}
