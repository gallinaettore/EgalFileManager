package it.Ettore.egalfilemanager.lan.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;


/**
 * Classe per la ricerca di server all'interno della lan
 */
public class ScanServer implements ScanIpThread.ScanIpListener {
    private final Activity activity;
    private final ScanServerListener listener;
    private final AtomicInteger threadCount;
    private ExecutorService executorService;
    private boolean isRunning;


    /**
     *
     * @param activity Activity chiamante
     * @param listener Listener della ricerca server
     */
    public ScanServer(@NonNull Activity activity, @NonNull ScanServerListener listener) {
        this.activity = activity;
        this.listener = listener;
        threadCount = new AtomicInteger();
    }


    /**
     * Avvia la ricerca dei server. Vengono scansionati tutti gli IP alla ricerca di server di condivisione files.
     */
    public void start(){
        if(isRunning) return;
        isRunning = true;
        try {
            final WifiManager wm = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wm.getConnectionInfo();
            final int localAddress = connectionInfo.getIpAddress();
            final String localAddressString = Formatter.formatIpAddress(localAddress);
            final String prefix = localAddressString.substring(0, localAddressString.lastIndexOf(".") + 1);

            threadCount.set(0);
            this.executorService = Executors.newCachedThreadPool();
            for (int i = 0; i < 255; i++) {
                //scansiono in un thread separato ogni ip della lan
                final String testIp = prefix + i;
                executorService.execute(new ScanIpThread(activity, testIp, this));
            }
        } catch (Exception e){
            //errore magari il dispositivo non è provvisto di wifi
            e.printStackTrace();
        } finally {
            if(executorService != null){
                executorService.shutdown();
            }
        }
    }


    /**
     * Interrompe i thread ancora in sospeso (non avviati), ma se tutti i 255 thread sono avviati insieme (senza FixedThreadPool) non possono essere interrotti
     */
    public void interruptAll(){
        if(isRunning) {
            if(this.executorService != null) {
                this.executorService.shutdownNow();
            }
            isRunning = false;
            listener.onScanFinished();
        }
    }


    /**
     * Listener chiamato quando il singolo ip viene scansionato
     * @param ipAddress Ip scansionato
     * @param hostName Nome NETBios del server
     * @param isValid True se è un server che contiene cartelle condivise
     */
    @Override
    public void onIpScanned(final String ipAddress, final String hostName, final boolean isValid) {
        threadCount.incrementAndGet();
        if(isValid){
            listener.onServerFound(ipAddress, hostName);
        }
        if(threadCount.get() >= 255){
            isRunning = false;
            listener.onScanFinished();
        }
    }


    /**
     * Listener per la scansione dei server
     */
    public interface ScanServerListener {

        /**
         * Chiamato quando viene trovato un server valido (che contiene cartelle condivise)
         * @param ipAddress Indirizzo IP del server
         * @param hostName Nome NETBios del server
         */
        void onServerFound(String ipAddress, String hostName);


        /**
         * Chiamato quando tutti gli IP sono stati scansionati
         */
        void onScanFinished();
    }
}
