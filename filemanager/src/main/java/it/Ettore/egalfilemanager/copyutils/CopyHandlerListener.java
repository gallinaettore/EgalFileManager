package it.Ettore.egalfilemanager.copyutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.util.List;


/**
 * Listener dell'handler di copia
 */
public interface CopyHandlerListener {

    /**
     * Listener chiamato quando il service termina la copia
     * @param success True se la copia è andata a buon fine. False se è avvenuto un errore o la copia è stata annullata.
     * @param destinationPath Path della cartella di destinazione
     * @param filesCopiati Lista di path dei files copiati correttamente
     * @param tipoCopia Una della variabili COPY della classe CopyService (specifica se la copia è avvenuta ad esempio da smb a locale)
     */
    void onCopyServiceFinished(boolean success, String destinationPath, List<String> filesCopiati, int tipoCopia);
}
