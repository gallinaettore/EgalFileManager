package it.Ettore.egalfilemanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;


/**
 * Classe per la gestione dei permessi lettura/scrittura
 */
public class PermissionsManager {
    public final static int REQ_PERMISSION_WRITE_EXTERNAL = 55;
    private final Activity activity;
    private int negazionePermessi;


    /**
     *
     * @param activity Activity chiamante
     */
    public PermissionsManager(@NonNull Activity activity){
        this.activity = activity;
    }


    /**
     * Verifica se il dispositivo ha i permessi di lettura e scrittura
     * @return True se ha entrambi i permessi
     */
    public boolean hasPermissions(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck1 = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            int permissionCheck2 = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return permissionCheck1 == PackageManager.PERMISSION_GRANTED && permissionCheck2 == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }


    /**
     * Richiede i permessi
     */
    public void requestPermissions(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // For example, if the request has been denied previously.
                final CustomDialogBuilder builder = new CustomDialogBuilder(activity);
                builder.setType(CustomDialogBuilder.TYPE_WARNING);
                builder.setMessage(R.string.permesso_lettura_scrittura);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @SuppressLint("InlinedApi")
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQ_PERMISSION_WRITE_EXTERNAL);
                    }

                });
                builder.create().show();
            } else {
                // permissions have not been granted yet. Request them directly.
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSION_WRITE_EXTERNAL);

            }
        }
    }


    /**
     * Gestione dei permessi non garantiti
     */
    public void manageNotGuaranteedPermissions(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //L'utente ha negato i permessi nella finestra di dialogo, li chiedo continuamente per 2 volte
            //Anche se è una pratica che può sembrare scomoda, su alcuni Huawei la dialog di richiesta viene chiusa alla prima visualizzazione e quindi cos' mi assicuro di mostrarla nuovamente
            negazionePermessi++;
            if(negazionePermessi < 2){
                requestPermissions();
            } else {
                ColoredToast.makeText(activity, R.string.permesso_lettura_scrittura, Toast.LENGTH_LONG).show();
            }
        } else {
            //Se non sono stati concessi i permessi (check box nega sempre), tramite la dialog apro le impostazioni sull'applicazione per concederli manualmente
            final CustomDialogBuilder builder = new CustomDialogBuilder(activity);
            builder.setType(CustomDialogBuilder.TYPE_WARNING);
            builder.setMessage(String.format("%s.\n%s", activity.getString(R.string.permesso_lettura_scrittura), activity.getString(R.string.permessi_scheda_autorizzazioni)));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    final Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    final Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    try {
                        activity.startActivity(intent);
                    } catch (Exception ignored){}
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        }
    }
}
