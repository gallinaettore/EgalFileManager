package it.Ettore.egalfilemanager.lan.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbFile;


/**
 * Thread per l'analisi di un singolo indirizzo IP
 */
public class ScanIpThread implements Runnable {
    private final WeakReference<Activity> activity;
    private final String ipAddress;
    private final ScanIpListener listener;
    private boolean serverIsValid;
    private String hostName;


    /**
     *
     * @param activity Activity chiamante
     * @param ipAddress IP da analizzare
     * @param listener Listener eseguito al termine dell'analisi dell'indirizzo
     */
    public ScanIpThread(@NonNull Activity activity, @NonNull String ipAddress, @NonNull ScanIpListener listener){
        this.activity = new WeakReference<>(activity);
        this.hostName = ipAddress;
        this.ipAddress = ipAddress;
        this.listener = listener;
    }


    /**
     * Avvia il thread
     */
    protected void start(){
        new Thread(this).start();
    }


    /**
     * Esegue l'analisi in background
     */
    @Override
    public void run() {
        try {
            final SmbFile server = new SmbFile("smb://" + ipAddress + "/");
            server.connect();
            //se non genera eccezioni il server è valido
            serverIsValid = true;
        } catch (SmbAuthException autEx){
            //se genera l'eccezione autenticazione il server è valido (ma non è stato possibile connettersi perchè non sono state fornite le credenziali)
            serverIsValid = true;
        } catch (Exception ignored){
            //per tutte le altre eccezioni il server non è valido
        }

        if(serverIsValid){
            try {
                //Uso la vecchia versione di NbtAddress (jcifs 1.2.25) perchè le nuove versioni non supportano la risoluzione del file host
                it.Ettore.egalfilemanager.lan.netbios.NbtAddress nbtAddress = it.Ettore.egalfilemanager.lan.netbios.NbtAddress.getByName(ipAddress);
                hostName = nbtAddress.getHostName();
            } catch (Exception ignored){}
        }

        if(activity.get() != null && !activity.get().isFinishing()){
            activity.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onIpScanned(ipAddress, hostName, serverIsValid);
                }
            });
        }
    }



    /**
     * Listener di scansione indirizzo IP
     */
    protected interface ScanIpListener {

        /**
         * Eseguito al termine dell'analisi dell'indirizzo
         * @param ipAddress Indirizzo analizzato
         * @param hostName Nome NETBios del server
         * @param isValid True se il server è valido (contiene cartelle condivise)
         */
        void onIpScanned(String ipAddress, String hostName, boolean isValid);
    }

}
