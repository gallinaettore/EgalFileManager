package it.Ettore.egalfilemanager;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;

import it.Ettore.androidutilsx.utils.BackupPreferences;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.fileutils.AssociazioneFiles;
import it.Ettore.egalfilemanager.ftp.ServerFtp;
import it.Ettore.egalfilemanager.lan.AutenticazioneLan;


/**
 * Classe di utilità per eseguire i compiti di backup delle impostazioni
 */
public class BackupPreferencesUtils {


    /**
     * Crea un file di backup delle impostazioni
     * @param context Context
     */
    public static void creaBackupPreferences(@NonNull Context context){
        final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(context, DialogFileChooserBuilder.TYPE_SAVE_FILE);
        fileChooser.setTitle(R.string.seleziona_destinazione);
        fileChooser.setCancelable(false);
        fileChooser.setStartFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        fileChooser.setFileName(BackupPreferences.Companion.createSettingsFileName("Egal File Manager"));
        fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
            @Override
            public void onFileChooserSelected(final File selected) {
                final BackupPreferences backupPreferences = new BackupPreferences(context);
                backupPreferences.addSharedPreferencesFiles(backupPreferences.getDefaultPrefs(),
                        AssociazioneFiles.PREFS_ASSOCIAZIONI, ServerFtp.PREFS_AUTH_FTP,
                        AutenticazioneLan.PREFS_AUTH_SMB);
                //aggiungo le chiavi delle prefs da escludere
                backupPreferences.addKeyToExclude(backupPreferences.getDefaultPrefs());
                backupPreferences.performBackup(selected);
            }

            @Override
            public void onFileChooserCanceled() {}
        });
        fileChooser.create().show();
    }


    /**
     * Ripristina le impostazioni da un file di backup
     * @param context Context
     */
    public static void ripristinaBackupPreferences(@NonNull Context context){
        final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(context, DialogFileChooserBuilder.TYPE_SELECT_FILE_FOLDER);
        fileChooser.setTitle(R.string.seleziona_file);
        fileChooser.setCancelable(false);
        fileChooser.setStartFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
            @Override
            public void onFileChooserSelected(final File selected) {
                final BackupPreferences backupPreferences = new BackupPreferences(context);
                backupPreferences.restoreBackup(selected);
            }

            @Override
            public void onFileChooserCanceled() {}
        });
        fileChooser.create().show();
    }
}
