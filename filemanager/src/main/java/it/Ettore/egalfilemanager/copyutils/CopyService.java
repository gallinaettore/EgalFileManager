package it.Ettore.egalfilemanager.copyutils;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.NotificationChannelManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;

import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_COPIA_IN_CORSO;
import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_RICERCA_DUPLICATI_IN_CORSO;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_BACKGROUND;



/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Servizio generico per la copia di files
 */
public abstract class CopyService extends IntentService {
    private static final String ACTION_STOP_SERVICE = "action_stop_service";
    protected static final int FREQ_AGGIORNAMENTO = 500;

    private static final String KEYBUNDLE_MESSENGER = "messenger";
    private static final String KEYBUNDLE_FILE_DATA_WRAPPER = "file_data_wrapper";

    public static final String KEYBUNDLE_TOT_FILES = "tot_files";
    public static final String KEYBUNDLE_TOT_SIZE = "tot_size";
    public static final String KEYBUNDLE_CANCELLA_ORIGINE = "cancella_origine";
    public static final String KEYBUNDLE_NOME_FILE = "nome_file";
    public static final String KEYBUNDLE_PATH_PARENT = "path_parent";
    public static final String KEYBUNDLE_PATH_DESTINAZIONE = "path_destinazione";
    public static final String KEYBUNDLE_DIMENSIONE_FILE = "dimensione_file";
    public static final String KEYBUNDLE_INDICE_FILE = "indice_file";
    public static final String KEYBUNDLE_BYTES_COPIATI_FILE = "bytes_copiati_file";
    public static final String KEYBUNDLE_TOT_BYTES_COPIATI = "tot_bytes_copiati";
    public static final String KEYBUNDLE_MESSAGGIO = "messaggio";
    public static final String KEYBUNDLE_SUCCESS = "success";
    public static final String KEYBUNDLE_FILES_COPIATI = "files_copiati";
    public static final String KEYBUNDLE_TIPO_COPIA = "tipo_copia";

    public static final int WHAT_START_COPY = 1;
    public static final int WHAT_UPDATE_FILE = 2;
    public static final int WHAT_UPDATE_PROGRESS = 3;
    public static final int WHAT_MESSAGE = 4;
    public static final int WHAT_MEDIA_SCANNER_FINISHED = 5;
    public static final int WHAT_COPY_FINISHED = 6;
    public static final int WHAT_COPY_CANCELED = 7;

    public static final int COPY_LOCAL_TO_LOCAL = 1;
    public static final int COPY_LOCAL_TO_SMB = 2;
    public static final int COPY_SMB_TO_LOCAL = 3;
    public static final int COPY_SMB_TO_SMB = 4;
    public static final int COPY_FTP_TO_LOCAL = 5;
    public static final int COPY_LOCAL_TO_FTP = 6;
    public static final int COPY_FTP_TO_FTP = 7;

    private static boolean isRunning;
    private NotificationCompat.Builder notif;
    private Messenger messenger;
    private boolean cancellaOrigine;
    private long totSize, totWrited;
    private List<String> listaPathDaCopiare, pathsFilesNonProcessati;
    private ArrayList<String> pathsFilesProcessati;
    private int indiceFile, totFiles;
    private String pathDestinazione;
    private Map<String, Integer> azioniPathsGiaPresenti;





    /**
     * Costruttore
     * @param name Nome del service
     */
    public CopyService(String name) {
        super(name);
    }



    /**
     * Crea un intent con i parametri generali
     * @param context Context chiamante
     * @param cls Classe del servizio
     * @param listaPaths Lista con i paths dei files da copiare
     * @param pathDestinazione Path della cartella di destinazione
     * @param azioniPathsGiaPresenti Map contenente le associazioni path-azione da eseguire
     * @param copyHandler Handler che permette al service di comunicare con la UI
     * @param totSize Dimensione totale in bytes dei files da copiare
     * @param totFiles Numero totale di files da copiare
     * @param cancellaOrigine True se in modalità taglia. False modalità copia.
     * @return Intent generale
     */
    protected static Intent makeStartIntent(@NonNull Context context, Class<?> cls, ArrayList<String> listaPaths, String pathDestinazione, HashMap<String, Integer> azioniPathsGiaPresenti,
                                            CopyHandler copyHandler, long totSize, int totFiles, boolean cancellaOrigine){
        final Intent intent = new Intent(context, cls);

        final FileDataWrapper fileDataWrapper = new FileDataWrapper(); //immagazzino i dati grandi in un oggetto serializzabile
        fileDataWrapper.listaPaths = listaPaths;
        fileDataWrapper.pathDestinazione = pathDestinazione;
        fileDataWrapper.azioniPathsGiaPresenti = azioniPathsGiaPresenti;
        intent.putExtra(KEYBUNDLE_FILE_DATA_WRAPPER, fileDataWrapper);
        intent.putExtra(KEYBUNDLE_MESSENGER, new Messenger(copyHandler));
        intent.putExtra(KEYBUNDLE_TOT_SIZE, totSize);
        intent.putExtra(KEYBUNDLE_TOT_FILES, totFiles);
        intent.putExtra(KEYBUNDLE_CANCELLA_ORIGINE, cancellaOrigine);
        return intent;
    }


    /**
     * Utilità per creare l'intent per fermare il service
     * @param context Context chiamante
     * @param cls Classe del servizio
     * @return Intent per fermare il service
     */
    private static Intent makeStopIntent(@NonNull Context context, @NonNull Class cls){
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
    public static void interrompi(){
        isRunning = false;
    }


    @Override
    public void onCreate(){
        super.onCreate();

        //creo la notifica
        new NotificationChannelManager(this).creaChannelBackground();
        notif = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_BACKGROUND)
                .setSmallIcon(R.drawable.ic_notif_copia)
                .setContentTitle(getString(R.string.copia_in_corso, ""))
                .setContentText("0%")
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle())
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        //intent per fermare la notifica
        final Intent serviceIntent = makeStopIntent(this, getClass());
        final PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notif.addAction(android.R.drawable.ic_input_delete, getString(android.R.string.cancel), servicePendingIntent);

        //intent mediante il quale cliccando sulla notifica si mostra l'activity (se non è più in primo piano)
        final Intent activityIntent = new Intent(getApplicationContext(), ActivityMain.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(getApplicationContext(), new Random().nextInt(), activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.setContentIntent(activityPendingIntent);

        //mostro la notifica con startforeground per evitare che il service venga eliminato da Android
        startForeground(NOTIF_ID_COPIA_IN_CORSO, notif.build());
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
            stopForeground(true);
            isRunning = false;
            sendMessageCanceled();
            return Service.START_NOT_STICKY;
        }

        if(!isRunning){
            //se il servizio era stato errestato in una sessione precedente, riavvio la notifica
            startForeground(NOTIF_ID_RICERCA_DUPLICATI_IN_CORSO, notif.build());
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
        cancellaOrigine = intent.getBooleanExtra(KEYBUNDLE_CANCELLA_ORIGINE, false);
        totSize = intent.getLongExtra(KEYBUNDLE_TOT_SIZE, 0L);
        totFiles = intent.getIntExtra(KEYBUNDLE_TOT_FILES, 0);

        final FileDataWrapper fileDataWrapper = (FileDataWrapper)intent.getSerializableExtra(KEYBUNDLE_FILE_DATA_WRAPPER);
        pathDestinazione = fileDataWrapper.pathDestinazione;
        listaPathDaCopiare = fileDataWrapper.listaPaths;
        azioniPathsGiaPresenti = fileDataWrapper.azioniPathsGiaPresenti;

        pathsFilesNonProcessati = new ArrayList<>();
        pathsFilesProcessati = new ArrayList<>();
        indiceFile = 0;
        totWrited = 0L;
    }


    /**
     * Notifica all'handler che la copia è stata annullata
     */
    protected void sendMessageCanceled(){
        isRunning = false; //non rimuovere
        final Message message = Message.obtain(null, WHAT_COPY_CANCELED);
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_PATH_DESTINAZIONE, pathDestinazione);
        bundle.putStringArrayList(KEYBUNDLE_FILES_COPIATI, pathsFilesProcessati);
        bundle.putInt(KEYBUNDLE_TIPO_COPIA, getTipoCopia());
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica all'handler che la copia è iniziata
     */
    protected void sendMessageStartCopy(){
        final Message message = Message.obtain(null, WHAT_START_COPY);
        final Bundle bundle = new Bundle();
        bundle.putInt(KEYBUNDLE_TOT_FILES, totFiles);
        bundle.putLong(KEYBUNDLE_TOT_SIZE, totSize);
        bundle.putBoolean(KEYBUNDLE_CANCELLA_ORIGINE, cancellaOrigine);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica all'handler che un nuovo file sta per essere copiato (per aggiornare la dialog con i nuovi riferimenti del file)
     * @param nomeFile Nome del file corrente
     * @param pathParent Path della cartella parent
     * @param pathDestinazione Path della cartella di destinazione
     * @param dimensioneFile Dimensione del file in bytes
     */
    protected void sendMessageUpdateFile(String nomeFile, String pathParent, String pathDestinazione, long dimensioneFile){
        if(nomeFile == null) nomeFile = "";
        if(pathParent == null) pathParent = "";
        if(pathDestinazione == null) pathDestinazione = "";
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_NOME_FILE, nomeFile);
        bundle.putString(KEYBUNDLE_PATH_PARENT, pathParent);
        bundle.putString(KEYBUNDLE_PATH_DESTINAZIONE, pathDestinazione);
        bundle.putLong(KEYBUNDLE_DIMENSIONE_FILE, dimensioneFile);
        bundle.putInt(KEYBUNDLE_INDICE_FILE, indiceFile);

        final Message message = Message.obtain(null, WHAT_UPDATE_FILE);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        notif.setContentTitle(getString(R.string.copia_in_corso, nomeFile));
        startForeground(NOTIF_ID_COPIA_IN_CORSO, notif.build());
    }


    /**
     * Notifica all'handler il progresso di copia
     * @param bytesFileCopiati Bytes del file copiati
     */
    protected void sendMessageUpdateProgress(long bytesFileCopiati){
        final Bundle bundle = new Bundle();
        bundle.putLong(KEYBUNDLE_BYTES_COPIATI_FILE, bytesFileCopiati);
        bundle.putLong(KEYBUNDLE_TOT_BYTES_COPIATI, totWrited);
        final Message message = Message.obtain(null, WHAT_UPDATE_PROGRESS);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int percent = (int) (totWrited * 100 / totSize);
            notif.setContentText(percent + "%");
            notif.setProgress(100, percent, false);
        } catch (ArithmeticException ignored){}

        startForeground(NOTIF_ID_COPIA_IN_CORSO, notif.build());
    }


    /**
     * Notifica all'handler di mostrare un messaggio nella dialog
     * @param text Testo da mostrare
     */
    protected void sendTextMessage(String text){
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_MESSAGGIO, text);
        final Message message = Message.obtain(null, WHAT_MESSAGE);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica all'handler che la copia è terminata
     */
    protected void sendMessageCopyFinished(){
        final Bundle bundle = new Bundle();
        bundle.putString(KEYBUNDLE_MESSAGGIO, makeResultMessage());
        bundle.putBoolean(KEYBUNDLE_SUCCESS, pathsFilesNonProcessati.isEmpty());
        bundle.putString(KEYBUNDLE_PATH_DESTINAZIONE, pathDestinazione);
        bundle.putStringArrayList(KEYBUNDLE_FILES_COPIATI, pathsFilesProcessati);
        bundle.putInt(KEYBUNDLE_TIPO_COPIA, getTipoCopia());
        final Message message = Message.obtain(null, WHAT_COPY_FINISHED);
        message.setData(bundle);
        try {
            messenger.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRunning = false;
        stopForeground(true); //rimuove la notifica se l'app viene chiusa e rimane solo il service
    }


    /**
     * Notifica all'handler che il media scanner ha terminato la scansione
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
     * Crea il messaggio da mostrare al termine della copia
     * @return Messaggio da mostrare al termine della copia
     */
    private String makeResultMessage(){
        if(pathsFilesNonProcessati.isEmpty()){
            //tutti files sono stati copiati correttamente
            String message = getString(R.string.files_copiati); //copia
            if(cancellaOrigine){ //taglia
                message = getString(R.string.files_spostati);
            }
            return String.format(message, String.valueOf(pathsFilesProcessati.size()));
        } else {
            //Mostro una dialog con la lista di files non processati
            final StringBuilder sb = new StringBuilder(getString(R.string.files_non_copiati));
            sb.append("\n");
            for(String path : pathsFilesNonProcessati){
                sb.append(String.format("\n• %s", path));
            }
            return sb.toString();
        }
    }


    /**
     * Aggiunge il path alla lista che contiene i paths non processati
     * @param path Path da aggiungere
     */
    protected void aggiungiPathNonProcessato(String path){
        pathsFilesNonProcessati.add(path);
    }


    /**
     * Aggiunge il path alla lista che contiene i paths processati
     * @param path Path da aggiungere
     */
    protected void aggiungiPathProcessato(String path){
        pathsFilesProcessati.add(path);
    }


    /**
     * Incrementa di 1 l'indice del file corrente
     */
    protected void incrementaIndiceFile(){
        indiceFile++;
    }


    /**
     * Incrementa il numero di bytes scritti aggiungendo altri bytes
     * @param bytesWrited Bytes da aggiungere
     */
    protected void incrementaTotWrited(long bytesWrited){
        totWrited += bytesWrited;
    }


    /**
     * Restituisce la modalità /taglia/copia impostata
     * @return True modalità taglia. False modalità copia
     */
    protected boolean isCancellaOrigine(){
        return cancellaOrigine;
    }


    /**
     * Restituisce il path di destinazione
     * @return Path di destinazione
     */
    protected String getPathDestinazione(){
        return this.pathDestinazione;
    }


    /**
     * Restituisce una map con path-azione
     * @return Map con path-azione
     */
    protected Map<String, Integer> getAzioniPathsGiaPresenti(){
        return azioniPathsGiaPresenti;
    }


    /**
     * Verifica se tutte le azioni della Map sono di ignoare il file corrente
     * @return True se tutte le azioni della Map sono di ignoare il file corrente. False se la map è vuota e se contiene anche altre azioni (diverse da IGNORA)
     */
    protected boolean tutteAzioniIgnoraFiles(){
        final Map<String, Integer> mapAzioni = getAzioniPathsGiaPresenti();
        if(mapAzioni.isEmpty()){
            return false;
        }
        for(Map.Entry<String, Integer> entry : mapAzioni.entrySet()){
            if(entry.getValue() != SovrascritturaFiles.AZIONE_IGNORA){
                return false;
            }
        }
        return true;
    }


    /**
     * Restituisce la lista di paths da copiare
     * @return Paths da copiare
     */
    protected List<String> getListaPathDaCopiare(){
        return listaPathDaCopiare;
    }


    /**
     * Restituisce il tipo di copia effettuato da servizio
     * @return Una della variabili COPY di questa classe (specifica se la copia è avvenuta ad esempio da smb a locale)
     */
    protected abstract int getTipoCopia();
}
