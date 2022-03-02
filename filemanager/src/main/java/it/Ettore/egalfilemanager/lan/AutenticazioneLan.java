package it.Ettore.egalfilemanager.lan;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.Crypt;


/**
 * Classe per il salvataggio dei dati di autenticazione di un server smb
 */
public class AutenticazioneLan {
    public static final String PREFS_AUTH_SMB = "auth_smb";
    private static final String JSON_USERNAME = "username";
    private static final String JSON_PASSWORD = "password";
    private final String path;
    private String username, password;
    private Crypt crypt;
    private final SharedPreferences prefs;


    /**
     * Restituisce un oggetto AutenticazioneLan salvato nelle preferences
     * @param context Context
     * @param path Path del server smb
     * @return AutenticazioneLan. Null se il path non è presente nelle preferences
     */
    public static AutenticazioneLan fromPreferences(@NonNull Context context, String path){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_AUTH_SMB, Context.MODE_PRIVATE);
        final String jsonString = prefs.getString(path, null);
        if(jsonString == null){
            return null;
        } else {
            return AutenticazioneLan.fromJson(context, path, jsonString);
        }
    }


    /**
     *
     * @param context Context
     * @param path Path del server smb
     * @param username Username
     * @param password Password
     */
    public AutenticazioneLan(@NonNull Context context, @NonNull String path, String username, String password){
        this.prefs = context.getSharedPreferences(PREFS_AUTH_SMB, Context.MODE_PRIVATE);
        this.path = path;
        setUsername(username);
        setPassword(password);
        try {
            this.crypt = new Crypt("h735s07m3sphatAX");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param context Context
     * @param path Path del server smb
     */
    private AutenticazioneLan(@NonNull Context context, @NonNull String path){
        this(context, path, null, null);
    }


    /**
     * Imposta la username
     * @param username Username
     */
    public void setUsername(String username) {
        if(username != null && username.isEmpty()){
            this.username = null;
        } else {
            this.username = username;
        }
    }


    /**
     * Imposta la password
     * @param password Password
     */
    public void setPassword(String password) {
        if(password != null && password.isEmpty()){
            this.password = null;
        } else {
            this.password = password;
        }
    }


    /**
     * Restituisce il path del server smb impostato
     * @return Path del server smb
     */
    public String getPath() {
        return path;
    }


    /**
     * Restituisce la username impostata
     * @return Username. Null se non è impostata nessuna username o se il server smb ha un accesso anonimo.
     */
    public String getUsername() {
        if(username != null && username.isEmpty()){
            return null;
        } else {
            return username;
        }
    }


    /**
     * Restituisce la password del server smb
     * @return Password. Null se non è impostata nessuna password o se il server smb ha un accesso anonimo.
     */
    public String getPassword() {
        if(password != null && password.isEmpty()){
            return null;
        } else {
            return password;
        }
    }


    /**
     * Salva le credenziali nelle preferences
     */
    public void saveToPreferences(){
        prefs.edit().putString(path, toJson()).apply();
    }


    /**
     * Restituisce la stringa che rappresenta un oggetto Json con i dati dell'oggetto corrente
     * @return Stringa Json
     */
    private String toJson(){
        final JSONObject jsonObject = new JSONObject();
        try {
            if(username != null){
                jsonObject.put(JSON_USERNAME, username);
            } else {
                jsonObject.put(JSON_USERNAME, "");
            }
            if(password != null){
                try{
                    jsonObject.put(JSON_PASSWORD, crypt.encrypt(password));
                } catch (Exception e){
                    e.printStackTrace();
                    jsonObject.put(JSON_PASSWORD, "");
                }
            } else {
                jsonObject.put(JSON_PASSWORD, "");
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Crea un oggetto AutenticazioneLan da una stringa Json
     * @param context Context
     * @param path Path del server smb usato come chiave della preference
     * @param json Strimga che rappresenta l'oggetto Json
     * @return AutenticazioneLan. Null se avviene un'errore durante la creazione dell'oggetto
     */
    private static AutenticazioneLan fromJson(@NonNull Context context, @NonNull String path, String json){
        try {
            final JSONObject jsonObject = new JSONObject(json);
            final AutenticazioneLan autenticazioneLan = new AutenticazioneLan(context, path);
            autenticazioneLan.setUsername(jsonObject.getString(JSON_USERNAME));
            final String pwdLetta = jsonObject.getString(JSON_PASSWORD);
            if(pwdLetta != null && !pwdLetta.isEmpty()) {
                try {
                    autenticazioneLan.setPassword(autenticazioneLan.crypt.decrypt(pwdLetta));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return autenticazioneLan;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
