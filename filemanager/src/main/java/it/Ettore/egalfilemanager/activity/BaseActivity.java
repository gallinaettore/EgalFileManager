package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import it.Ettore.androidutilsx.lang.LanguageManager;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.ui.MyActivity;
import it.Ettore.egalfilemanager.Lingue;
import it.Ettore.egalfilemanager.PermissionsManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.view.ViewUtils;


/**
 * Activity generale
 */
public abstract class BaseActivity extends MyActivity {
    private final static int REQ_CODE_OPEN_DOCUMENT_TREE = 55;
    private File extSdToSaveUri;
    private PermissionsManager permissionsManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getOverflowMenu();
        settaTema();
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        final LanguageManager languageManager = new LanguageManager(newBase, Lingue.getValues());
        super.attachBaseContext(languageManager.changeLocaleFromPreferences());
    }


    /**
     * Mostra la visualizzazione Document Tree per la scelta dello storage esterno in modo da ottenere l'uri
     * @param file File salvato sullo storage esterno di cui si richiede l'uri
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void chiediTreeUriSdEsterna(final File file){
        if(file == null) return;
        final StoragesUtils storagesUtils = new StoragesUtils(this);
        final File extSdToSaveUri = storagesUtils.getExtStorageForFile(file);
        if(extSdToSaveUri == null) return;
        final CustomDialogBuilder builder = new CustomDialogBuilder(this);
        builder.setType(CustomDialogBuilder.TYPE_WARNING);
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_permessi_extsd, null);
        builder.setView(view);
        final TextView messageTextView = view.findViewById(R.id.textview_messaggio);
        String message = getString(R.string.autorizzazione_memorie_esterne);
        String labelStorage = storagesUtils.getVolumeLabel(extSdToSaveUri);
        if(labelStorage != null){
            labelStorage = "\"" + labelStorage + "\"";
            message += ("\n" + getString(R.string.seleziona_label_storage, labelStorage));
        }
        messageTextView.setText(message);
        final ImageView imageView = view.findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.scelta_extsd);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BaseActivity.this.extSdToSaveUri = extSdToSaveUri;
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                //intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                try {
                    startActivityForResult(intent, REQ_CODE_OPEN_DOCUMENT_TREE);
                    //Su Android 7.1.2 causa ActivityNotFoundException, non sarà quindi possibile utilizzare il tree uri
                } catch (ActivityNotFoundException e){
                    ColoredToast.makeText(BaseActivity.this, R.string.impossibile_completare_operazione, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CustomDialogBuilder.make(BaseActivity.this, R.string.nessuna_autorizzazione_esterna, CustomDialogBuilder.TYPE_WARNING).show();
            }
        });
        builder.create().show();
    }


    @Override
    @SuppressLint("NewApi")
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode == REQ_CODE_OPEN_DOCUMENT_TREE) {
            if (resultCode == RESULT_OK) {
                final Uri treeUri = resultData.getData();

                if(SAFUtils.treeUriIsValid(treeUri, extSdToSaveUri, this)){
                    SAFUtils.writeUriToPreferences(this, extSdToSaveUri, treeUri);

                    // Persist access permissions.
                    if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        // noinspection ResourceType
                        getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                    }
                } else {
                    //uri non corretto (è stata selezionata una sotto cartella della scheda sd
                    final File fileCopy = extSdToSaveUri;
                    final CustomDialogBuilder customDialogBuilder = new CustomDialogBuilder(this);
                    customDialogBuilder.setType(CustomDialogBuilder.TYPE_ERROR);
                    customDialogBuilder.setMessage(R.string.tree_uri_extsd_non_valido);
                    customDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            chiediTreeUriSdEsterna(fileCopy); //passo una copia del file perchè in seguito l'originale viene impostato a null
                        }
                    });
                    customDialogBuilder.create().show();
                }
            }
            extSdToSaveUri = null;
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }


    /**
     * Gestione generale del menu
     * @param item Menu item
     * @return .
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Setta il titolo nell'action bar
     * @param title Titolo
     */
    public void setActionBarTitle(String title){
        getSupportActionBar().setTitle(title);
    }


    /**
     * Setta il titolo nell'action bar
     * @param resIdTitle Risorsa del titolo
     */
    public void setActionBarTitle(@StringRes int resIdTitle){
        getSupportActionBar().setTitle(resIdTitle);
    }


    /**
     * Gestore dei permessi storage
     * @return permission manager creato
     */
    public PermissionsManager getPermissionsManager(){
        if(this.permissionsManager == null){
            permissionsManager = new PermissionsManager(this);
        }
        return this.permissionsManager;
    }


    protected void settaTema(){
        setTheme(ViewUtils.getResIdTheme(this));
    }
}
