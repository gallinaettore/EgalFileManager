package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import it.Ettore.androidutilsx.ext.VarieExtKt;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.R;


/**
 * Builder per la Dialog che gestisce l'ordinamento degli albums
 */
public class DialogPermessiFileRootBuilder {
    private final Context context;
    private final String permessiLetti;
    private final DialogPermessiFileRootListener listener;



    public DialogPermessiFileRootBuilder(@NonNull Context context, String permessiLetti, DialogPermessiFileRootListener listener){
        this.context = context;
        this.permessiLetti = permessiLetti;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return AlertDialog. Null le striga passata nel costruttore non è valida
     */
    public AlertDialog create(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.setTitle(VarieExtKt.togli2punti(context.getString(R.string.permessi)));
        builder.hideIcon(true);

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_permessi_file_root, null);
        final CheckBox checkBoxProprietarioLettura = view.findViewById(R.id.checkBox_proprietario_lettura);
        final CheckBox checkBoxProprietarioScrittura = view.findViewById(R.id.checkBox_proprietario_scrittura);
        final CheckBox checkBoxProprietarioEsecuzione = view.findViewById(R.id.checkBox_proprietario_esecuzione);
        final CheckBox checkBoxGruppoLettura = view.findViewById(R.id.checkBox_gruppo_lettura);
        final CheckBox checkBoxGruppoScrittura = view.findViewById(R.id.checkBox_gruppo_scrittura);
        final CheckBox checkBoxGruppoEsecuzione = view.findViewById(R.id.checkBox_gruppo_esecuzione);
        final CheckBox checkBoxAltroLettura = view.findViewById(R.id.checkBox_altro_lettura);
        final CheckBox checkBoxAltroScrittura = view.findViewById(R.id.checkBox_altro_scrittura);
        final CheckBox checkBoxAltroEsecuzione = view.findViewById(R.id.checkBox_altro_esecuzione);

        String[] split = null;
        if(permessiLetti != null){
            split = permessiLetti.split(" ");
        }
        if(split != null && split.length == 3){
            impostaCheckbox(split[0], checkBoxProprietarioLettura, checkBoxProprietarioScrittura, checkBoxProprietarioEsecuzione);
            impostaCheckbox(split[1], checkBoxGruppoLettura, checkBoxGruppoScrittura, checkBoxGruppoEsecuzione);
            impostaCheckbox(split[2], checkBoxAltroLettura, checkBoxAltroScrittura, checkBoxAltroEsecuzione);
        } else {
            return null;
        }

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(listener != null){
                    final String proprietario = octalPermissionRepresentation(checkBoxProprietarioLettura, checkBoxProprietarioScrittura, checkBoxProprietarioEsecuzione);
                    final String gruppo = octalPermissionRepresentation(checkBoxGruppoLettura, checkBoxGruppoScrittura, checkBoxGruppoEsecuzione);
                    final String altro = octalPermissionRepresentation(checkBoxAltroLettura, checkBoxAltroScrittura, checkBoxAltroEsecuzione);
                    final String permessi = proprietario + gruppo + altro;
                    listener.onPositiveButtonClicked(permessi);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }


    private void impostaCheckbox(String sottostringa, CheckBox checkBoxLettura, CheckBox checkBoxScrittura, CheckBox checkBoxEsecuzione){
        if(sottostringa != null && sottostringa.length() == 3){
            checkBoxLettura.setChecked(sottostringa.substring(0, 1).equalsIgnoreCase("r"));
            checkBoxScrittura.setChecked(sottostringa.substring(1, 2).equalsIgnoreCase("w"));
            checkBoxEsecuzione.setChecked(sottostringa.substring(2, 3).equalsIgnoreCase("x"));
        }
    }


    private String octalPermissionRepresentation(CheckBox checkBoxLettura, CheckBox checkBoxScrittura, CheckBox checkBoxEsecuzione){
        int ottale = 0;
        if(checkBoxEsecuzione.isChecked()){
            ottale += 1;
        }
        if(checkBoxScrittura.isChecked()){
            ottale += 2;
        }
        if(checkBoxLettura.isChecked()){
            ottale += 4;
        }
        return String.valueOf(ottale);
    }


    public interface DialogPermessiFileRootListener {
        void onPositiveButtonClicked(String newPermissions);
    }
}
