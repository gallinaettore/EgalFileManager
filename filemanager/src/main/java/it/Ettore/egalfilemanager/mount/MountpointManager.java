package it.Ettore.egalfilemanager.mount;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/



import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.utils.RootUtils;


/**
 * Classe per la gestione dei mountpoints
 */
public class MountpointManager {
    private List<Mountpoint> mountpoints;


    /**
     * Invia da shell il comando "mount" e ottiene una lista di mountpoint
     */
    private void getMountpoints(){
        if(mountpoints == null){
            mountpoints = new ArrayList<>();
            final List<String> outputs = RootUtils.sendCommands("mount");
            for (String outputLine : outputs) {
                //Log.w("Mount line", outputLine);
                try {
                    final String[] lineContent = outputLine.split(" ");
                    final String path, typeLine, fileSystem;
                    int type = 0;
                    if (lineContent[1].equals("on")) {
                        //risultati di mount su nuovi dispositivi
                        path = lineContent[2];
                        fileSystem = lineContent[4];
                        typeLine = lineContent[5].replace("(", "").replace(")", "");
                    } else {
                        //risultati di mount su vecchi dispositivi
                        path = lineContent[1];
                        fileSystem = lineContent[2];
                        typeLine = lineContent[3];
                    }
                    if (typeLine.startsWith("rw")) {
                        type = Mountpoint.RW;
                    } else if (typeLine.startsWith("ro")) {
                        type = Mountpoint.RO;
                    }
                    if (type == Mountpoint.RW || type == Mountpoint.RO) {
                        final Mountpoint mountpoint = new Mountpoint(path, type);
                        mountpoint.setFilesystem(fileSystem);
                        mountpoint.setAltriParametri(typeLine.length() > 3 ? typeLine.substring(3) : "");
                        mountpoints.add(mountpoint);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Restituisce il mountpoint relativo al file
     * @param file File da analizzare
     * @return Mountpoint. Null se il file è null o se non è possibile trovare un mountpoint.
     */
    public Mountpoint mountpointForFile(File file){
        if(file == null){
            return null;
        }
        getMountpoints();
        Mountpoint currentMountpoint = null;
        for(Mountpoint mountpoint : this.mountpoints){
            if(file.getAbsolutePath().startsWith(mountpoint.getPath())){
                if(currentMountpoint == null){
                    currentMountpoint = mountpoint;
                } else {
                    // current found point is bigger than last one, hence not a conflicting one
                    // we're finding the best match, this omits for eg. / and /sys when we're actually
                    // looking for /system
                    if(mountpoint.getPath().length() > currentMountpoint.getPath().length()){
                        currentMountpoint = mountpoint;
                    }
                }
            }
        }
        return currentMountpoint;
    }


    /**
     * Monta il mountpoint con i permessi di scrittura
     * @param mountpoint Mountpoint da montare con i nuovi permessi
     * @return True se sono stati cambiati i permessi in RW. False se il mountpoint era già in RW, se il mountpoint è null, se avviene un errore e non è possibile cambiare i permessi
     */
    public boolean mountAsRW(Mountpoint mountpoint){
        if(mountpoint == null || mountpoint.getStato() == Mountpoint.RW){
            return false;
        }
        final String mountCommand = "mount -o rw,remount " + mountpoint.getPath();
        final List<String> mountOutput = RootUtils.sendCommands(mountCommand);
        boolean success = mountOutput.size() == 0;
        if(success){
            mountpoint.setStato(Mountpoint.RW);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Monta il mountpoint con i permessi di sola lettura
     * @param mountpoint Mountpoint da montare con i nuovi permessi
     * @return True se sono stati cambiati i permessi in RO. False se il mountpoint era già in RO, se il mountpoint è null, se avviene un errore e non è possibile cambiare i permessi
     */
    public boolean mountAsRO(Mountpoint mountpoint){
        if(mountpoint == null || mountpoint.getStato() == Mountpoint.RO){
            return false;
        }
        final String mountCommand = "mount -o ro,remount " + mountpoint.getPath();
        final List<String> mountOutput = RootUtils.sendCommands(mountCommand);
        boolean success = mountOutput.size() == 0;
        if(success){
            mountpoint.setStato(Mountpoint.RO);
            return true;
        } else {
            Log.e(getClass().getSimpleName(), "Non è stato possibile montare correttamente in RO " + mountpoint.getPath() + "\n" + mountOutput.get(0));
            return false;
        }
    }


    /**
     * Monta il mountpoint relativo al file con i permessi di scrittura
     * @param file File da analizzare
     * @return True se sono stati cambiati i permessi in RW. False se il mountpoint era già in RW, se il file è null, se avviene un errore e non è possibile cambiare i permessi
     */
    public boolean mountAsRW(File file) {
        if (file == null) return false;
        final Mountpoint mountpoint = mountpointForFile(file);
        return mountpoint != null && mountAsRW(mountpoint);
    }


    /**
     * Monta il mountpoint relativo al file con i permessi di sola lettura
     * @param file File da analizzare
     * @return True se sono stati cambiati i permessi in RO. False se il mountpoint era già in RO, se il file è null, se avviene un errore e non è possibile cambiare i permessi
     */
    public boolean mountAsRO(File file) {
        if (file == null) return false;
        final Mountpoint mountpoint = mountpointForFile(file);
        return mountpoint != null && mountAsRO(mountpoint);
    }


    /**
     * Restituisce la lista dei mountpoint. La lista viene letta ogni volta che si chiama il metodo
     * @return Lista dei mountpoint
     */
    public List<Mountpoint> getMountpointList(){
        this.mountpoints = null; //imposto a null in modo da parmettere sempre la lettura ogni volta che si chiama questo metodo
        getMountpoints();
        return this.mountpoints;
    }
}
