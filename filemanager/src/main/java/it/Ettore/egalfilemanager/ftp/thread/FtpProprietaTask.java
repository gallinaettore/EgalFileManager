package it.Ettore.egalfilemanager.ftp.thread;

import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.FtpFileUtils;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.view.ViewProprieta;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Task per la visualizzazione delle proprietà di un file ftp
 */
public class FtpProprietaTask extends AsyncTask <Void, Object, Void> {
    private static final int FREQ_AGGIORNAMENTO = 300;
    private final WeakReference<Activity> activity;
    private final List<FtpElement> listaFiles;
    private AlertDialog dialog;
    private final FtpElement primoFile;
    private final AtomicBoolean annulla;
    private long totBytes, ultimoAggiornamento;
    private int totFiles, totCartelle;
    private final FtpSession ftpSession;
    private WeakReference<ViewProprieta> viewProprieta;


    /**
     *
     * @param activity Activity chiamante
     * @param listaFiles Lista files da analizzare
     */
    public FtpProprietaTask(@NonNull Activity activity, @NonNull List<FtpElement> listaFiles, @NonNull FtpSession ftpSession){
        this.activity = new WeakReference<>(activity);
        this.listaFiles = listaFiles;
        this.primoFile = listaFiles.get(0);
        this.annulla = new AtomicBoolean(false);
        this.ftpSession = ftpSession;
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
        if(percorsoComune != null){
            percorsoComune = primoFile.getHostName() + percorsoComune;
        }
        viewProprieta.get().setPercorso(percorsoComune);
        viewProprieta.get().setFile(primoFile.getName(), primoFile.isDirectory(), false);
        viewProprieta.get().setPercorso(percorsoComune);
        viewProprieta.get().setDimensione(primoFile.getSize());
        viewProprieta.get().setData(primoFile.getDate());

        if(primoFile.isUnix()){
            viewProprieta.get().setPermessiUnix(primoFile.getUnixPermissions());
        } else {
            //non è possibile sapere se un file è nascosto su server windows
            viewProprieta.get().nascondiRigaFileNascosto();
        }
        viewProprieta.get().setNascosto(primoFile.isHidden(), false);

        builder.setView(viewProprieta.get());
        builder.setNeutralButton(android.R.string.ok, null);
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
        if(ftpSession == null || ftpSession.getFtpClient() == null || ftpSession.getServerFtp() == null || listaFiles == null) return null;

        //analizzo file e directory
        for(FtpElement file : listaFiles){
            analizzaFileRicorsivo(file);
        }

        //le directory principali non vengono conteggiate (quindi le rimuovo dal totale)
        for(FtpElement file : listaFiles){
            if(file.isDirectory()){
                totCartelle--;
            }
        }
        publishProgress();

        return null;
    }


    /**
     * Aggiorna la dialog
     * @param values Nessun valore da passare
     */
    @Override
    protected void onProgressUpdate(Object... values) {
        if (activity.get() != null && !activity.get().isFinishing() && dialog != null && dialog.isShowing() && viewProprieta.get() != null) {
            viewProprieta.get().setDimensione(totBytes);
            viewProprieta.get().setContenuto(totFiles, totCartelle);
        }
    }




    /**
     * Analizza i file ricorsivamente per calcolare anche la dimensione delle cartelle e il loro contenuto
     * @param file File o cartella da analizzare
     */
    private void analizzaFileRicorsivo(FtpElement file){
        if(annulla.get()) return;
        if (!file.isDirectory()) {
            totBytes += file.getSize();
            totFiles++;
            final long now = System.currentTimeMillis();
            if (now - ultimoAggiornamento > FREQ_AGGIORNAMENTO) {
                publishProgress();
                ultimoAggiornamento = now;
            }
        } else {
            totCartelle++;
            final List<FtpElement> listaFiles = FtpFileUtils.explorePath(ftpSession, file.getAbsolutePath());
            for(FtpElement ftpElement : listaFiles){
                analizzaFileRicorsivo(ftpElement);
            }
        }
    }
}
