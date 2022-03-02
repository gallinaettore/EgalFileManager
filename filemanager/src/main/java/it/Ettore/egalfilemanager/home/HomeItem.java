package it.Ettore.egalfilemanager.home;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import java.io.File;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;


/**
 *  Item utilizzato per la creazione delle view delle momorie nel fragment main e nella navigation bar
 */
public class HomeItem {
    public final String titolo;
    public final File startDirectory;
    public final int menuItemId;
    public @DrawableRes
    final int resIdIcona;
    public @DrawableRes
    final int resIdIconaNav;


    /**
     *
     * @param resIdIcona Risorsa dell'icona da mostrare nel fragment
     * @param titolo Titolo dell'item
     * @param startDirectory Directory da cui deve cominciare la navigazione
     * @param menuItemId Id del menù della navigation bar
     * @param resIdIconaNav Risorsa dell'icona da mostrare nella navigation bar
     */
    public HomeItem(@DrawableRes int resIdIcona, @NonNull String titolo, @NonNull File startDirectory, int menuItemId, @DrawableRes int resIdIconaNav){
        this.resIdIcona = resIdIcona;
        this.titolo = titolo;
        this.startDirectory = startDirectory;
        this.menuItemId = menuItemId;
        this.resIdIconaNav = resIdIconaNav;
    }
}
