package it.Ettore.egalfilemanager.filemanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.RootUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyService;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.thread.AnalisiPreCopiaTask;
import it.Ettore.egalfilemanager.filemanager.thread.BaseExtractService;
import it.Ettore.egalfilemanager.filemanager.thread.CompressHandler;
import it.Ettore.egalfilemanager.filemanager.thread.CompressService;
import it.Ettore.egalfilemanager.filemanager.thread.CopiaSingoloFileHandler;
import it.Ettore.egalfilemanager.filemanager.thread.CopiaSingoloFileService;
import it.Ettore.egalfilemanager.filemanager.thread.CreaCartellaTask;
import it.Ettore.egalfilemanager.filemanager.thread.CreaFileTask;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaService;
import it.Ettore.egalfilemanager.filemanager.thread.ExtractHandler;
import it.Ettore.egalfilemanager.filemanager.thread.ExtractRarService;
import it.Ettore.egalfilemanager.filemanager.thread.ExtractZipService;
import it.Ettore.egalfilemanager.filemanager.thread.LsTask;
import it.Ettore.egalfilemanager.filemanager.thread.RinominaHandler;
import it.Ettore.egalfilemanager.filemanager.thread.RinominaService;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.RootFile;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaScannerUtil;
import it.Ettore.egalfilemanager.mount.MountpointManager;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ROOT_EXPLORER;



/**
 *  Classe per la gestione dei files locali
 *  @author Ettore Gallina
 */
public class FileManager {
    private final Context context;
    private boolean permessiRoot;
    private MediaScannerUtil.MediaScannerListener mediaScannerListener;
    private final MountpointManager mountpointManager;
    private final SharedPreferences prefs;
    private final StoragesUtils storagesUtils;


    /**
     *
     * @param context Context
     */
    public FileManager(@NonNull Context context){
        this(context, null);
    }


    /**
     *
     * @param context Context
     */
    public FileManager(@NonNull Context context, SharedPreferences prefs){
        this.context = context;
        this.mountpointManager = new MountpointManager();
        this.storagesUtils = new StoragesUtils(context);
        if(prefs != null){
            this.prefs = prefs;
        } else {
            this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }



    /**
     *
     * @return Context associato al file manager
     */
    public Context getContext(){
        return this.context;
    }



    /**
     *
     * @return True se al file manager è stato concesso di utilizzare i permessi di root
     */
    public boolean haPermessiRoot(){
        return this.permessiRoot;
    }



    /**
     *
     * @param permessiRoot Concede o meno l'autorizzazione a utilizzare i permessi di root
     */
    public void setPermessiRoot(boolean permessiRoot) {
        this.permessiRoot = permessiRoot;
    }



    /**
     *
     * @param mediaScannerListener Imposta un'oggetto media scanner listener. Sarà notificato ogni volta che l'aggiornamento al media store è stato effettuato
     */
    public void setMediaScannerListener(MediaScannerUtil.MediaScannerListener mediaScannerListener){
        this.mediaScannerListener = mediaScannerListener;
    }


    /**
     * Restituisce l'oggetto storage utils creato
     * @return Storage Utils
     */
    public StoragesUtils getStoragesUtils(){
        return this.storagesUtils;
    }



    /**
     * Effettua la scansione del contenuto della cartella in un task separato
     * @param directory Cartella da scansionare
     * @param listener Listener chiamato al termine della scansione
     */
    public void ls(File directory, LsTask.LsListener listener){
        new LsTask(this, directory, listener).execute();
    }



    /**
     * Crea una nuova cartella in un task separato
     * @param percorso Cartella in cui creare la nuova cartella
     * @param nomeCartella Nome della nuova cartella
     * @param listener Listener chiamato al termine della creazione
     */
    public void creaCartella(File percorso, @NonNull String nomeCartella, CreaCartellaTask.CreaCartellaListener listener){
        new CreaCartellaTask(this, percorso, nomeCartella, listener).execute();
    }



    /**
     * Copia i files in un task separato
     * @param listaFiles Lista files da copiare
     * @param destinazione Cartella in cui devono essere copiati i files
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void copia(List<File> listaFiles, File destinazione, @NonNull CopyHandler copyHandler){
        if(context instanceof Activity) {
            copyHandler.setMediaScannerListener(mediaScannerListener);
            final AnalisiPreCopiaTask task = new AnalisiPreCopiaTask((Activity) context, new ArrayList<>(listaFiles), destinazione, copyHandler);
            task.execute();
        }
    }



    /**
     * Sposta i files in un task separato
     * @param listaFiles Lista files da spostare
     * @param destinazione Cartella in cui devono essere spostati i files
     * @param copyHandler Handler per far comunicare il service con la UI
     */
    public void sposta(List<File> listaFiles, File destinazione, CopyHandler copyHandler){
        if(context instanceof Activity) {
            copyHandler.setMediaScannerListener(mediaScannerListener);
            final AnalisiPreCopiaTask task = new AnalisiPreCopiaTask((Activity) context, new ArrayList<>(listaFiles), destinazione, copyHandler);
            task.setCancellaOrigine(true);
            task.execute();
        }
    }



    /**
     * Copia un singolo file in un task separato (Permettere di scegliere il nome del file di destinazione)
     * @param fileOrigine File da copiare
     * @param fileDestinazione File di destinazione
     * @param handler Handler che gestisce la dialog
     */
    public void copiaSingoloFile(File fileOrigine, File fileDestinazione, @NonNull CopiaSingoloFileHandler handler){
        if(!(context instanceof Activity) || fileOrigine == null || fileDestinazione == null) {
            return;
        }
        final Intent serviceIntent = CopiaSingoloFileService.createStartIntent(context, fileOrigine, fileDestinazione, handler);
        if(fileDestinazione.exists()){
            //chiedo come comportarmi se il file esiste
            final CustomDialogBuilder builder = new CustomDialogBuilder(context);
            builder.setType(CustomDialogBuilder.TYPE_WARNING);
            builder.setCancelable(false);
            final View view = LayoutInflater.from(context).inflate(R.layout.dialog_sovrascrivi_file, null);
            final TextView messageTextView = view.findViewById(R.id.textview_messaggio);
            final CheckBox checkBoxApplicaATutti = view.findViewById(R.id.checkbox_tutti);
            checkBoxApplicaATutti.setVisibility(View.GONE);
            builder.setView(view);
            messageTextView.setText(String.format(context.getString(R.string.file_esistente), fileDestinazione.getName()));
            builder.setPositiveButton(R.string.sovrascrivi, (dialogInterface, i) -> startService(serviceIntent));
            builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                if(handler.getListener() != null){
                    handler.getListener().onCopyServiceFinished(false, fileDestinazione.getAbsolutePath(), new ArrayList<>(), CopyService.COPY_LOCAL_TO_LOCAL);
                }
            });
            builder.create().show();
        } else {
            //continuo con la copia se il file di destinazione non esiste
            startService(serviceIntent);
        }
    }




    /**
     * Rinomina in sequenza un insieme di files in un task separato. I files rinominati avranno un numero progressivo.
     * @param listaFiles Lista files da rinominare
     * @param nuovoNome Nuovo nome da assegnare al file
     * @param handler Handler che gestisce la dialog
     */
    public void rinomina(@NonNull List<File> listaFiles, @NonNull String nuovoNome, @NonNull RinominaHandler handler){
        if(!(context instanceof Activity) || listaFiles == null || nuovoNome == null){
            return;
        }
        handler.setMediaScannerListener(mediaScannerListener);
        final Intent serviceIntent = RinominaService.createStartIntent(context, new ArrayList<>(listaFiles), nuovoNome, false, handler);
        startService(serviceIntent);
    }



    /**
     * Gestisce la visibilità di un file in un task separato
     * @param file File su cui modificare la visibilità
     * @param nascondi Nascondi o mostra file
     * @param handler Handler che gestisce la dialog
     */
    public void nascondiFile(@NonNull File file, boolean nascondi, @NonNull RinominaHandler handler){
        if(file == null || !file.exists()) return;
        String nuovoNome;
        if(nascondi && !file.isHidden()) {
            nuovoNome = "." + file.getName();
        } else if (!nascondi && file.isHidden()){
            nuovoNome = file.getName().substring(1);
        } else {
            return;
        }
        final List<File> files = new ArrayList<>(1);
        files.add(file);
        final Intent serviceIntent = RinominaService.createStartIntent(context, files, nuovoNome, true, handler);
        startService(serviceIntent);
    }



    /**
     * Cancella i files in un task separato
     * @param listaFiles Lista files da cancellare
     * @param handler Handler che gestisce la dialog
     */
    public void elimina(@NonNull List<File> listaFiles, @NonNull final EliminaHandler handler){
        if(!(context instanceof Activity) || listaFiles == null){
            return;
        }

        final List<File> daCancellare = new ArrayList<>(listaFiles);
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.setType(CustomDialogBuilder.TYPE_WARNING);
        String message;
        if(daCancellare.size() == 1){
            message = String.format(context.getString(R.string.conferma_eliminazione1), daCancellare.get(0).getName());
        } else if (daCancellare.size() > 1){
            message = String.format(context.getString(R.string.conferma_eliminazione2), String.valueOf(daCancellare.size()));
        } else {
            return;
        }
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            handler.setMediaScannerListener(mediaScannerListener);
            final Intent serviceIntent = EliminaService.createStartIntent(context, daCancellare, handler);
            startService(serviceIntent);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }



    /**
     * Analizza i files e mostra una dialog sulle proprietà (utilizzare questo metodo sui files locali)
     * @param listaFiles Lista files da analizzare
     * @param listener Listener eseguito quando una proprietà è stata modificata (es. mostra/nascondi)
     */
    public void mostraProprieta(@NonNull List<File> listaFiles, ProprietaTask.ProprietaNascondiListener listener){
        new ProprietaTask((Activity)context, new ArrayList<>(listaFiles), listener).execute();
    }



    /**
     * Analizza i files e mostra una dialog sulle proprietà (utilizzare questo metodo su i files mostrati dentro gli albums)
     * Non sarà possibile mostrare e modificare la visibilità del file
     * @param listaFiles Lista files da analizzare
     */
    public void mostraProprietaCategoria(@NonNull List<File> listaFiles){
        final ProprietaTask proprietaTask = new ProprietaTask((Activity)context, new ArrayList<>(listaFiles), null);
        proprietaTask.setModificaVisibilita(false);
        proprietaTask.execute();
    }



    /**
     * Estrae un archivio compresso in un task separato
     * @param file Archivio compresso
     * @param destinationFolder Cartella in cui verrà estratto l'archivio
     * @param handler Handler che gestisce la dialog
     */
    public void estraiArchivio(File file, File destinationFolder, @NonNull ExtractHandler handler){
        if(file != null && file.exists() && destinationFolder != null && context instanceof Activity && !BaseExtractService.isRunning()){
            final String mime = FileUtils.getMimeType(file);
            Intent serviceIntent = null;
            if(mime.equals("application/zip") || mime.equals("application/x-zip-compressed") || mime.equals("application/java-archive")) {
                serviceIntent = ExtractZipService.createStartIntent(context, file, destinationFolder, handler);
            } else if (mime.equals("application/rar") || mime.equals("application/x-rar-compressed")){
                serviceIntent = ExtractRarService.createStartIntent(context, file, destinationFolder, handler);
            }
            if(serviceIntent != null) {
                startService(serviceIntent);
            }
        }
    }




    /**
     * Comprime i files in un archivio zip in un task separato
     * @param listaFiles Lista files da comprimere
     * @param destinationZipFile File zip in cui saranno inseriti i files
     * @param compressHandler Handler che gestisce la dialog di compressione
     */
    public void comprimiFiles(@NonNull List<File> listaFiles, File destinationZipFile, @NonNull CompressHandler compressHandler){
        if(!(context instanceof Activity) || destinationZipFile == null){
            return;
        }

        if(!CompressService.isRunning()) {
            final Intent serviceIntent = CompressService.createStartIntent(context, listaFiles, destinationZipFile, compressHandler);
            startService(serviceIntent);
        }
    }



    /**
     * Effettua la scansione del contenuto della cartella. Da usare in un task separato
     * @param f Cartella da scansionare
     * @return Lista di files trovati. Lista vuota se non viene trovato nessun file
     */
    public List<File> ls(File f){
        File file;
        try{
            file = f.getCanonicalFile();
        } catch (Exception e){
            file = f;
        }
        if(file == null){
            return new ArrayList<>();
        }
        if(!storagesUtils.isOnRootPath(file)){
            //file leggibile normalmente
            final File[] files = file.getAbsoluteFile().listFiles();
            if (files != null) {
                return new ArrayList<>(Arrays.asList(files));
            } else {
                return new ArrayList<>();
            }
        } else {
            //non leggibile (provo con i permessi di root se ci sono)
            if(permessiRoot){
                return lsComeRoot(file);
            } else {
                return new ArrayList<>();
            }
        }
    }


    /**
     * Crea una nuovo file in un task separato
     * @param percorso Cartella in cui creare il nuovo file
     * @param nomeFile Nome del nuovo file
     * @param listener Listener chiamato al termine della creazione
     */
    public void creaFile(File percorso, @NonNull String nomeFile, CreaFileTask.CreaFileListener listener){
        new CreaFileTask(this, percorso, nomeFile, listener).execute();
    }



    /**
     * Effettua la scansione del contenuto della cartella usando i permessi di root. Da usare in un task separato
     * @param file Cartella da scansionare
     * @return Lista di files trovati. Lista vuota se non viene trovato nessun file
     */
    public List<File> lsComeRoot(File file){
        if(file == null) return new ArrayList<>();
        final String lsCommand = "ls -la \"" + file.getAbsolutePath() + "\"";
        final List<String> results = RootUtils.sendCommands(false, lsCommand);
        final ArrayList<File> files = new ArrayList<>(results.size());
        for(String line : results){
            final RootFile f = RootFile.fromLsResult(file, line);
            if(f != null){
                files.add(f);
            }
            /*if (!".".equalsIgnoreCase(line) && !"..".equalsIgnoreCase(line)) {
                final RootFile f = RootFile.fromLsResult(file, line);
                files.add(f);
            }*/
        }
        files.trimToSize();
        return files;
    }



    /**
     * Crea una nuova cartella. Da usare in un task separato
     * @param percorso Percorso in cui creare la nuova cartella
     * @param nomeCartella Nome della nuova cartella
     * @return True se la nuova cartella già esiste o se viene creata Correttamente.
     * False se percorso è null, se nome cartella è null o se non è stato possibile creare la nuova cartella
     */
    public boolean creaCartella(File percorso, String nomeCartella){
        if(percorso == null || nomeCartella == null) return false;
        final File nuovaCartella = new File(percorso, nomeCartella.trim());
        if(nuovaCartella.exists()) return true;
        //provo a creare la cartella col metodo normale
        boolean success = nuovaCartella.mkdir();
        if(!success){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(nuovaCartella)) {
                // Try with Storage Access Framework.
                final DocumentFile document = SAFUtils.getDocumentFile(context, nuovaCartella, true);
                // getDocumentFile implicitly creates the directory.
                success = document != null && document.exists();
            }
        }
        if(!success && permessiRoot){
            //se la creazione della cartella fallisce, riprovo con i permessi di root
            success = creaCartellaComeRoot(nuovaCartella);
        }
        return success;
    }



    /**
     * Crea una nuova cartella usando i permessi di root. Da usare in un task separato
     * @param cartella Percorso completo della nuova cartella
     * @return True se al termine della creazione la nuova cartella esiste
     */
    private boolean creaCartellaComeRoot(File cartella){
        boolean montareInRO = mountpointManager.mountAsRW(cartella);
        final List<String> results = RootUtils.sendCommands(true, "mkdir \"" + cartella.getAbsolutePath() + "\"");
        if(montareInRO){
            mountpointManager.mountAsRO(cartella);
        }
        return results.isEmpty();
    }


    /**
     * Copia un file utilizzando i permessi di root. Da usare in un task separato
     * @param fileOrigine File da copiare
     * @param fileDestinazione File di destinazione
     * @param montaInRW True se si vuole montare il mountpoint del file in RW. False se il montaggio viene gestito esternamente.
     * @return True se al termine dell'operazione il file di destinazione esiste
     */
    public boolean copiaFileComeRoot(File fileOrigine, File fileDestinazione, boolean montaInRW){
        if(fileOrigine.isDirectory() || fileDestinazione == null) return false;
        boolean montareInRO = false;
        if(montaInRW){
            montareInRO = mountpointManager.mountAsRW(fileDestinazione);
        }
        final String rawPathDestinazione = getRawPath(fileDestinazione);
        String comandoCopia = "cat \"" + getRawPath(fileOrigine) + "\" > \"" + rawPathDestinazione + "\"";
        final String comandoPermessi = "chmod 777 \"" + rawPathDestinazione + "\""; //assegno i permessi perchè su alcune versioni di android il file copiato non può essere letto
        final List<String> results = RootUtils.sendCommands(true, comandoCopia, comandoPermessi);
        if(montareInRO && montaInRW){ //se è da montare in RO ma prima è stato montato in RW
            mountpointManager.mountAsRO(fileDestinazione);
        }
        return results.isEmpty(); //nessun errore se la copia va a buon fine
    }


    /**
     * Android tramite il metodo Environment.getExternalStorageDirectory() restituisce la memoria emulata.
     * Purtroppo alcune versioni di Android non sono in grado di risolvere il symlink alla cartella effettiva in cui sono i files della SD Card.
     * Questo metodo tenta di ricostruire la cartella originale della Sd card utile per le funzioni da shell.
     * @param file File
     * @return Cartella originale della SD Card
     */
    private String getRawPath(File file){
        if(file == null){
            return null;
        }
        final Set<String> possibiliRawPath = new HashSet<>();
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        if(rawExternalStorage != null && !rawExternalStorage.isEmpty()){
            possibiliRawPath.add(rawExternalStorage);
        }
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if(rawEmulatedStorageTarget != null && !rawEmulatedStorageTarget.isEmpty()){
            possibiliRawPath.add(rawEmulatedStorageTarget);
        }
        final String defaultStoragePath = "/storage/sdcard0"; //se le variabili "EXTERNAL_STORAGE" e "EMULATED_STORAGE_TARGET" non sono impostate, Android usa questo path
        possibiliRawPath.add(defaultStoragePath);

        final String standardInternalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        String rawPath = file.getAbsolutePath();
        if(file.getAbsolutePath().startsWith(standardInternalStorage)){
            for(String possibileRawPath : possibiliRawPath){
                final String pathDaProvare = file.getAbsolutePath().replace(standardInternalStorage, possibileRawPath);
                if(new File(pathDaProvare).exists()){
                    rawPath = pathDaProvare;
                    break;
                }
            }
        }
        return rawPath;
    }


    /**
     * Cancella un file. Da usare in un task separato
     * @param file File da cancellare
     * @param montaInRW True se si vuole montare il mountpoint del file in RW. False se il montaggio viene gestito esternamente.
     * @return True se il file è stato cancellato
     */
    public boolean cancella(File file, boolean montaInRW){
        if(file == null) return false;
        final File fileToDelete = getCorrectCaseExistingFile(file);
        //metodo normale
        boolean success = fileToDelete.delete();
        if(!success){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(fileToDelete)) {
                // Try with Storage Access Framework.
                final DocumentFile document = SAFUtils.getDocumentFile(context, fileToDelete, false);
                success = document != null && document.delete();
            }
        }
        if(!success && permessiRoot){
            //se la cancellazione fallisce, riprovo con i permessi di root
            success = cancellaComeRoot(fileToDelete, montaInRW);
        }
        return success;
    }



    /**
     * Cancella un file usando i permessi di root. Da usare in un task separato
     * @param file File da cancellare
     * @param montaInRW True se si vuole montare il mountpoint del file in RW. False se il montaggio viene gestito esternamente.
     * @return True se il file è stato cancellato
     */
    private boolean cancellaComeRoot(File file, boolean montaInRW){
        if(file == null) return false;
        boolean montareInRO = false;
        if(montaInRW){
            montareInRO = mountpointManager.mountAsRW(file);
        }
        final String comandoCancellazione = "rm -R \"" + file.getAbsolutePath() + "\"";
        final List<String> results = RootUtils.sendCommands(true, comandoCancellazione);
        if(montareInRO && montaInRW){ //se è da montare in RO ma prima è stato montato in RW
            mountpointManager.mountAsRO(file);
        }
        return results.isEmpty();
    }




    /**
     * Rinomina un file. Da usare in un task separato
     * @param file File da rinominare
     * @param nuovoNome Nuovo nome del file
     * @param montaInRW True se si vuole montare il mountpoint del file in RW. False se il montaggio viene gestito esternamente.
     * @return True se il file è stato rinominato correttamente
     * @throws FileExistException Viene lanciata l'eccezione se il nuovo nome appartiene ad un file già esistente in quel percorso
     */
    public boolean rinomina(File file, String nuovoNome, boolean montaInRW) throws FileExistException {
        if(file == null || file.getAbsolutePath().equals("/")) return false;
        final File nuovoFile = new File(file.getParent(), nuovoNome);
        if(nuovoFile.exists()){
            throw new FileExistException();
        }
        boolean success = file.renameTo(nuovoFile);
        if(!success){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(file)) {
                // Try with Storage Access Framework.
                final DocumentFile document = SAFUtils.getDocumentFile(context, file, false);
                success = document != null && document.renameTo(nuovoNome);
            }
        }
        if(!success && permessiRoot){
            //se la rinominazione fallisce, riprovo con i permessi di root
            success = rinominaComeRoot(file, nuovoNome, montaInRW);
        }
        return success;
    }



    /**
     * Rinomina un file usando i permessi di root. Da usare in un task separato
     * @param file File da rinominare
     * @param nuovoNome Nuovo nome del file
     * @param montaInRW True se si vuole montare il mountpoint del file in RW. False se il montaggio viene gestito esternamente.
     * @return True se il file è stato rinominato correttamente
     */
    private boolean rinominaComeRoot(File file, String nuovoNome, boolean montaInRW){
        if(file == null) return false;
        file = new File(file.getParent(), file.getName().replace("\uFFFD", "?"));
        final File nuovoFile = new File(file.getParent(), nuovoNome);
        boolean montareInRO = false;
        if(montaInRW){
            montareInRO = mountpointManager.mountAsRW(file);
        }
        final String comandoRinominazione = "mv \"" + file.getAbsolutePath() + "\" \"" + nuovoFile.getAbsolutePath() + "\"";
        final List<String> results = RootUtils.sendCommands(true, comandoRinominazione);
        if(montareInRO && montaInRW){ //se è da montare in RO ma prima è stato montato in RW
            mountpointManager.mountAsRO(file);
        }
        return results.isEmpty();
    }


    /**
     * Crea un file vuoto
     * @param file Percorso del file da creare
     * @return True se il file viene creato o se il file già esiste
     */
    public boolean creaFile(File file){
        if(file == null || file.isDirectory()) return false;
        if(file.exists()) return true;
        boolean success;
        try {
            success = file.createNewFile();
        } catch (IOException e) {
            success = false;
        }
        if(!success){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(file)) {
                // Try with Storage Access Framework.
                final DocumentFile document = SAFUtils.getDocumentFile(context, file, false); //crea il file
                success = document != null;
            }
        }
        if(!success && permessiRoot){
            //se la creazione fallisce, riprovo con i permessi di root
            success = creaFileComeRoot(file);
        }
        return success;
    }



    /**
     *
     * @param file Percorso del file da creare
     * @return True se il file viene creato
     */
    private boolean creaFileComeRoot(File file){
        if(file == null) return false;
        boolean ripristinaRo = mountpointManager.mountAsRW(file);
        final String comandoCreazione = "touch \"" + file.getAbsolutePath() + "\"";
        final List<String> results = RootUtils.sendCommands(true, comandoCreazione);
        if(ripristinaRo){
            mountpointManager.mountAsRO(file);
        }
        return results.isEmpty();
    }



    /*
     * Legge i permessi dei files che si trovano in percorsi root
     * @param file File da analizzare
     * @return Stringa che rappresenta i permessi
     */
    String leggiPermessiFileRoot(File file){
        if(file == null){
            return null;
        }
        final File parent = file.getParentFile();
        final List<File> files = lsComeRoot(parent);
        File rootFile = null;
        for(File f : files){
            if(f.equals(file)){
                rootFile = f;
            }
        }
        if(rootFile != null){
            return  ((RootFile)rootFile).getPermissions();
        } else {
            return null;
        }


        /*try {
            String infos;
            if (!file.isDirectory()) {
                final List<String> result = RootUtils.sendCommands("ls -l \"" + file.getAbsolutePath() + "\"");
                infos = result.get(0);
            } else {
                final List<String> result = RootUtils.sendCommands("ls -ld \"" + file.getAbsolutePath() + "\"");
                infos = result.get(0);
            }
            return infos.substring(1, 4) + " " + infos.substring(4, 7) + " " + infos.substring(7, 10);
        } catch (Exception e){
            return null;
        }*/
    }


    /**
     * Modifica i permessi di un file che si trova nella cartella root
     * @param file File di cui modificare i permessi
     * @param permessi Permessi in modalità ottale (esempio 777)
     * @return True se i permessi vengono modificati. False se non è possibile modificare i permessi, se il file è null, se il file non esiste o se la stringa con i permessi è in un formato non valido
     */
    boolean cambiaPermessiFileRoot(File file, String permessi){
        if(file == null || !file.exists() || permessi == null) {
            return false;
        }
        final Pattern pattern = Pattern.compile("[0-7][0-7][0-7]");
        final Matcher matcher = pattern.matcher(permessi);
        if(matcher.matches()){
            boolean montareInRO = mountpointManager.mountAsRW(file);
            final List<String> results = RootUtils.sendCommands("chmod " + permessi + " \"" + file.getAbsolutePath() + "\"");
            if(montareInRO){
                mountpointManager.mountAsRO(file);
            }
            return results.isEmpty(); //vuoto se non ci sono errori
        }
        return false;
    }


    /**
     * Restituisce lo spazio disponibile per la cartella
     * @param file Cartella da analizzare
     * @return Spazio libero in bytes
     */
    public long getFreeSpace(File file){
        if(file == null) return 0L;
        long freeSpace = file.getUsableSpace();
        if(freeSpace == 0 && permessiRoot){
            //provo la lettura con root
            final String comandoFreeSpace = "df \"" + file.getAbsolutePath() + "\"";
            final List<String> results = RootUtils.sendCommands(comandoFreeSpace);
            if(results.size() == 2){
                final String line = results.get(1);
                final String[] split = line.split("\\s+");
                if(split != null && split.length == 6){
                    final String available = split[3];
                    try {
                        freeSpace = Long.parseLong(available);
                    } catch (NumberFormatException ignored){}
                }
            }
        }
        return freeSpace;
    }


    public boolean fileExists(File file){
        if(file == null) return false;
        if(storagesUtils.isOnRootPath(file) && permessiRoot){
            //percorsi root
            return LocalFileUtils.rootFileExists(file);
        } else {
            //percorsi normale o senza permessi di root
            return file.exists();
        }
    }



    /**
     * Verifica l'esistenza di files case sensitive
     * Se scrivo Capture.jpg ma è presente un file capture.jpg il metodo exists() ritorna ugualmente true. Con questo metodo ottengo il file che già è esistente.
     * @param file File da verificare
     * @return File realmente esistente
     */
    private File getCorrectCaseExistingFile(File file){
        if(file.exists()){
            final File parent = file.getParentFile();
            if(parent == null) return file;
            final List<File> childs = ls(parent);
            if(childs != null){
                for(File f : childs){
                    if(f.getName().equalsIgnoreCase(file.getName())){
                        return f;
                    }
                }
            }
        }
        return file;
    }


    /**
     * Avvia il service, catturando l'eccezione se ci sono troppi elementi da gestire
     * @param serviceIntent Intent del service da avviare
     */
    private void startService(@NonNull Intent serviceIntent){
        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e){
            Log.e(getClass().getSimpleName(), e.getMessage());
            ColoredToast.makeText(context, R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Ottiene lo stato "root explorer" impostato (se il file manager deve usare i permessi di root)
     */
    public void ottieniStatoRootExplorer(){
        boolean rootExplorer = prefs.getBoolean(KEY_PREF_ROOT_EXPLORER, false);
        setPermessiRoot(rootExplorer);
    }



    /**
     * Eccezione per file esistenti usata nel metodo rinomina
     */
    public class FileExistException extends Exception {
    }
}
