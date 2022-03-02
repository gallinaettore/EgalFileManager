package it.Ettore.egalfilemanager.mediastore;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.R;

import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_SCAN_LIBRARY_IN_CORSO;
import static it.Ettore.egalfilemanager.Costanti.NOTIF_ID_SCAN_LIBRARY_TERMINATA;
import static it.Ettore.egalfilemanager.NotificationChannelManager.NOTIF_CHANNEL_ID_OPERAZIONI;


/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Service per avviare la scansione per la libreria multimediale, ricercando tutti i files presenti nel dispositivo e nemme memoria esterne
 */
public class ScanMediaLibraryService extends IntentService {
    private static final String KEYBUNDLE_LISTA_STORAGES = "lista_storages";
    private static boolean isRunning;
    private int filesScansionati;
    private NotificationCompat.Builder notif;


    /**
     * Costruttore di default (obbligatori)
     */
    public ScanMediaLibraryService(){
        super("ScanMediaLibraryService");
    }


    /**
     * Utilità per creare l'intent per avviare in service
     * @param context Context dell'activity chiamante
     * @param storages Lista che contiene i path storages da scansionare
     * @return Intent per avviare in service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull ArrayList<String> storages){
        final Intent intent = new Intent(context, ScanMediaLibraryService.class);
        intent.putExtra(KEYBUNDLE_LISTA_STORAGES, storages);
        return intent;
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

        //creo la notifica
        notif = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_OPERAZIONI)
                .setSmallIcon(R.drawable.ic_status_bar)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.aggiornamento_media_library))
                .setStyle(new NotificationCompat.BigTextStyle())
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOnlyAlertOnce(true) //suona una volta sola e non ad ogni aggiornamento
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        //mostro la notifica con startforeground per evitare che il service venga eliminato da Android
        startForeground(NOTIF_ID_SCAN_LIBRARY_IN_CORSO, notif.build());
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

        if(!isRunning) {
            startForeground(NOTIF_ID_SCAN_LIBRARY_IN_CORSO, notif.build());
        }

        //operazioni all'avvio standard del service
        isRunning = true;

        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Scansione dei files in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        filesScansionati = 0;
        final List<String> pathsStorages = intent.getStringArrayListExtra(KEYBUNDLE_LISTA_STORAGES);
        final List<File> storages = new ArrayList<>(pathsStorages.size());
        for(String path : pathsStorages){
            storages.add(new File(path));
        }
        final Set<File> allFiles = getAllFiles(storages);
        final String[] allPaths = new String[allFiles.size()];
        final File[] arrayFiles = allFiles.toArray(new File[allFiles.size()]);
        for(int i=0; i < arrayFiles.length; i++){
           allPaths[i] = arrayFiles[i].getAbsolutePath();
        }
        notif.setProgress(allPaths.length, 0, false);
        startForeground(NOTIF_ID_SCAN_LIBRARY_IN_CORSO, notif.build());
        MediaScannerConnection.scanFile(this, allPaths, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String s, Uri uri) {
                filesScansionati++;
                double percent = filesScansionati * 100 / (double)allPaths.length;
                notif.setContentText(String.format(Locale.ENGLISH,"%.1f", percent) + "%");
                notif.setProgress(allPaths.length, filesScansionati, false);
                startForeground(NOTIF_ID_SCAN_LIBRARY_IN_CORSO, notif.build());
            }
        });

        //attendo finchè non termina la scansione
        while (filesScansionati < allPaths.length){
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }

        isRunning = false;
        stopForeground(true); //rimuove la notifica se l'app viene chiusa e rimane solo il service

        //mostra la notifica scansione terminata
        final NotificationCompat.Builder notifScansioneTerminata = new NotificationCompat.Builder(getApplicationContext(), NOTIF_CHANNEL_ID_OPERAZIONI)
                .setSmallIcon(R.drawable.ic_status_bar)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.scansione_media_library_terminata))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID_SCAN_LIBRARY_TERMINATA, notifScansioneTerminata.build());
    }


    /**
     * Crea un Set con tutti i files presenti, anche quelli all'interno di sottodirectory
     * @param files Lista di files o cartelle da aggiungere al media Store
     * @return Set con tutti i files presenti
     */
    private Set<File> getAllFiles(List<File> files){
        final Set<File> allFiles = new LinkedHashSet<>();
        for(File file : files){
            if(!file.isDirectory()){
                allFiles.add(file);
            } else {
                final File[] contenutoCartella = file.listFiles();
                if(contenutoCartella != null){
                    allFiles.addAll(getAllFiles(Arrays.asList(contenutoCartella)));
                }
            }
        }
        return allFiles;
    }
}
