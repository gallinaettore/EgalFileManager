package it.Ettore.egalfilemanager.mount;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;


/**
 * Classe di utilità per la gestione dei files all'interno delle cartelle root.
 * Il mount point viene portato in RW, tutti i files vengono processati e al termine viene riportato in RO.
 */
public class MountUtils {
    private final Context context;
    private final MountpointManager mountpointManager;
    private final Set<Mountpoint> mountpointsDaRipristinareInRo;


    /**
     *
     * @param context Context
     */
    public MountUtils(@NonNull Context context){
        this.context = context;
        mountpointManager = new MountpointManager();
        mountpointsDaRipristinareInRo = new HashSet<>();
    }


    /**
     * Imposta i mountpoint come riscrivibili se necessario.
     * Se il file non si trova all'interno di una sd card, monta il mountpoint in RW e lo aggiunge alla lista di mountpoints di rimettere su RO
     * @param file File da verificare il mountpoint
     */
    public void montaInRwSeNecessario(File file){
        final StoragesUtils storagesUtils = new StoragesUtils(context);
        if(file != null && storagesUtils.isOnRootPath(file)){
            final Mountpoint mountpoint = mountpointManager.mountpointForFile(file);
            if(mountpoint != null && mountpoint.getStato() == Mountpoint.RO){
                boolean success = mountpointManager.mountAsRW(mountpoint);
                if(success) {
                    mountpointsDaRipristinareInRo.add(mountpoint);
                }
            }
        }
    }



    /**
     * Tutti i mountpoint che nello stato originale erano RO e poi sono stati impostati temporaneamente in RW, adesso saranno ripristinati nuovamente in RO
     * @return True se tutti i mountpoint sono stati correttamente ripristinati
     */
    public boolean ripristinaRo(){
        int tuttiRipristinati = 0;
        for(Mountpoint mountpoint : mountpointsDaRipristinareInRo){
            boolean ripristinato = mountpointManager.mountAsRO(mountpoint);
            if(ripristinato){
                tuttiRipristinati++;
            }
        }
        return tuttiRipristinati == mountpointsDaRipristinareInRo.size();
    }


    /**
     * Tutti i mountpoint che nello stato originale erano RO e poi sono stati impostati temporaneamente in RW, adesso saranno ripristinati nuovamente in RO
     * Effettua diversi tentati
     * @param timeout Numero massimo di secondi oltre i quali non effettua più tentativi. Viene fatto un tentativo al secondo
     * @return True se è stato ripristinato in RO. False in caso contrario
     */
    public boolean ripristinaRoRicorsivamenteSeOccupato(final int timeout){
        boolean isRo = ripristinaRo();
        int mTimeout = timeout;
        while (!isRo && mTimeout > 0){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            isRo = ripristinaRo();
            mTimeout--;
        }
        return isRo;
    }
}
