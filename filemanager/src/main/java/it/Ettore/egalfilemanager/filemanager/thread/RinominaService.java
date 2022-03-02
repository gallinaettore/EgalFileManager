package it.Ettore.egalfilemanager.filemanager.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.MultirinominaFilesLocali;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.mount.MountUtils;


/**
 * Servizio che esegue la rinominazione di files locali
 */
public class RinominaService extends BaseProgressService {
    private static final String KEYBUNDLE_NUOVO_NOME = "nuovo_nome";
    private static final String KEYBUNDLE_HIDE_MODE = "hide_mode";

    private FileManager fileManager;
    private List<File> filesNonRinominati, filesDaCancellareDaMediaStore, filesDaAggiungereSuMediaStore;


    /**
     * Costruttore
     */
    public RinominaService() {
        super("RinominaService");
    }


    /**
     * Crea l'intent per l'esecuzione del service
     * @param context Context chiamante
     * @param listaFiles Lista di files da processare
     * @param nuovoNome Nuovo nome
     * @param hideMode Setta la modalità nascondi (quando si rinominano i files con il punto iniziale)
     * @param handler Handler che permette al service di comunicare con la UI
     * @return Intent per l'esecuzione del service
     */
    public static Intent createStartIntent(@NonNull Context context, @NonNull List<File> listaFiles, @NonNull String nuovoNome, boolean hideMode, @NonNull RinominaHandler handler){
        final ArrayList<String> listaPaths = FileUtils.listFileToListPath(listaFiles);
        final Intent intent = makeStartBaseIntent(context, RinominaService.class, listaPaths, handler);
        intent.putExtra(KEYBUNDLE_NUOVO_NOME, nuovoNome);
        return intent;
    }


    /**
     * Esecuzione in background
     * @param intent Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);

        fileManager = new FileManager(this);
        fileManager.ottieniStatoRootExplorer();
        final MountUtils mountUtils = new MountUtils(this);

        final String nuovoNome = intent.getStringExtra(KEYBUNDLE_NUOVO_NOME);
        final boolean hideMode = intent.getBooleanExtra(KEYBUNDLE_HIDE_MODE, false);

        final List<String> listaPaths = getListaPaths();
        List<File> listaFiles = FileUtils.listPathToListFile(listaPaths);
        listaFiles = LocalFileUtils.fileListToRootFileList(this, listaFiles);

        //verifico la correttezza dei dati
        if (listaFiles == null || nuovoNome == null || listaFiles.isEmpty()) {
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //notifico di avviare la progress dialog (e aggiorno la notifica)
        sendMessageStartOperation(getString(R.string.rinomina), null);

        this.filesNonRinominati = new ArrayList<>();
        this.filesDaCancellareDaMediaStore = new ArrayList<>();
        this.filesDaAggiungereSuMediaStore = new ArrayList<>();

        //verifico che il nome sia valido
        if(!FileUtils.fileNameIsValid(nuovoNome)){
            filesNonRinominati = listaFiles;
            sendMessageError(R.string.nome_non_valido);
            sendMessageOperationFinished(); //sempre prima di ogni return in onHandleIntent()
            return;
        }

        //rinomino i files fisicamente
        try {
            if(listaFiles.size() == 1){
                final File fileDaRinominare = listaFiles.get(0);
                mountUtils.montaInRwSeNecessario(fileDaRinominare);
                final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                sendMessageUpdateProgress(message, 1, listaFiles.size());
                rinomina(fileDaRinominare, nuovoNome);
            } else {
                listaFiles = OrdinatoreFiles.ordinaPerNome(listaFiles);
                final MultirinominaFilesLocali multirinomina = new MultirinominaFilesLocali(nuovoNome, listaFiles.size());
                for(int i=0; i < listaFiles.size(); i++){
                    final File fileDaRinominare = listaFiles.get(i);
                    if(!isRunning()){
                        filesNonRinominati.add(fileDaRinominare);
                        continue;
                    }
                    mountUtils.montaInRwSeNecessario(fileDaRinominare);
                    final String message = String.format(getString(R.string.rinominazione_in_corso), fileDaRinominare.getName());
                    sendMessageUpdateProgress(message, i+1, listaFiles.size());

                    final String nuovoNomeFile = multirinomina.getNuovoNomeFileProgressivo(fileDaRinominare);
                    if(nuovoNomeFile != null){
                        rinomina(fileDaRinominare, nuovoNomeFile);
                    } else {
                        //errore
                        filesNonRinominati.add(fileDaRinominare);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            filesNonRinominati = listaFiles;
        }

        //aggiorno il mediastore
        if(!filesDaCancellareDaMediaStore.isEmpty() && new StoragesUtils(this).isOnSdCard(filesDaCancellareDaMediaStore.get(0))){
            try {
                sendMessageUpdateProgress(R.string.aggiornamento_media_library, listaFiles.size(), listaFiles.size());
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.removeFilesFromMediaLibrary(filesDaCancellareDaMediaStore);
                mediaUtils.addFilesToMediaLibrary(filesDaAggiungereSuMediaStore, () -> sendMessageMediaScannerFinished());
            } catch (Exception ignored){}
        }

        mountUtils.ripristinaRo();

        if(isRunning()){
            if(filesNonRinominati.isEmpty()){
                //tutti i files sono stati rinominati
                if(hideMode){
                    //modalità mostra/nascondi
                    sendMessageSuccessfully(R.string.visibilita_modificata);
                } else {
                    //modalità rinomina
                    sendMessageSuccessfully(String.format(getString(R.string.files_rinominati), String.valueOf(listaFiles.size())));
                }
            } else {
                //Mostro una dialogRinomina con la lista di files non processati
                final StringBuilder sb = new StringBuilder(getString(R.string.files_non_rinominati));
                sb.append("\n");
                for (File file : filesNonRinominati) {
                    sb.append(String.format("\n• %s", file.getAbsolutePath()));
                }
                sendMessageError(sb.toString());
            }
        }

        sendMessageOperationFinished();
    }


    /**
     * Rinomina il file
     * @param fileDaRinominare File
     * @param nuovoNome Nome da assegnare al file
     */
    private void rinomina(File fileDaRinominare, String nuovoNome){
        boolean success;
        try{
            success = rinominaCaseSensitive(fileDaRinominare, nuovoNome);
        } catch (FileManager.FileExistException e){
            success = false;
            if(getListaPaths().size() == 1){
                //c'era un solo file da rinominare e il nome del file corrisponde ad un file già esistente
                sendMessageError(R.string.file_gia_presente);
            }
        }
        if(success){
            filesDaCancellareDaMediaStore.add(fileDaRinominare);
            filesDaAggiungereSuMediaStore.add(new File(fileDaRinominare.getParent(), nuovoNome));
        } else {
            filesNonRinominati.add(fileDaRinominare);
        }
    }


    /**
     * Rinomina il file. Se il nome è uguale ma con maiuscole/minuscole differenti rinomina prima il file con un nome temporaneo e poi lo riporta al nome corretto
     * @param file File da rinominare
     * @param nuovoNome Nuovo nome
     * @return True se è stato rinominato correttamente
     * @throws FileManager.FileExistException Se nella rinominazione ordinaria il nuovo nome corrisponde a quello di un file già esistente
     */
    private boolean rinominaCaseSensitive(@NonNull File file, @NonNull String nuovoNome) throws FileManager.FileExistException {
        boolean success = false;
        if(file.getName().equalsIgnoreCase(nuovoNome) && !file.getName().equals(nuovoNome)){
            //se il nome è uguale ma con maiuscole/minuscole differenti rinomino prima il file con un nome temporaneo
            try {
                final String randomName = MyUtils.getRandomString(25);
                success = fileManager.rinomina(file, randomName, false);
                if(success){
                    final File tempFile = new File(file.getParent(), randomName);
                    success = fileManager.rinomina(tempFile, nuovoNome, false);
                }
                return success;
            } catch (FileManager.FileExistException ignored) {}
        } else {
            //rinominazione normale
            success = fileManager.rinomina(file, nuovoNome, false);
        }
        return success;
    }



    /**
     * Restituisce un oggetto serializzabile che contiene tutti i dati da passare poi al listener
     * (questo oggetto verrà passato al bundle dell'handler che a sua volta passerà i dati al listener)
     * @return Dati da passare al listener
     */
    @Override
    protected Serializable creaDatiPerListener() {
        final RinominaHandler.ListenerData listenerData = new RinominaHandler.ListenerData();
        listenerData.oldFiles = filesDaCancellareDaMediaStore;
        listenerData.newFiles = filesDaAggiungereSuMediaStore;
        return listenerData;
    }
}
