package it.Ettore.egalfilemanager.tools.duplicati;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;

import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_RICERCA_DUPLICATI_IN_CORSO;
import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_RICERCA_DUPLICATI_TERMINATA;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_BACKGROUND;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_OPERAZIONI;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per la ricerca in background di files duplicati
 */
public class CercaFilesDuplicatiService extends IntentService {

    public static final String KEYBUNDLE_DUPLICATI_TROVATI = "duplicati_trovati";
    public static final String KEYBUNDLE_FINISH = "finish";
    public static final String KEYBUNDLE_CANCELED = "canceled";

    private static final String ACTION_STOP_SERVICE = "action_stop_service";
    private static final String KEYBUNDLE_START_FOLDER = "start_folder";
    private static final String KEYBUNDLE_MESSENGER = "messenger";

    private MessageDigest md;
    private FileManager fileManager;
    private int duplicati;
    private Messenger messenger;
    private List<File> cartelleAndroidDaEscludere;
    private NotificationCompat.Builder notifRicercaBuilder;
    private static boolean isRunning;



    /**
     * Costruttore di default (obbligatori)
     */
    public CercaFilesDuplicatiService(){
        super("CercaFilesDuplicatiService");
    }




    /**
     * Utilità per creare l'intent per avviare in service
     * @param context Context dell'activity chiamante
     * @param startFolders Lista di cartelle da cui iniziare l'analisi
     * @return Intent per avviare il service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull SerializableFileList startFolders, @NonNull Handler handler){
        final Intent intent = new Intent(context, CercaFilesDuplicatiService.class);
        intent.putExtra(KEYBUNDLE_START_FOLDER, startFolders);
        intent.putExtra(KEYBUNDLE_MESSENGER, new Messenger(handler));
        return intent;
    }


    /**
     * Utilità per creare l'intent per fermare il service
     * @param context Context chiamante
     * @return Intent per fermare il service
     */
    public static Intent createStopIntent(@NonNull Context context){
        final Intent stopIntent = new Intent(context, CercaFilesDuplicatiService.class);
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


    @Override
    public void onCreate() {
        super.onCreate();

        notifRicercaBuilder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_BACKGROUND)
                .setSmallIcon(R.drawable.ic_status_bar)
                .setContentTitle(getString(R.string.ricerca_duplicati_in_corso))
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        final Intent serviceIntent = createStopIntent(this);
        final PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notifRicercaBuilder.addAction(android.R.drawable.ic_input_delete, getString(android.R.string.cancel), servicePendingIntent);

        //mostro la notifica con startforeground per evitare che il service venga eliminato da Android
        startForeground(NOTIF_ID_RICERCA_DUPLICATI_IN_CORSO, notifRicercaBuilder.build());
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

        if(!isRunning) {
            //se il servizio era stato errestato in una sessione precedente, riavvio la notifica
            startForeground(NOTIF_ID_RICERCA_DUPLICATI_IN_CORSO, notifRicercaBuilder.build());
        }

        //operazioni all'avvio standard del service
        isRunning = true;

        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Esecuzione in background
     * @param intent .
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //parametri passati all'intent

        final SerializableFileList startDirectories = (SerializableFileList)intent.getSerializableExtra(KEYBUNDLE_START_FOLDER);
        messenger = intent.getParcelableExtra(KEYBUNDLE_MESSENGER);
        duplicati = 0; //reimposto la variabile (se riavvio il servizio altrimenti parte da dove era rimasta)
        cartelleAndroidDaEscludere = new ArrayList<>();
        for(File startDirectory : startDirectories){
            final File cartellaAndroidDaEscludere = new File(startDirectory, "Android");
            cartelleAndroidDaEscludere.add(cartellaAndroidDaEscludere);
        }

        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(md == null || startDirectories.isEmpty()){
            isRunning = false;
            sendMessageCanceled();
            return;
        }
        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();

        //avvio l'analisi
        final Map<String, List<String>> map = new HashMap<>();
        try {
            for(File startDirectory : startDirectories){
                find(map, startDirectory);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //creo una lista che contiene i gruppi di files duplicati. Ogni gruppo contiene una lista di path con i files uguali.
        final List<List<String>> listaDuplicati = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if(entry.getValue().size() > 1){
                final List<String> paths = entry.getValue();
                Collections.sort(paths);
                listaDuplicati.add(paths);
            }
        }
        //salvo i risultati su un file che sarà letto dal fragment che li mostrerà
        final GruppiListSerializer listSerializer = new GruppiListSerializer(this);
        listSerializer.serialize(listaDuplicati);

        //se non è stato annullato il service mostro la notifica di completamento operazione
        if(isRunning){
            mostraNotificaRisultati(listaDuplicati.size());
            sendMessageDuplicateFound(duplicati, true);
        } else {
            sendMessageCanceled();
        }

        isRunning = false;
    }


    /**
     * Ricerca ricorsiva di files duplicati
     * @param map Map che contiene come chiave gli hash dei files trovati e come valore una lista di files con lo stesso contenuto
     * @param directory Directory da scansionare
     * @throws Exception Lanciata se non è stato possibile leggere il file
     */
    private void find(Map<String, List<String>> map, File directory) throws Exception  {
        String hash;
        List<File> filesContenuti = fileManager.ls(directory);
        final OrdinatoreFiles ordinatoreFiles = new OrdinatoreFiles(this);
        ordinatoreFiles.ottieniStatoMostraNascosti();
        filesContenuti = ordinatoreFiles.ordinaListaFiles(filesContenuti); //l'ordinamento rimuove anche i files nascosti (se non devono essere visualizzati)
        for (File child : filesContenuti) {
            if(!isRunning){
                return;
            }
            if (child.isDirectory()) {
                if(cartelleAndroidDaEscludere.contains(child)){
                    continue; //ignora i files contenuti nella cartella <sdcard>/Android/ che contiene dati delle applicazioni (cache a altro)
                }
                find(map, child);
            } else {
                try {
                    hash = getHash(child);
                    List<String> list = map.get(hash);
                    if (list == null) {
                        list = new LinkedList<>();
                        map.put(hash, list);
                    } else {
                        if(list.size() == 1) { //se la lista contiene già un file lo conteggio, se ne contiene più di uno è già stata conteggiata
                            duplicati++;
                            sendMessageDuplicateFound(duplicati, false);
                        }
                    }
                    list.add(child.getAbsolutePath());
                } catch (IOException e) {
                    throw new RuntimeException("cannot read file " + child.getAbsolutePath(), e);
                } catch (IllegalArgumentException ignored){} //salta i files vuoti
            }
        }
    }


    /**
     * Ricava l'hash del file
     * @param infile File da analizzare
     * @return Stringa con l'hash del file
     * @throws Exception Eccezione se avviene un'errore durante la generazione. IllegalArgumentException se il file è vuoto (0 bytes)
     */
    private String getHash(File infile) throws Exception {
        final RandomAccessFile file = new RandomAccessFile(infile, "r");

        int buffSize = 1024 * 16;
        byte[] buffer = new byte[buffSize];
        long read = 0L;

        // calculate the hash of the whole file for the test
        long offset = file.length();
        if(offset == 0){
            throw new IllegalArgumentException("File vuoto");
        }
        int unitsize;
        while (read < offset) {
            unitsize = (int) (((offset - read) >= buffSize) ? buffSize : (offset - read));
            file.read(buffer, 0, unitsize);
            md.update(buffer, 0, unitsize);
            read += unitsize;
        }

        file.close();
        return new BigInteger(1, md.digest()).toString(16);
    }


    /**
     * Invia al fragment un messaggio per aggiornare la view
     * @param numDuplicati Numero di gruppi di files duplicati trovati
     * @param finish True se è terminata la ricerca. False se la ricerca sta ancora continuando
     */
    private void sendMessageDuplicateFound(int numDuplicati, boolean finish){
        final Bundle bundle = new Bundle();
        bundle.putInt(KEYBUNDLE_DUPLICATI_TROVATI, numDuplicati);
        if(finish){
            bundle.putBoolean(KEYBUNDLE_FINISH, true);
        }
        final Message message = Message.obtain();
        message.setData(bundle);
        try {
            if(messenger != null) {
                messenger.send(message);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Invia al fragment un messaggio per aggiornare la view notificando che la ricerca è stata annullata
     */
    private void sendMessageCanceled(){
        final Bundle bundle = new Bundle();
        bundle.putBoolean(KEYBUNDLE_CANCELED, true);
        final Message message = Message.obtain();
        message.setData(bundle);
        try {
            if(messenger != null) {
                messenger.send(message);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * Mostra la notifica di ricerca terminata. Cliccando sulla notifica saranno mostrati i risultati
     * @param duplicatiTrovati Numero di duplicati trovati da mostrare nella notifica
     */
    private void mostraNotificaRisultati(int duplicatiTrovati){
        //intent per aprire l'activity main che si occuperà di mostrare il gragment dei risultati
        final Intent mostraRisultatiIntent = new Intent(getApplicationContext(), ActivityMain.class);
        //mostraRisultatiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mostraRisultatiIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mostraRisultatiIntent.setAction(Costanti.ACTION_RISULTATI_FILES_DUPLICATI);
        final PendingIntent mostraRisultatiPendingIntent = PendingIntent.getActivity(getApplicationContext(), new Random().nextInt(), mostraRisultatiIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //notifica
        final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final NotificationCompat.Builder notifRisultatoBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIF_CHANNEL_ID_OPERAZIONI)
                .setSmallIcon(R.drawable.ic_status_bar)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(String.format(getString(R.string.duplicati_trovati), String.valueOf(duplicatiTrovati)))
                .setContentText(getString(R.string.mostra_risultati_duplicati))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{100L, 100L, 200L, 200L})
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setContentIntent(mostraRisultatiPendingIntent);
        notifRisultatoBuilder.build();

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID_RICERCA_DUPLICATI_TERMINATA, notifRisultatoBuilder.build());
    }
}
