package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.lan.AutenticazioneLan;
import it.Ettore.egalfilemanager.lan.thread.SmbAuthenticationTask;



/**
 * Dialog per l'inserimento delle credenziali di un server smb (e successiva verifica)
 */
public class DialogAutenticazioneLanBuilder implements SmbAuthenticationTask.AuthenticationTaskListener{
    private final Activity activity;
    private final boolean mostraErrore;
    private final String path;
    private final SmbAuthenticationTask.AuthenticationTaskListener listener;
    private boolean salvaPassword;


    /**
     *
     * @param activity Activity chiamante
     * @param path Path del server smb
     * @param mostraErrore True se si vuole mostrare la dialog di notifica user/password errati. False per mostrare la dialog normale
     * @param listener Listener eseguito al termine del task di verifica delle credenziali
     */
    public DialogAutenticazioneLanBuilder(@NonNull Activity activity, @NonNull String path, boolean mostraErrore, SmbAuthenticationTask.AuthenticationTaskListener listener){
        this.activity = activity;
        this.path = path;
        this.mostraErrore = mostraErrore;
        this.listener = listener;
    }


    /**
     * Creda la dialog di inserimento delle credenziali. Dopo l'inserimento esegue il task di verifica username/password
     * @return
     */
    public AlertDialog create() {
        final CustomDialogBuilder builder = new CustomDialogBuilder(activity);
        if(mostraErrore){
            builder.setType(CustomDialogBuilder.TYPE_ERROR);
        } else {
            builder.setTitle(R.string.autenticazione);
            builder.hideIcon(true);
        }

        final View view = LayoutInflater.from(activity).inflate(R.layout.dialog_autenticazione_lan, null);
        final TextView textViewErrore = view.findViewById(R.id.textview_errore);
        final TextView textViewUsername = view.findViewById(R.id.textview_username);
        final TextView textViewPassword = view.findViewById(R.id.textview_password);
        final EditText editTextUsername = view.findViewById(R.id.edittext_username);
        final EditText editTextPassword = view.findViewById(R.id.edittext_password);
        final CheckBox checkBoxAnonimo = view.findViewById(R.id.checkbox_anonimo);
        final CheckBox checkBoxSalvaPassword = view.findViewById(R.id.checkbox_salva_password);
        builder.setView(view);

        if(mostraErrore){
            textViewErrore.setVisibility(View.VISIBLE);
        }

        checkBoxAnonimo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                textViewUsername.setEnabled(!isChecked);
                editTextUsername.setEnabled(!isChecked);
                textViewPassword.setEnabled(!isChecked);
                editTextPassword.setEnabled(!isChecked);
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String username = null, password = null;
                if(!checkBoxAnonimo.isChecked()){
                    username = editTextUsername.getText().toString();
                    password = editTextPassword.getText().toString();
                }
                if(checkBoxSalvaPassword.isChecked()){
                    salvaPassword = true;
                }
                //avvio il task per vedere se user e password sono corretti
                new SmbAuthenticationTask(activity, path, username, password, DialogAutenticazioneLanBuilder.this).execute();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }


    /**
     * Listener eseguito al termine del task di verifica. Chiama il listener passato al costruttore (dello stesso tipo) usato per notifica all'activity i risultati della verifica
     * @param result Intero che rappresenta il risultato della verifica credenziali. Una delle costanti RESULT di SmbAuthenticationTask
     * @param path Path del server smb
     * @param username Username del server smb. Null se accesso anomino
     * @param password Password del server smb. Null se accesso anonimo
     */
    @Override
    public void onAuthenticationFinished(int result, @NonNull String path, String username, String password) {
        if(salvaPassword && result == SmbAuthenticationTask.RESULT_AUTHENTICATED){
            final AutenticazioneLan autenticazioneLan = new AutenticazioneLan(activity, path, username, password);
            autenticazioneLan.saveToPreferences();
        }
        listener.onAuthenticationFinished(result, path, username, password);
    }

}
