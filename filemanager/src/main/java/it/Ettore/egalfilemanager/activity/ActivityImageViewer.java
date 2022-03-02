package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.filemanager.thread.EliminaHandler;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.fileutils.UriUtils;
import it.Ettore.egalfilemanager.imageviewer.DepthPageTransformer;
import it.Ettore.egalfilemanager.imageviewer.ImagePagerAdapter;
import it.Ettore.egalfilemanager.imageviewer.LoadImageFileTask;
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.widget.MyWidgetManager;

import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_PRESENTAZIONE;


/**
 * Activity per la visualizzazione delle immagini
 */
public class ActivityImageViewer extends BaseActivity implements EliminaHandler.EliminaListener {
    public static final long RITARDO_NASCONDIMENTO = 3000L;
    private static final long DURATA_DIAPOSITIVA_PRESENTAZIONE = 2500L;
    private static final int REQ_ACTIVITY_SET_WALLPAPER = 1;
    private Toolbar toolbar;
    private ViewPager pager;
    private ImagePagerAdapter pagerAdapter;
    private FrameLayout layoutErrore;
    private boolean inAttesaDiNascondimento, modalitaPresentazione;
    private FileManager fileManager;
    private Timer timer;
    private long ritardoPrimaDiapositiva = 1500L;
    private EliminaHandler eliminaHandler;
    private MyWidgetManager widgetManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        settaTitolo(R.string.egal_image_viewer);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        eliminaHandler = new EliminaHandler(this, this);
        widgetManager = new MyWidgetManager(this);
        pager = findViewById(R.id.pager);
        pager.setPageTransformer(true, new DepthPageTransformer()); //animazione quando si cambia pagina, si può omettere questa riga se non si desidera quest'animazione
        layoutErrore = findViewById(R.id.layout_errore);
        layoutErrore.setOnClickListener(view -> mostraToolbar());
        fileManager = new FileManager(this);


        final Serializable elementiPresentazionePassati = getIntent().getSerializableExtra(KEY_BUNDLE_ELEMENTI_PRESENTAZIONE);
        if(elementiPresentazionePassati != null){
            //all'activity viene passata una lista di immagini da visualizzare come presentazione
            final SerializableFileList filesDaIntent = (SerializableFileList)elementiPresentazionePassati;
            //creo l'adapter da visualizzare nel pager e imposto l'indice dell'immagine da visualizzare
            pagerAdapter = new ImagePagerAdapter(this, filesDaIntent.toFileList());
            pager.setAdapter(pagerAdapter);
            modalitaPresentazione = true; //la presentazione sarà avviata nell'onStart
            ritardoPrimaDiapositiva = DURATA_DIAPOSITIVA_PRESENTAZIONE;
        } else {
            //all'activity viene passata un'immagine da visualizzare
            final String type = getIntent().getType();
            if (type != null && type.startsWith("image/")) {
                final Uri fileUri = getIntent().getData();
                final File file = UriUtils.uriToFile(this, fileUri);
                if (file != null) {
                    //creo la lista delle immagini contenute nella stessa cartella dell'immagine da visualizzare
                    final List<File> immagini = new ArrayList<>();
                    fileManager.ottieniStatoRootExplorer();
                    List<File> filesNellaCartella = fileManager.ls(file.getParentFile());
                    final OrdinatoreFiles ordinatoreFiles = new OrdinatoreFiles(getPrefs());
                    ordinatoreFiles.ottieniStatoMostraNascosti();
                    filesNellaCartella = ordinatoreFiles.ordinaListaFiles(filesNellaCartella);
                    for(File f : filesNellaCartella){
                        if(FileTypes.getTypeForFile(f) == FileTypes.TYPE_IMMAGINE){
                            immagini.add(f);
                        }
                    }
                    if(immagini.isEmpty()){
                        //se non sono state trovate immagini nella cartella (ad esempio la cartella si trova in un percorso non esplorabile)
                        immagini.add(file);
                    }

                    int fileIndex = 0;
                    for(int i=0; i < immagini.size(); i++){
                        if(immagini.get(i).getAbsolutePath().equals(file.getAbsolutePath())){
                            fileIndex = i;
                            break;
                        }
                    }

                    //creo l'adapter da visualizzare nel pager e imposto l'indice dell'immagine da visualizzare
                    pagerAdapter = new ImagePagerAdapter(this, immagini);
                    pager.setAdapter(pagerAdapter);
                    //pager.setCurrentItem(immagini.indexOf(file));
                    pager.setCurrentItem(fileIndex);
                } else {
                    notifyError();
                }
            } else {
                notifyError();
            }
        }
    }


    /**
     * In caso di errore mostra il layout errore
     */
    private void notifyError(){
        layoutErrore.setVisibility(View.VISIBLE);
        pager.setVisibility(View.GONE);
        if(pagerAdapter != null){
            pagerAdapter.getImmagini().clear();
        }
        //immagini.clear();
    }


    /**
     * Nasconde la toolbar dopo un tot di tempo
     */
    public void nascondiToolbarConRitardo(){
        if(toolbar.getVisibility() == View.VISIBLE && !inAttesaDiNascondimento){
            inAttesaDiNascondimento = true;
            new Handler().postDelayed(() -> {
                if(inAttesaDiNascondimento) {
                    toolbar.setVisibility(View.GONE);
                    inAttesaDiNascondimento = false;
                }
            }, RITARDO_NASCONDIMENTO);
        }
    }


    /**
     * Mostra la toolbar temporaneamente
     */
    public void mostraToolbar(){
        toolbar.setVisibility(View.VISIBLE);
        nascondiToolbarConRitardo();
    }


    @Override
    public void onStart(){
        super.onStart();
        if(modalitaPresentazione){
            avviaPresentazione();
        }
    }


    @Override
    public void onStop(){
        super.onStop();
        stopTimerPresentazione();
    }


    @Override
    protected void onDestroy() {
        if(eliminaHandler != null) {
            eliminaHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
    }


    /**
     * Ferma l'eventuale timer della presentazione
     */
    private void stopTimerPresentazione(){
        modalitaPresentazione = false;
        invalidateOptionsMenu();
        if(timer != null){
            timer.cancel();
            timer.purge();
        }
    }


    private void avviaPresentazione(){
        if(pagerAdapter == null) return;
        modalitaPresentazione = true;
        invalidateOptionsMenu();
        if(timer != null){
            timer.cancel();
            timer.purge();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (modalitaPresentazione) {
                    int prossimaDiapositiva = pager.getCurrentItem() + 1;
                    if (prossimaDiapositiva >= pagerAdapter.getCount()) {
                        prossimaDiapositiva = 0;
                    }
                    final int diapositivaDaMostrare = prossimaDiapositiva;
                    runOnUiThread(() -> pager.setCurrentItem(diapositivaDaMostrare));
                }
            }
        }, ritardoPrimaDiapositiva, DURATA_DIAPOSITIVA_PRESENTAZIONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        if(!modalitaPresentazione) {
            getMenuInflater().inflate(R.menu.menu_image_viewer, menu);
            if(widgetManager.isRequestPinAppWidgetSupported()) {
                getMenuInflater().inflate(R.menu.menu_widget_collegamento, menu);
            }
        } else {
            getMenuInflater().inflate(R.menu.menu_presentazione, menu);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(pagerAdapter == null || pagerAdapter.getCount() == 0){
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
            case R.id.media_info:
                final Map<String, String> mapMediaInfo = MediaInfo.getMetadata(this, pagerAdapter.immagineAt(pager.getCurrentItem()));
                final DialogInfoBuilder dialogInfoBuilder = new DialogInfoBuilder(this, R.string.media_info, mapMediaInfo);
                dialogInfoBuilder.create().show();
                return true;
            case R.id.elimina:
                final File fileDaElininare = pagerAdapter.immagineAt(pager.getCurrentItem());
                if(!FileUtils.fileIsInCache(fileDaElininare, this)) {
                    final List<File> filesDaEliminare = new ArrayList<>(1);
                    filesDaEliminare.add(fileDaElininare);
                    fileManager.elimina(filesDaEliminare, eliminaHandler);
                } else {
                    ColoredToast.makeText(this, R.string.impossibile_completare_operazione, Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.ruota_destra:
                pagerAdapter.rotate(pager.getCurrentItem(), 90);
                return true;
            case R.id.ruota_sinistra:
                pagerAdapter.rotate(pager.getCurrentItem(), -90);
                return true;
            case R.id.condividi:
                final List<File> filesDaCondividere = new ArrayList<>(1);
                filesDaCondividere.add(pagerAdapter.immagineAt(pager.getCurrentItem()));
                new FileOpener(this).shareFiles(filesDaCondividere);
                return true;
            case R.id.imposta_sfondo:
                final CustomDialogBuilder builder = new CustomDialogBuilder(this);
                builder.setType(CustomDialogBuilder.TYPE_NORMAL);
                builder.setMessage(R.string.impostare_come_sfondo);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityImageViewer.this);
                        final File imageFile = pagerAdapter.immagineAt(pager.getCurrentItem());
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                final Uri imageUri = FileUtils.uriWithFileProvider(ActivityImageViewer.this, imageFile);
                                final Intent intent = wallpaperManager.getCropAndSetWallpaperIntent(imageUri);
                                startActivityForResult(intent, REQ_ACTIVITY_SET_WALLPAPER);
                            } else {
                                final LoadImageFileTask loadImageFileTask = new LoadImageFileTask(imageFile, new LoadImageFileTask.LoadImageFileListener() {
                                    @Override
                                    public void onLoadImage(Bitmap bitmap, boolean isGif) {
                                        if(bitmap != null){
                                            try {
                                                wallpaperManager.setBitmap(bitmap);
                                                ColoredToast.makeText(ActivityImageViewer.this, R.string.sfondo_impostato, Toast.LENGTH_LONG).show();
                                            } catch (IOException e){
                                                ColoredToast.makeText(ActivityImageViewer.this, R.string.impossibile_visualizzare_immagine, Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            ColoredToast.makeText(ActivityImageViewer.this, R.string.impossibile_visualizzare_immagine, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                                loadImageFileTask.setProgressDialog(ActivityImageViewer.this, getString(R.string.imposta_come_sfondo));
                                loadImageFileTask.execute();
                            }
                        } catch (Exception e) {
                            ColoredToast.makeText(ActivityImageViewer.this, R.string.impossibile_visualizzare_immagine, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
                return true;
            case R.id.presentazione:
                avviaPresentazione();
                return true;
            case R.id.stop:
                stopTimerPresentazione();
                ColoredToast.makeText(this, R.string.presentazione_interrotta, Toast.LENGTH_LONG).show();
                return true;
            case R.id.collegamento_home:
                if( widgetManager.isRequestPinAppWidgetSupported()) {
                    final File imageFile = pagerAdapter.immagineAt(pager.getCurrentItem());
                    widgetManager.addWidgetToHome(imageFile);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQ_ACTIVITY_SET_WALLPAPER && resultCode == RESULT_OK) {
            //sfondo impostato dopo aver chiamato l'intent getCropAndSetWallpaperIntent
            ColoredToast.makeText(ActivityImageViewer.this, R.string.sfondo_impostato, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Azione da eseguire alla pressione del tasto indietro
     */
    @Override
    public void onBackPressed() {
        if(modalitaPresentazione){
            stopTimerPresentazione();
            ColoredToast.makeText(this, R.string.presentazione_interrotta, Toast.LENGTH_LONG).show();
        } else {
            super.onBackPressed();
        }
    }



    @Override
    public void onFileManagerDeleteFinished(boolean success, List<File> deletedFiles) {
        if(success && !deletedFiles.isEmpty()){
            //dopo la rimozione del file, lo rimuovo anche dall'adapter e visualizzo un altro file
            final File deletedFile = deletedFiles.get(0);
            pagerAdapter.remove(deletedFile);
            int currentItem = pager.getCurrentItem();
            if(currentItem >= pagerAdapter.getCount()){
                currentItem = currentItem-1;
            }
            if(currentItem >= 0){
                try {
                    pager.setCurrentItem(currentItem, true);
                } catch (Exception e){
                    e.printStackTrace();
                    notifyError();
                }
            } else {
                notifyError();
            }
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_UP:
                mostraToolbar();
                //do il focus al tasto home della toolbar
                if(toolbar.getChildCount() > 2){
                    toolbar.getChildAt(1).requestFocus();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }


    /**
     * Implementazione vuota per evitare che venga impostato un tema diverso
     */
    @Override
    protected void settaTema() {}
}
