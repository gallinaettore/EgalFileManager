package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import it.Ettore.androidutilsx.utils.FileUtils;


/**
 * Classe per la gestione della momeria esterna
 */
public class StoragesUtils {
    private final Context context;
    private List<File> allStorages;


    public StoragesUtils(@NonNull Context context){
        this.context = context;
    }


    /**
     * Ottiene lo storage interno non removibile
     * @return Storage interno
     */
    public File getInternalStorage(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final List<File> storages = getLollipopStorageDirs();
            if (storages.size() > 0) {
                for (File storage : storages) {
                    try {
                        if (!Environment.isExternalStorageRemovable(storage)) {
                            return storage;
                        }
                    } catch (IllegalArgumentException e){
                        e.printStackTrace(); //in rari casi (alcuni dispositivi) il metodo isExternalStorageRemovable() causa un'eccezione
                    }
                }
            }
            return Environment.getExternalStorageDirectory();
        } else {
            return Environment.getExternalStorageDirectory();
        }
    }



    /**
     * Ottiene una lista con gli storages esterni
     * @return Lista con gli storages esterni. Lista vuota se non sono stati trovati storages esterni.
     */
    public List<File> getExternalStorages(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final List<File> storages = getLollipopStorageDirs();
            final File internalStorage = getInternalStorage();
            storages.remove(internalStorage);
            return storages;
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            final List<File> storages = getExternalStoragesKitKat();
            final File internalStorage = getInternalStorage();
            storages.remove(internalStorage);
            return storages;
        } else {
            return getExternalStoragesOldApi();
        }
    }


    /**
     * Ottiene una lista con tutti gli storages (interni ed esterni)
     * @return Lista con tutti gli storages (interni ed esterni)
     */
    public List<File> getAllStorages(){
        final List<File> storages = new ArrayList<>();
        final File internalStorage = getInternalStorage();
        storages.add(internalStorage);
        final List<File> externalStorages = getExternalStorages();
        storages.addAll(externalStorages);
        return storages;
    }


    /**
     * Ottiene una lista con tutti gli storages del dispositivo (sia interni che esterni)
     * @return Lista con tutti gli storages. Lista vuota se non viene trovato nessuno storage.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<File> getLollipopStorageDirs(){
        if(this.allStorages != null){
            return this.allStorages;
        } else {
            final File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
            final List<File> storages = new ArrayList<>();
            for (File file : dirs) {
                if (file != null) {
                    int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                    if (index < 0) {
                        Log.e(StoragesUtils.class.getSimpleName(), "Unexpected external file dir: " + file.getAbsolutePath());
                    } else {
                        String path = file.getAbsolutePath().substring(0, index);
                        try {
                            path = new File(path).getCanonicalPath();
                        } catch (IOException e) {
                            // Keep non-canonical path.
                        }

                        storages.add(new File(path));
                    }
                }
            }
            this.allStorages = storages;
            return this.allStorages;
        }
    }



    /**
     * Ottiene la lista con gli storages esterni per Android KitKat
     * @return Lista con gli storages esterni trovati. Lista vuota se non viene trovato nessuno storage.
     */
    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private List<File> getExternalStoragesKitKat() {
        final List<File> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.e(StoragesUtils.class.getSimpleName(), "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(new File(path));
                }
            }
        }
        return paths;
    }



    /**
     * Ottiene la lista con gli storages esterni per versioni inferiori ad Android KitKat
     * @return Lista con gli storages esterni trovati. Lista vuota se non viene trovato nessuno storage.
     */
    private List<File> getExternalStoragesOldApi(){
        final List<String> mMounts = new ArrayList<>();
        final List<String> mVold = new ArrayList<>();

        //analizzo mount
        try {
            final File mountFile = new File("/proc/mounts");
            if(mountFile.exists()){
                final Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    final String line = scanner.nextLine();
                    if (line != null && line.startsWith("/dev/block/vold/")) {
                        final String[] lineElements = line.split(" ");
                        final String element = lineElements[1];
                        // don't add the default mount path
                        if (!element.equals("/mnt/sdcard")) {
                            mMounts.add(element);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //analizzo vold
        try {
            final File voldFile = new File("/system/etc/vold.fstab");
            if(voldFile.exists()){
                final Scanner scanner = new Scanner(voldFile);
                while (scanner.hasNext()) {
                    final String line = scanner.nextLine();
                    if (line != null && line.startsWith("dev_mount")) {
                        final String[] lineElements = line.split(" ");
                        String element = lineElements[2];

                        if (element.contains(":"))
                            element = element.substring(0, element.indexOf(":"));
                        if (!element.equals("/mnt/sdcard"))
                            mVold.add(element);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //confronto mount e vould
        for (int i = 0; i < mMounts.size(); i++) {
            final String mount = mMounts.get(i);
            if (!mVold.contains(mount))
                mMounts.remove(i--);
        }
        mVold.clear();

        final List<File> paths = new ArrayList<>();
        for(String mount : mMounts){
            final File root = new File(mount);
            if (root.exists() && root.isDirectory() && root.canWrite()) {
                paths.add(root);
            }
        }
        mMounts.clear();

        return paths;
    }



    /**
     * Ottiene il path dello storage esterno in cui si trova il file
     * @param file File su cui verificare in quale storage si trova
     * @return Path dello storage in cui si trova il file. Null se il file non si trova in uno storage esterno.
     */
    public String getExtStoragePathForFile(final File file) {
        if(file == null) return null;
        final List<File> externalStorages = getExternalStorages();
        try {
            for (File storage : externalStorages) {
                if (file.getCanonicalPath().startsWith(storage.getAbsolutePath())) {
                    return storage.getAbsolutePath();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }


    /**
     * Ottiene lo storage esterno in cui si trova il file
     * @param file File su cui verificare in quale storage si trova
     * @return Storage in cui si trova il file. Null se il file non si trova in uno storage esterno on in caso di errore
     */
    public File getExtStorageForFile(File file){
        final String extPath = getExtStoragePathForFile(file);
        if(extPath != null){
            return new File(extPath);
        } else {
            return null;
        }
    }



    /**
     * Verifica se il file si trova su uno storage esterno
     * @param file File su cui verificare
     * @return True se il file si trova su uno storage esterno
     */
    public boolean isOnExtSdCard(final File file) {
        return getExtStoragePathForFile(file) != null;
    }



    /**
     * Verifica se il file si trova sullo storage interno
     * @param file File su cui verificare
     * @return True se il file si trova sullo storage interno
     */
    public boolean isOnInternalSdCard(final File file){
        try {
            final File internalStorage = getInternalStorage();
            return file.getCanonicalPath().startsWith(internalStorage.getAbsolutePath());
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * Verifica se il file si trova all'interno di uno storage (interno o esterno) e non in una cartella che necessita di permessi di root
     * @param file File su cui verificare
     * @return True se il file si trova sulla sd card interna o su una sd card esterna. False se si trova su un percorso root
     */
    public boolean isOnSdCard(final File file){
        return isOnInternalSdCard(file) || isOnExtSdCard(file);
    }


    /**
     * Verifica se il file si trova in un percorso root: quindi nè dentro una sd card ne dentro una cartella dell'app
     * @param file File su cui verificare
     * @return True se si trova su un percorso root. False se il file è dentro la sd card, dentro la cartella dell'app o in caso di errore
     */
    public boolean isOnRootPath(final File file){
        return !isOnSdCard(file) && !FileUtils.fileIsInCache(file, context);
    }


    /**
     * Trova l'etichetta dell'SD Card in cui si trova il file
     * @param file File
     * @return Etichetta dell'SD Card in cui si trova il file. Null se il metodo viene chiamato da versioni inferiori a Lollipop, se il file è null o se non è possibile ottenere l'etichetta del volume.
     */
    public String getVolumeLabel(File file){
        if(file == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            return null;
        }
        final StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final StorageVolume storageVolume = storageManager.getStorageVolume(file);
            if(storageVolume != null) {
                return storageVolume.getDescription(context);
            } else {
                return null;
            }
        } else {
            try {
                final Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
                final Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");
                final Method getPath = storageVolumeClass.getMethod("getPath");
                final Method getDescription = storageVolumeClass.getMethod("getDescription", Context.class);
                final Object result = getVolumeList.invoke(storageManager);
                final int length = Array.getLength(result);
                for(int i = 0; i<length; i++){
                    final Object storageVolumeElement = Array.get(result, i);
                    final String storagePath = (String) getPath.invoke(storageVolumeElement);
                    if(file.getAbsolutePath().startsWith(storagePath)){
                        //se il file si trova in quel volume
                        final Object label = getDescription.invoke(storageVolumeElement, context);
                        if(label != null){
                            return (String)label;
                        } else {
                            return null;
                        }
                    }
                }
                return null;
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
    }


    /**
     * Verifica se il file si trova in uno storage USB
     * @param file File da verificare
     * @return True se il file si trova in uno storage USB. False se si trova su un altro tipo di storage o in caso di errore
     */
    public boolean isInUsbStorage(File file) {
        if(file == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            return false;
        }
        final StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final StorageVolume storageVolume = storageManager.getStorageVolume(file);
            if(storageVolume != null) {
                try {
                    final Field subSystem = storageVolume.getClass().getDeclaredField("mSubSystem");
                    subSystem.setAccessible(true);
                    final String subSystemValue = (String) subSystem.get(storageVolume);
                    return subSystemValue != null && subSystemValue.toLowerCase().contains("usb");
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            try {
                final Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
                final Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");
                final Method getPath = storageVolumeClass.getMethod("getPath");
                final Field subSystem = storageVolumeClass.getDeclaredField("mSubSystem");
                subSystem.setAccessible(true);
                final Object result = getVolumeList.invoke(storageManager);
                final int length = Array.getLength(result);
                for(int i = 0; i<length; i++){
                    final Object storageVolumeElement = Array.get(result, i);
                    final String storagePath = (String) getPath.invoke(storageVolumeElement);
                    if(file.getAbsolutePath().startsWith(storagePath)){
                        //se il file si trova in quel volume
                        final String subSystemValue = (String) subSystem.get(storageVolumeElement);
                        return subSystemValue != null && subSystemValue.toLowerCase().contains("usb");
                    }
                }
                return false;
            } catch (Exception e){
                return false;
            }
        }
    }


}
