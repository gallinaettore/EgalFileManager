package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import it.Ettore.androidutilsx.ext.ViewsExtKt;
import it.Ettore.androidutilsx.utils.FocusUtils;
import it.Ettore.androidutilsx.utils.ViewUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.ftp.ServerFtp;


/**
 * Dialog per l'immissione dei dati di configurazione di un server FTP
 */
public class DialogDatiServerFtpBuilder {
    private final static String CODIFICA_AUTO = "Auto";
    private final Context context;
    private final DialogInterface.OnClickListener listener;
    private ServerFtp serverFtp;


    /**
     *
     * @param context Context chiamante
     * @param serverFtp Passare null se la dialog serve per creare un nuovo server, Passare un'oggetto ServerFtp se la dialog serve per modificare i dati
     * @param listener Listener eseguito alla chiusura della dialog
     */
    public DialogDatiServerFtpBuilder(@NonNull Context context, ServerFtp serverFtp, @NonNull DialogInterface.OnClickListener listener){
        this.context = context;
        this.listener = listener;
        this.serverFtp = serverFtp;
    }


    /**
     * Crea la dialog
     * @return Dialog creata
     */
    public AlertDialog create() {
        FocusUtils.startMonitoring((Activity)context);
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.hideIcon(true);
        builder.removeTitleSpace(true);
        builder.setTitle(R.string.dati_server);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_dati_server_ftp, null);
        final RadioButton radioButtonFtp = view.findViewById(R.id.radioButton_ftp);
        final RadioButton radioButtonFtps = view.findViewById(R.id.radioButton_ftps);
        final EditText editTextServer = view.findViewById(R.id.edittext_server);
        final EditText editTextPorta = view.findViewById(R.id.edittext_porta);
        final RadioButton radioButtonAttivo = view.findViewById(R.id.radioButton_attivo);
        final RadioButton radioButtonPassivo = view.findViewById(R.id.radioButton_passivo);
        final TextView textViewUsername = view.findViewById(R.id.textview_username);
        final TextView textViewPassword = view.findViewById(R.id.textview_password);
        final EditText editTextUsername = view.findViewById(R.id.edittext_username);
        final EditText editTextPassword = view.findViewById(R.id.edittext_password);
        final CheckBox checkboxAnonimo = view.findViewById(R.id.checkbox_anonimo);
        checkboxAnonimo.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            textViewUsername.setEnabled(!isChecked);
            editTextUsername.setEnabled(!isChecked);
            textViewPassword.setEnabled(!isChecked);
            editTextPassword.setEnabled(!isChecked);
        });
        final Spinner spinnerCodifica = view.findViewById(R.id.spinner_codifica);
        final Map<String, String> mapCodifiche = ServerFtp.getMapCodifiche();
        final List<String> listaNomiCodifiche = new ArrayList<>(mapCodifiche.size()+1);
        final List<String> listaCodifiche = new ArrayList<>(mapCodifiche.size()+1);
        listaNomiCodifiche.add(CODIFICA_AUTO);
        listaCodifiche.add(CODIFICA_AUTO);
        for(Map.Entry<String, String> entry : mapCodifiche.entrySet()){
            listaNomiCodifiche.add(entry.getValue() + " (" + entry.getKey() + ")");
            listaCodifiche.add(entry.getKey());
        }
        ViewsExtKt.popola(spinnerCodifica, listaNomiCodifiche);
        final EditText editTextNomeVisualizzato = view.findViewById(R.id.edittext_nome_visualizzato);

        if (serverFtp != null) {
            if(serverFtp.getTipo() == ServerFtp.TIPO_FTP){
                radioButtonFtp.setChecked(true);
            } else {
                radioButtonFtps.setChecked(true);
            }
            editTextServer.setText(serverFtp.getHost());
            editTextPorta.setText(String.valueOf(serverFtp.getPorta()));
            if (serverFtp.getModalita() == ServerFtp.MODALITA_ATTIVO) {
                radioButtonAttivo.setChecked(true);
            } else {
                radioButtonPassivo.setChecked(true);
            }
            editTextUsername.setText(serverFtp.getUsername());
            editTextPassword.setText(serverFtp.getPassword());
            checkboxAnonimo.setChecked(serverFtp.getUsername() == null && serverFtp.getPassword() == null);
            int posizioneCodifica = listaCodifiche.indexOf(serverFtp.getCodifica());
            if(posizioneCodifica != -1){
                spinnerCodifica.setSelection(posizioneCodifica);
            }
            editTextNomeVisualizzato.setText(serverFtp.getNomeVisualizzato());
        }

        ViewUtils.cursoreAllaFine(editTextServer, editTextPorta, editTextUsername, editTextPassword, editTextNomeVisualizzato);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                ServerFtp serverConVecchioHost = null;
                boolean nuovoServer = false;
                if (serverFtp == null) {
                    serverFtp = new ServerFtp(context, editTextServer.getText().toString());
                    nuovoServer = true;
                }
                if(!serverFtp.getHost().equals(editTextServer.getText().toString().trim())){
                    //se il nome host è diverso
                    serverConVecchioHost = serverFtp;
                    serverFtp = new ServerFtp(context, editTextServer.getText().toString());
                }
                if(radioButtonFtp.isChecked()){
                    serverFtp.setTipo(ServerFtp.TIPO_FTP);
                } else {
                    serverFtp.setTipo(ServerFtp.TIPO_FTPS);
                }
                try {
                    serverFtp.setPorta(Integer.parseInt(editTextPorta.getText().toString()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (radioButtonAttivo.isChecked()) {
                    serverFtp.setModalita(ServerFtp.MODALITA_ATTIVO);
                } else {
                    serverFtp.setModalita(ServerFtp.MODALITA_PASSIVO);
                }
                if(checkboxAnonimo.isChecked()){
                    serverFtp.setUsername(null);
                    serverFtp.setPassword(null);
                } else {
                    serverFtp.setUsername(editTextUsername.getText().toString());
                    serverFtp.setPassword(editTextPassword.getText().toString());
                }
                final String codificaSelezionata = listaCodifiche.get(spinnerCodifica.getSelectedItemPosition());
                if(codificaSelezionata.equals(CODIFICA_AUTO)){
                    serverFtp.setCodifica(null);
                } else {
                    serverFtp.setCodifica(codificaSelezionata);
                }
                serverFtp.setNomeVisualizzato(editTextNomeVisualizzato.getText().toString());

                if(nuovoServer && serverFtp.hostAlreadyExists()){
                    //se il nuovo server inserito corrisponde a un server già esistente
                    CustomDialogBuilder.make(context, context.getString(R.string.server_esistente, serverFtp.getHost()), CustomDialogBuilder.TYPE_ERROR).show();
                } else if (serverConVecchioHost != null){
                    //se è stato cambiato il nome host cancello il vecchio server e salvo il nuovo
                    serverConVecchioHost.removeFromPreferences();
                    serverFtp.saveToPreferences();
                } else if (serverFtp.getHost() != null && !serverFtp.getHost().isEmpty()){
                    //nuovo server da inserire
                    serverFtp.saveToPreferences();
                }

                if (listener != null) {
                    listener.onClick(dialogInterface, which);
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (listener != null) {
                    listener.onClick(dialogInterface, which);
                }
            }
        });

        builder.setView(view);
        return builder.create();
    }

}
