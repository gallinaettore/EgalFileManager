package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.NotNull;

import it.Ettore.androidutilsx.ui.ColoredProgressDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.iconmanager.LoadAppChooserIconThread;


/**
 * Classe che gestisce l'apertura di un file
 */
public class FileOpener {
    private final Context context;
    private final AssociazioneFiles associazioneFiles;
    private AlertDialog appChooserDialog, apriComeDialog;
    private final PackageManager pm;


    public FileOpener(Context context){
        this.context = context;
        this.associazioneFiles = new AssociazioneFiles(context);
        this.pm = context.getPackageManager();
    }



    /**
     * Crea l'intent per aprire il file
     * @param file File da aprire
     * @param mimeType Tipo mime
     * @return Intent per aprire il file
     */
    @NotNull
    private Intent getIntent(File file, String mimeType){
        final Uri path = FileUtils.uriWithFileProvider(context, file);
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(path, mimeType);
        intent.setAction(Intent.ACTION_VIEW);
        return intent;
    }



    /**
     * Apre il file, facendo scegliere il mime type automaticamente
     * @param file File da aprire
     */
    public void openFile(final File file){
        openFile(file, null);
    }



    /**
     * Apre il file immettendo manualmente il mime type
     * @param file File da aprire
     * @param mimeType Tipo mime
     */
    private void openFile(final File file, String mimeType){
        if(file == null || !file.exists()) return;

        String newMime;
        if(mimeType == null){
            //mime non impostato, lo ottengo automaticamente
            newMime = FileUtils.getMimeType(file);
        } else {
            newMime = mimeType;
        }

        final String estenzione = FileUtils.getFileExtension(file);
        final ComponentName componentNameAssociato = associazioneFiles.getComponentNameAssociato(estenzione);
        if(componentNameAssociato == null){
            //l'estenzione non è associata a nessuna applicazione
            if(mimeType == null && newMime.equals("*/*")){
                //ricerca del mime automatica, non non è stato restituito un mime soddisfacente
                openFileAs(file);
            } else {
                //mime ottenuto
                new ResolveInfoListTask(context, getIntent(file, newMime), file).execute();
            }
        } else {
            //trovata applicazione associata
            try {
                if(mimeType != null){
                    //è stato inviato appositamente un mime per permettere la scelta dell'applicazione con cui aprilo
                    new ResolveInfoListTask(context, getIntent(file, newMime), file).execute();
                } else {
                    //apre con l'app associata
                    final Intent intentAppPredefinita = getIntent(file, newMime);
                    intentAppPredefinita.setComponent(componentNameAssociato);
                    intentAppPredefinita.addCategory(Intent.CATEGORY_LAUNCHER);
                    context.startActivity(intentAppPredefinita);
                }
            } catch (Exception e){
                //se non riesce, ripropone l'app chooser
                if(mimeType == null && newMime.equals("*/*")){
                    //ricerca del mime automatica, non non è stato restituito un mime soddisfacente
                    openFileAs(file);
                } else {
                    new ResolveInfoListTask(context, getIntent(file, newMime), file).execute();
                }
            }
        }
    }



    /**
     * Mostra la dialog di scelta del mime type
     * @param file File da aprire
     */
    public void openFileAs(File file){
        showMimeChooserDialog(file);
    }



    /**
     * Condivide i files
     * @param files Lista di files da condividere
     */
    public void shareFiles(List<File> files){
        if(files == null || files.isEmpty()) return;
        Intent sharingIntent;
        String mime;
        if(files.size() == 1){
            //condivisione singolo file
            sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            final File file = files.get(0);
            mime = FileUtils.getMimeType(file);
            final Uri uri = FileUtils.uriWithFileProvider(context, file);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        } else {
            //condivisione di più files
            sharingIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
            mime = FileUtils.getMimeType(files);
            final ArrayList<Uri> uriFiles = new ArrayList<>();
            for(File file : files){
                uriFiles.add(FileUtils.uriWithFileProvider(context, file));
            }
            sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriFiles);
        }
        sharingIntent.setType(mime);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.condividi)));
        } catch (Exception e){
            ColoredToast.makeText(context, R.string.troppi_elementi_da_gestire, Toast.LENGTH_LONG).show();
            //si potrebbe generare una TransactionTooLargeException se il numero di files da condividere è troppo elevato
            e.printStackTrace();
        }
    }




    /**
     * Mostra la dialog di scelta dell'app da usare per aprire il file
     * @param launchables Lista di resolve info che contiene le applicazioni in grado di aprire quel tipo di file
     * @param intent Intent per l'apertura
     * @param estenzione Estenzione del file
     */
    private void showAppChooserDialog(final List<ResolveInfo> launchables, final Intent intent, final String estenzione){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.hideIcon(true);
        builder.setTitle(R.string.apri_con);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_chooser_lista_applicazioni, null);
        final LinearLayout layoutListaApp = view.findViewById(R.id.layout_lista_app);
        final ExecutorService executorService = Executors.newFixedThreadPool(8);
        final CheckBox checkboxUsaApp = view.findViewById(R.id.checkbox_usa_sempre_app);
        for(final ResolveInfo resolveInfo : launchables){
            final View riga = inflater.inflate(R.layout.riga_chooser_app, layoutListaApp, false);
            final ImageView imageView = riga.findViewById(R.id.imageview_icona_app);
            final TextView textView = riga.findViewById(R.id.textView_nome_app);
            executorService.execute(new LoadAppChooserIconThread(context, imageView, resolveInfo, pm));
            textView.setText(resolveInfo.loadLabel(pm));
            riga.setOnClickListener(view1 -> {
                //Apro il file
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                final ComponentName name = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(name);
                try {
                    context.startActivity(intent);
                    //salvo l'associazione estenzione/applicazione
                    if(checkboxUsaApp.isChecked()){
                        associazioneFiles.associaEstenzione(estenzione, activityInfo);
                    }
                } catch (Exception e){
                    ColoredToast.makeText(context, R.string.impossibile_completare_operazione, Toast.LENGTH_LONG).show();
                }
                //chiudo la dialog
                if(appChooserDialog != null && appChooserDialog.isShowing()){
                    appChooserDialog.dismiss();
                }
            });
            layoutListaApp.addView(riga);
        }

        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, null);
        appChooserDialog = builder.create();
        appChooserDialog.show();
    }




    /**
     * Mostra la dialog di scelta del mime type
     * @param file File da aprire
     */
    private void showMimeChooserDialog(final File file){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.hideIcon(true);
        builder.setTitle(R.string.apri_come);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_chooser_tipo_mime, null);
        builder.setView(view);
        final LinearLayout layoutLista = view.findViewById(R.id.layout_lista);
        for(final Tipo tipo : Tipo.values()){
            final View riga = inflater.inflate(R.layout.riga_chooser_mime, layoutLista, false);
            final ImageView imageView = riga.findViewById(R.id.imageview_icona);
            final TextView textView = riga.findViewById(R.id.textview_tipo);
            imageView.setImageResource(tipo.resIdIcona);
            textView.setText(tipo.resIdNome);
            riga.setOnClickListener(view1 -> {
                //apro il file impostando il tipo mime
                final String mime = tipo.mime;
                openFile(file, mime);
                //chiudo la dialog
                if(apriComeDialog != null && apriComeDialog.isShowing()){
                    apriComeDialog.dismiss();
                }
            });
            layoutLista.addView(riga);
        }

        builder.setNegativeButton(android.R.string.cancel, null);
        apriComeDialog = builder.create();
        apriComeDialog.show();
    }



    /**
     * Enum con mime types generali
     */
    private enum Tipo {
        TESTO(R.string.testo, R.drawable.ico_file_testo, "text/*"),
        IMMAGINE(R.string.immagine, R.drawable.ico_file_immagine, "image/*"),
        AUDIO(R.string.audio, R.drawable.ico_file_audio, "audio/*"),
        VIDEO(R.string.video, R.drawable.ico_file_video, "video/*"),
        ALTRO(R.string.altro, R.drawable.ico_file, "*/*");

        private final int resIdNome;
        private final int resIdIcona;
        private final String mime;

        Tipo(@StringRes int resIdNome, @DrawableRes int resIdIcona, String mime){
            this.resIdNome = resIdNome;
            this.resIdIcona = resIdIcona;
            this.mime = mime;
        }
    }









    /**
     * Asynctask per ottenere la lista delle applicazioni che possono aprire quel mime
     */
    private class ResolveInfoListTask extends AsyncTask<Void, Void, List<ResolveInfo>> {
        private final WeakReference<Context> context;
        private final Intent intent;
        private final File file;
        private ColoredProgressDialog progress;

        private ResolveInfoListTask(Context context, Intent intent, File file){
            this.context = new WeakReference<>(context);
            this.intent = intent;
            this.file = file;
        }

        @Override
        protected void onPreExecute(){
            progress = ColoredProgressDialog.show(context.get(), null, context.get().getString(R.string.lettura_lista_applicazioni));
            progress.setCancelable(false);
        }

        @Override
        protected List<ResolveInfo> doInBackground(Void... params){
            final List<ResolveInfo> launchables = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            final List<String> labelList = new ArrayList<>(launchables.size());
            final List<ResolveInfo> senzaDuplicati = new ArrayList<>();
            for(ResolveInfo info : launchables){
                final String appLabel = info.loadLabel(pm).toString();
                if(appLabel != null && !appLabel.isEmpty() && !labelList.contains(appLabel)){
                    labelList.add(appLabel);
                    senzaDuplicati.add(info);
                }
            }
            Collections.sort(senzaDuplicati, new ResolveInfo.DisplayNameComparator(pm));
            return senzaDuplicati;
        }

        @Override
        protected void onPostExecute(List<ResolveInfo> launchables){
            try {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
            } catch (final IllegalArgumentException ignored) {}
            if(launchables != null && !launchables.isEmpty()){
                showAppChooserDialog(launchables, intent, FileUtils.getFileExtension(file));
            } else {
                //se nessuna app è in grado di aprire quel file
                showMimeChooserDialog(file);
            }
        }
    }
}
