package it.Ettore.egalfilemanager.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.PermissionsManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.copyutils.CopyHandler;
import it.Ettore.egalfilemanager.copyutils.CopyHandlerListener;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.thread.CopiaSingoloFileHandler;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.UriUtils;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Activity per il salvataggio dei files
 */
public class ActivitySalvaFile extends BaseActivity implements CopyHandlerListener {
    private List<File> listaFiles;
    private CopyHandler copyHandler;
    private CopiaSingoloFileHandler copiaSingoloFileHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        copyHandler = new CopyHandler(this, this);
        copiaSingoloFileHandler = new CopiaSingoloFileHandler(this, this);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        listaFiles = new ArrayList<>();

        //IMPORTANTE: vengono passati solo gli uri dei files e non le cartelle!
        if (Intent.ACTION_SEND.equals(action)) {
            final Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            final File file = UriUtils.uriToFile(this, fileUri);
            if(file != null) {
                listaFiles.add(file);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            final List<Uri> listaFileUri = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if(listaFileUri != null) {
                for (Uri uri : listaFileUri) {
                    final File file = UriUtils.uriToFile(this, uri);
                    if (file != null) {
                        listaFiles.add(file);
                    }
                }
            }
        } else {
            ColoredToast.makeText(this, R.string.errore_salvataggio, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(listaFiles.isEmpty()){
            ColoredToast.makeText(this, R.string.errore_salvataggio, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(getPermissionsManager().hasPermissions()) {
            mostraDialogStorage();
        } else {
            getPermissionsManager().requestPermissions();
        }
    }



    /**
     * Mostra la dialog per la scelta dello storage
     */
    private void mostraDialogStorage(){
        final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(this);
        builder.setTitle(R.string.seleziona_destinazione);
        builder.hideIcon(true);
        builder.setStorageItems(new HomeNavigationManager(this).listaItemsArchivioLocale());
        builder.setCancelable(false);
        builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
            @Override
            public void onSelectStorage(File storagePath) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                final FileManager fileManager = new FileManager(ActivitySalvaFile.this);
                fileManager.ottieniStatoRootExplorer();
                int dialogType = listaFiles.size() == 1 ? DialogFileChooserBuilder.TYPE_SAVE_FILE : DialogFileChooserBuilder.TYPE_SELECT_FILE_FOLDER;
                final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(ActivitySalvaFile.this, dialogType);
                fileChooser.setTitle(R.string.seleziona_destinazione);
                fileChooser.setCancelable(false);
                fileChooser.setStartFolder(storagePath);
                if(listaFiles.size() == 1){
                    fileChooser.setFileName(listaFiles.get(0).getName());
                }
                fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                    @Override
                    public void onFileChooserSelected(final File selected) {
                        if(listaFiles.size() == 1){
                            fileManager.copiaSingoloFile(listaFiles.get(0), selected, copiaSingoloFileHandler);
                        } else {
                            fileManager.copia(listaFiles, selected, copyHandler);
                        }
                    }

                    @Override
                    public void onFileChooserCanceled() {
                        finish();
                    }
                });
                fileChooser.create().show();

                //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                new ChiediTreeUriTask(ActivitySalvaFile.this, storagePath, true).execute();
            }

            @Override
            public void onCancelStorageSelection() {
                finish();
            }
        });
        builder.showSelectDialogIfNecessary();
    }


    @Override
    public void onDestroy(){
        if(copyHandler != null) {
            copyHandler.dismissProgressDialogOnDestroy(); //chiudo (se visibile) la copy dialog per evitare errori activity leak
        }
        if(copiaSingoloFileHandler != null) {
            copiaSingoloFileHandler.dismissProgressDialogOnDestroy();
        }
        super.onDestroy();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionsManager.REQ_PERMISSION_WRITE_EXTERNAL:
                // If request is cancelled, the result arrays are empty.
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    //permessi non garantiti
                    getPermissionsManager().manageNotGuaranteedPermissions();
                } else {
                    //permessi garantiti
                    mostraDialogStorage();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onCopyServiceFinished(boolean success, String destinationPath, List<String> filesCopiati, int tipoCopia) {
        finish();
    }
}
