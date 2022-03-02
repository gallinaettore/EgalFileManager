package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import it.Ettore.androidutilsx.utils.FileUtils;

// esempi:
// https://github.com/TeamAmaze/AmazeFileManager/blob/master/app/src/main/java/com/amaze/filemanager/filesystem/FileUtil.java
// https://github.com/jeisfeld/Augendiagnose/blob/master/AugendiagnoseIdea/augendiagnoseLib/src/main/java/de/jeisfeld/augendiagnoselib/util/imagefile/FileUtil.java


/**
 * Classe di utilità per la gestione di files tramite Storage Access Framework
 */
public class SAFUtils {
    private static final String PREFS_TREE_URIS = "tree_uris";


    /**
     * Salva l'uri dello storage esterno (ricevuto dall'activity) nelle preferences
     *
     * @param context   Context
     * @param extSdPath Path dello storage esterno
     * @param uri       Uri dello storage esterno
     */
    public static void writeUriToPreferences(@NonNull Context context, File extSdPath, Uri uri) {
        if (extSdPath == null || uri == null && extSdPath.getAbsolutePath().equals("/")) return;
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_TREE_URIS, Context.MODE_PRIVATE);
        prefs.edit().putString(extSdPath.getAbsolutePath(), uri.toString()).apply();
    }


    /**
     * Richiama l'uri dello storage esterno salvato nelle preferences, in cui si trova il file
     *
     * @param context Context
     * @param file    File
     * @return Uri dello storage esterno in cui si trova il file. Null se il file è null o se nelle preferences non è stato trovato nessun uri per quello storage
     */
    private static Uri getSavedUriForFile(@NonNull Context context, File file) {
        if (file == null) return null;
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_TREE_URIS, Context.MODE_PRIVATE);
        final Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            final String extSdCard = entry.getKey();
            if (file.getAbsolutePath().startsWith(extSdCard)) {
                return Uri.parse((String) entry.getValue());
            }
        }
        return null;
    }




    /**
     * Verifica se il percorso è scrivibile normalmente (senza utilizzare SAF)
     *
     * @param folder Percorso da verificare. IMPORTANTE: passare sempre una cartella.
     * @return True se è possibile scrivere in quel percorso
     */
    public static boolean isWritable(final File folder) {
        // Verify that this is a directory.
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return false;
        }

        final String fileName = "DummyFile" + System.currentTimeMillis();
        final File file = new File(folder, fileName);

        boolean creato, writable = false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            creato = true;
        } catch (Exception ignored){
            creato = false;
        } finally {
            try{
                fos.close();
            } catch (Exception ignored){}
        }

        if(creato){
            writable = file.canWrite();
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        return writable;
    }



    /**
     * Verifica se il percorso è scrivibile normalmente o utilizzando SAF
     *
     * @param context Context
     * @param folder  Percorso da verificare. IMPORTANTE: passare sempre una cartella.
     * @return True se è scrivibile. False se il percorso è null, se non esiste, se non è una directory o semplicemente se non è scrivibile.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean isWritableNormalOrSaf(@NonNull Context context, @Nullable final File folder) {

        // First check regular writability
        if (isWritable(folder)) {
            return true;
        }

        // Verify that this is a directory.
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return false;
        }

        final String fileName = "DummyFile" + System.currentTimeMillis();
        final File file = new File(folder, fileName);

        // Next check SAF writability.
        DocumentFile document;
        try {
            document = getDocumentFile(context, file, false);
        } catch (Exception e) {
            return false;
        }

        if (document == null) {
            return false;
        }

        // This should have created the file - otherwise something is wrong with access URL.
        boolean result = document.canWrite() && file.exists();

        // Ensure that the dummy file is not remaining.
        document.delete();

        return result;
    }


    /**
     * Ottiene un DocumentFile
     *
     * @param context     Context
     * @param file        File locale
     * @param isDirectory boolean impostato a true utilizzato solo in fase di creazione di una cartella. Per tutti gli altri usi impostare a false
     * @return DocumentFile utilizzato da SAF. Null se non è possibile creare il document file.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DocumentFile getDocumentFile(@NonNull Context context, final File file, final boolean isDirectory) {
        return getDocumentFile(context, file, isDirectory, null);
    }


    /**
     * Ottiene un DocumentFile.
     *
     * @param context     Context
     * @param file        File locale
     * @param isDirectory boolean impostato a true utilizzato solo in fase di creazione di una cartella. Per tutti gli altri usi impostare a false
     * @param treeUri     Tree uri ottenuto alla richiesta ACTION_OPEN_DOCUMENT_TREE. Passare null se il tree uri è stato ottenuto in precedenza ed è salvato nelle prefs
     * @return DocumentFile utilizzato da SAF. Null se non è possibile creare il document file.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static DocumentFile getDocumentFile(@NonNull Context context, final File file, final boolean isDirectory, Uri treeUri) {
        if (treeUri == null) {
            treeUri = getSavedUriForFile(context, file);
        }
        final String baseFolder = new StoragesUtils(context).getExtStoragePathForFile(file);
        boolean originalDirectory = false;
        if (treeUri == null || baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            final String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relativePath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            return null;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) return document;
        final String[] parts = relativePath.split("/");  //originale era "\\/"

        for (int i = 0; i < parts.length; i++) {
            if (document == null) return null;
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    final String mime = FileUtils.getMimeType(file);
                    nextDocument = document.createFile(mime, parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }


    /**
     * Verifica che il tree uri sia l'uri corretto associato a quello storage
     * @param treeUri Uri da verificare
     * @param externalSd Storage associato all'uri
     * @param context Activity chiamante
     * @return True se l'uri corrisponde a quello storage. False se l'uri è errato, se l'uri è null, se lo storage è null
     */
    public static boolean treeUriIsValid(Uri treeUri, File externalSd, @NonNull Context context) {
        if(treeUri == null || externalSd == null){
            return false;
        }
        boolean uriCorretto = false;
        final File tempFile = new File(externalSd, System.currentTimeMillis() + ".tmp");
        final DocumentFile documentFile;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            documentFile = SAFUtils.getDocumentFile(context, tempFile, false, treeUri);
            if(documentFile !=null){
                if (tempFile.exists()) {
                    //l'uri è corretto solo se il file è stato creato nella directory giusta
                    uriCorretto = true;
                }
                //cancello il file al termine (in qualsiasi directory si trovi)
                documentFile.delete();
            }
        }
        return uriCorretto;
    }




    /**
     * Apre un output strem su un file utilizzando il metodo normale o il DocumentFile se su Lollipop o superiore
     * @param context Context
     * @param outputFile File su cui aprire l'output stream
     * @return Output Stream del file
     */
    public static OutputStream getOutputStream(@NonNull Context context, File outputFile){
        if(outputFile == null) return null;
        try {
            if (SAFUtils.isWritable(outputFile.getParentFile())) {
                //se il percorso è scrivibile normalmente
                return new FileOutputStream(outputFile);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    final DocumentFile targetDocument = SAFUtils.getDocumentFile(context, outputFile, false);
                    if (targetDocument != null) {
                        return context.getContentResolver().openOutputStream(targetDocument.getUri());
                    }
                }
            }
        } catch (FileNotFoundException e){
            return null;
        }
        return null;
    }

}
