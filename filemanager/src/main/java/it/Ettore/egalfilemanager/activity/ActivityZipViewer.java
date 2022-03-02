package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.PermissionsManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.UriUtils;
import it.Ettore.egalfilemanager.fragment.FragmentZipExplorer;


/**
 * Activity per la visualizzazione ed estrazione degli archivi compressi
 */
public class ActivityZipViewer extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getPermissionsManager().hasPermissions()) {
            gestisciZipDaIntent(getIntent());
        } else {
            getPermissionsManager().requestPermissions();
        }
    }


    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if(getPermissionsManager().hasPermissions()) {
            gestisciZipDaIntent(intent);
        } else {
            getPermissionsManager().requestPermissions();
        }
    }


    /**
     * Gestione del tasto Back.
     * Il primo fragment iniziale è sempre vuoto, ritornando indietro col tasto back viene mostrata la pagina bianca
     * quando rimane l'ultimo fragment prima di mostrare il fragment vuoto chiudo l'activity
     */
    @Override
    public void onBackPressed() {
        int fragments = getSupportFragmentManager().getBackStackEntryCount();
        if (fragments == 1) {
            finish();
        } else {
            if (getFragmentManager().getBackStackEntryCount() > 1) {
                getFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }


    /**
     * Mostra il fragment dopo aver ricevuto l'uri dell'archivio
     * @param intent Intent ricevuto
     */
    private void gestisciZipDaIntent(Intent intent){
        final String type = intent.getType();
        if ("application/zip".equals(type) || "x-zip-compressed".equals(type) || "application/rar".equals(type) || "application/x-rar-compressed".equals(type) || "application/java-archive".equals(type)) {
            final Uri zipUri = intent.getData();
            final File zipFile = UriUtils.uriToFile(this, zipUri);
            if(zipFile != null) {
                showFragment(FragmentZipExplorer.getInstance(zipFile, null));
            }
        }
    }


    /**
     * Cambia fragment
     * @param fragment Nuovo fragment
     */
    public void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).addToBackStack(Costanti.ZIP_BACKSTACK).commitAllowingStateLoss(); //effettua il commit dolo che lo stato dell'activity è salvato
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
                    gestisciZipDaIntent(getIntent());
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
