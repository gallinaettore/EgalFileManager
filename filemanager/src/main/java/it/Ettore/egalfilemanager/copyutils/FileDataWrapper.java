package it.Ettore.egalfilemanager.copyutils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Classe wrapper per passare dati di grandi dimensioni tramite intent
 */
public class FileDataWrapper implements Serializable {
    public ArrayList<String> listaPaths;
    public String pathDestinazione;
    public HashMap<String, Integer> azioniPathsGiaPresenti;
}
