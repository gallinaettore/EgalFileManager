package it.Ettore.egalfilemanager.filemanager;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogPermessiFileRootBuilder;
import it.Ettore.egalfilemanager.fileutils.RootFile;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.view.ViewProprieta;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Classe per la visualizzazione delle proprietà di un file. Esegue l'analisi in un task separato.
 */
public class ProprietaTask extends AsyncTask <Void, Object, Void> {
    private static final int FREQ_AGGIORNAMENTO = 300;
    private final FileManager fileManager;
    private final WeakReference<Activity> activity;
    private final ProprietaNascondiListener listener;
    private final List<File> listaFiles;
    private AlertDialog dialog;
    private final File primoFile;
    private long totBytes, ultimoAggiornamento;
    private int totFiles, totCartelle;
    private boolean isRootFile, modificaVisibilita = true;
    private String permessiUnix;
    private final AtomicBoolean annulla;
    private WeakReference<ViewProprieta> viewProprieta;


    /**
     *
     * @param activity Activity
     * @param listaFiles Lista files da analizzare
     * @param listener Listener eseguito quando si modifica una proprietà (come ad esempio la visibilità)
     */
    ProprietaTask(@NonNull Activity activity, @NonNull List<File> listaFiles, ProprietaNascondiListener listener){
        this.activity = new WeakReference<>(activity);
        this.fileManager = new FileManager(activity);
        this.fileManager.ottieniStatoRootExplorer();
        this.listener = listener;
        this.listaFiles = listaFiles;
        this.primoFile = listaFiles.get(0);
        this.annulla = new AtomicBoolean(false);
    }


    /**
     * Permette o meno di abilitare la modifica della visibilità. Nella categorie non deve essere permessa la modifica.
     * @param modificaVisibilita Abilitazione modifica visibilità
     */
    void setModificaVisibilita(boolean modificaVisibilita){
        this.modificaVisibilita = modificaVisibilita;
    }


    /**
     * Crea la dialog e mostra le informazioni subito disponibili
     */
    @Override
    protected void onPreExecute(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(activity.get());
        builder.setTitle(R.string.proprieta);
        builder.hideIcon(true);

        viewProprieta = new WeakReference<>(new ViewProprieta(activity.get(), listaFiles.size() == 1));

        //analizza tutti i percorsi e imposta il percorso a null se sono differenti
        String percorsoComune = primoFile.getParent();
        for(int i=1; i < listaFiles.size(); i++){
            if(!listaFiles.get(i).getParent().equals(percorsoComune)){
                percorsoComune = null;
                break;
            }
        }
        boolean isSymLink = false;
        if(listaFiles.size() == 1 && FileUtils.isSymlink(primoFile)){
            isSymLink = true;
            try {
                percorsoComune = primoFile.getCanonicalPath();
            } catch (IOException ignored) {}
        }

        viewProprieta.get().setFile(primoFile.getName(), primoFile.isDirectory(), isSymLink);
        viewProprieta.get().setPercorso(percorsoComune);
        viewProprieta.get().setDimensione(primoFile.length());
        viewProprieta.get().setData(primoFile.lastModified());

        if(listaFiles.size() == 1){
            final StoragesUtils storagesUtils = new StoragesUtils(activity.get());
            if(storagesUtils.isOnRootPath(primoFile)){
                //se il file non si trova nè nella momoria interna, nè in una sd esterna allora è un file root
                //la text view verrà popolata nell'asynctask
                isRootFile = true;
            } else {
                //file comuni
                viewProprieta.get().setPermessiNormali(primoFile.canRead(), primoFile.canWrite());
            }
        }

        viewProprieta.get().setNascosto(primoFile.isHidden(), modificaVisibilita);
        builder.setView(viewProprieta.get());

        builder.setNeutralButton(android.R.string.ok, (dialogInterface, i) -> {
            if(primoFile.isHidden() != viewProprieta.get().nascostoCheckBoxIsChecked() && listener != null){
                listener.onFileManagerHidePropertyChanged(primoFile, viewProprieta.get().nascostoCheckBoxIsChecked());
            }
        });

        builder.setOnDismissListener(dialogInterface -> annulla.set(true));

        dialog = builder.create();
        dialog.show();
    }


    /**
     * Analizza in background e mostra le informazioni via via che vengono trovate
     * @param params Nessun parametro
     * @return Void
     */
    @Override
    protected Void doInBackground(Void... params) {
        //permessi unix del primo file (su file multipli non saranno visualizzati)
        if(isRootFile && primoFile instanceof RootFile) {
            //leggo i permessi tramite file manager e non quelli impostati sul file perchè se cambio i permessi non si vedrebvbero le modifiche
            permessiUnix = fileManager.leggiPermessiFileRoot(primoFile);
            publishProgress();
        }

        //analizzo file e directory
        for(File file : listaFiles){
            analizzaFileRicorsivo(file);
        }
        //le directory principali non vengono conteggiate (quindi le rimuovo dal totale)
        for(File file : listaFiles){
            if(file.isDirectory()){
                totCartelle--;
            }
        }
        publishProgress();
        return null;
    }


    /**
     * Aggiorna la dialog
     * @param values .
     */
    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);

        if (activity.get() != null && !activity.get().isFinishing() && dialog != null && dialog.isShowing() && viewProprieta.get() != null) {
            viewProprieta.get().setDimensione(totBytes);
            viewProprieta.get().setContenuto(totFiles, totCartelle);
            if(isRootFile) {
                viewProprieta.get().setPermessiUnix(permessiUnix);
                viewProprieta.get().setPermessiUnixModificabili(view -> {
                    final DialogPermessiFileRootBuilder builder = new DialogPermessiFileRootBuilder(activity.get(), permessiUnix, newPermissions -> {
                        boolean success = fileManager.cambiaPermessiFileRoot(primoFile, newPermissions);
                        if (success) {
                            final String permissionsString = octalToStringPermissions(newPermissions);
                            if (permissionsString != null) {
                                ProprietaTask.this.permessiUnix = permissionsString;
                                viewProprieta.get().setPermessiUnix(permissionsString);
                            }
                        } else {
                            ColoredToast.makeText(activity.get(), R.string.impossibile_completare_operazione, Toast.LENGTH_LONG).show();
                        }
                    });
                    final AlertDialog dialog = builder.create();
                    if (dialog != null) {
                        //dialog è null le la stringa con i permessi non è valida
                        dialog.show();
                    } else {
                        ColoredToast.makeText(activity.get(), R.string.impossibile_completare_operazione, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }


    /**
     * Analizza i file ricorsivamente per calcolare anche la dimensione delle cartelle e il loro contenuto
     * @param file File o cartella da analizzare
     */
    private void analizzaFileRicorsivo(File file){
        if(annulla.get()) return;
        final long now = System.currentTimeMillis();
        if(!file.isDirectory()){
            totBytes += file.length();
            totFiles ++;
            if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                publishProgress();
                ultimoAggiornamento = now;
            }
        } else {
            totCartelle++;
            if(now - ultimoAggiornamento > FREQ_AGGIORNAMENTO){
                publishProgress();
                ultimoAggiornamento = now;
            }
            final List<File> listaFile = fileManager.ls(file); //utilizzando il file manager posso usare anche i permessi di root
            if(listaFile != null) {
                for (File f : listaFile) {
                    analizzaFileRicorsivo(f);
                }
            }
        }
    }


    /**
     * Trasforma un'intera stringa ottale (esempio: 777) nel formato visualizzato nella dialog (esempio: rwx rwx rwx)
     * @param octal Notazione ottale
     * @return Stringa nel formato classico. Null se la stringa passata non è valida o è null.
     */
    private String octalToStringPermissions(String octal){
        if(octal == null || octal.length() != 3){
            return null;
        }
        final String proprietario = intToStringPermission(octal.substring(0, 1));
        final String gruppo = intToStringPermission(octal.substring(1, 2));
        final String altro = intToStringPermission(octal.substring(2, 3));
        if(proprietario != null && gruppo != null && altro != null) {
            return proprietario + " " + gruppo + " " + altro;
        } else {
            return null;
        }
    }


    /**
     * Trasforma un intero del formato ottale in formato stringa (esempio 7 -> rwx)
     * @param numString Stringa che rappresenta un numero da 0 a 7
     * @return Stringa in formato normale. Null se la stringa passata non è valida.
     */
    private String intToStringPermission(String numString){
        int num;
        try{
            num = Integer.parseInt(numString);
        } catch (NumberFormatException e){
            return null;
        }
        switch (num){
            case 0:
                return "---";
            case 1:
                return "--x";
            case 2:
                return "-w-";
            case 3:
                return "-wx";
            case 4:
                return "r--";
            case 5:
                return "r-x";
            case 6:
                return "rw-";
            case 7:
                return "rwx";
            default:
                return null;
        }
    }


    /**
     * Listener per il cambiamento della prospietà "nascosto"
     */
    public interface ProprietaNascondiListener {

        /**
         * Chiamato quando si modifica una proprietà del file
         * @param file File modificato
         * @param hidden True se nascosto. False se visibile.
         */
        void onFileManagerHidePropertyChanged(File file, boolean hidden);
    }
}
