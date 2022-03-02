package it.Ettore.egalfilemanager.tools;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import it.Ettore.egalfilemanager.R;


/**
 * Enum per i dati dei tools
 */
public enum Tool {
    SPAZIO_OCCUPATO(R.drawable.tool_spazio_occupato, R.string.tool_analisi_spazio_occupato, false),
    RICERCA_FILES(R.drawable.tool_ricerca_file, R.string.tool_cerca_file, false),
    BACKUP_APP(R.drawable.tool_backup_app, R.string.tool_backup_app, true),
    FILES_RECENTI(R.drawable.tool_recenti, R.string.tool_recenti, false),
    FILES_DUPLICATI(R.drawable.tool_files_duplicati, R.string.trova_files_duplicati, false),
    MOUNTPOINTS(R.drawable.tool_mountpoints, R.string.tool_mountpoints, true);


    public @DrawableRes final int resIdIcon;
    public @StringRes final int resIdNome;
    public final boolean isPro;

    Tool(@DrawableRes int resIdIcon, @StringRes int resIdNome, boolean isPro){
        this.resIdIcon = resIdIcon;
        this.resIdNome = resIdNome;
        this.isPro = isPro;
    }
}
