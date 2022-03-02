package it.Ettore.egalfilemanager.ftp;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.Crypt;


/**
 * Classe che gestisce i dati del server FTP
 */
public class ServerFtp implements Comparable<ServerFtp> {
    public static final int MODALITA_PASSIVO = 0;
    public static final int MODALITA_ATTIVO = 1;
    public static final int TIPO_FTP = 0;
    public static final int TIPO_FTPS = 1;

    public static final String PREFS_AUTH_FTP = "auth_ftp";
    private static final String JSONKEY_HOST = "host";
    private static final String JSONKEY_USER = "username";
    private static final String JSONKEY_PASSWORD = "password";
    private static final String JSONKEY_NOME_VISUALIZZATO = "nome_visualizzato";
    private static final String JSONKEY_PORTA = "porta";
    private static final String JSONKEY_MODALITA = "modalita";
    private static final String JSONKEY_TIPO = "tipo";
    private static final String JSONKEY_CODIFICA = "codifica";

    private Crypt crypt;
    private final Context context;
    private final SharedPreferences prefs;
    private final String host;
    private String username;
    private String password;
    private String nomeVisualizzato;
    private String codifica;
    private int porta = 21, tipo = TIPO_FTP, modalita = MODALITA_PASSIVO;


    /**
     *
     * @param context Context chiamante
     * @param host Indirizzo del server
     */
    public ServerFtp(@NonNull Context context, @NonNull String host){
        this.context = context;
        this.host = host;
        this.prefs = context.getSharedPreferences(PREFS_AUTH_FTP, Context.MODE_PRIVATE);
        try {
            this.crypt = new Crypt("h735s07m3sphatAX");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Metodo factory per ottenere un ServerFtp da una stringa Json
     * @param context Context chiamante
     * @param json Stringa json
     * @return ServerFTP. Null in caso di errore.
     */
    public static ServerFtp fromJson(@NonNull Context context, String json){
        try {
            final JSONObject jsonObject = new JSONObject(json);
            final String host = jsonObject.getString(JSONKEY_HOST);
            final ServerFtp serverFtp = new ServerFtp(context, host);
            serverFtp.setTipo(jsonObject.getInt(JSONKEY_TIPO));
            serverFtp.setPorta(jsonObject.getInt(JSONKEY_PORTA));
            if(jsonObject.has(JSONKEY_USER)){
                serverFtp.setUsername(jsonObject.getString(JSONKEY_USER));
            }
            if(jsonObject.has(JSONKEY_PASSWORD)){
                final String pwdLetta = jsonObject.getString(JSONKEY_PASSWORD);
                if(!pwdLetta.isEmpty()) {
                    try {
                        serverFtp.setPassword(serverFtp.crypt.decrypt(pwdLetta));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            serverFtp.setModalita(jsonObject.getInt(JSONKEY_MODALITA));
            if(jsonObject.has(JSONKEY_NOME_VISUALIZZATO)){
                serverFtp.setNomeVisualizzato(jsonObject.getString(JSONKEY_NOME_VISUALIZZATO));
            }
            if(jsonObject.has(JSONKEY_CODIFICA)){
                serverFtp.setCodifica(jsonObject.getString(JSONKEY_CODIFICA));
            }
            return serverFtp;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Ottiene una lista con tutti i server salvati nelle preferences
     * @param context Context chiamante
     * @return Lista con tutti i server salvati nelle preferences
     */
    protected static List<ServerFtp> getAllSavedServers(@NonNull Context context){
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_AUTH_FTP, Context.MODE_PRIVATE);
        final Map<String,?> allEntries = prefs.getAll();
        final List<ServerFtp> listaServer = new ArrayList<>(allEntries.size());
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            final String serverFtpString = (String) entry.getValue();
            final ServerFtp serverFtp = ServerFtp.fromJson(context, serverFtpString);
            if(serverFtp != null) {
                listaServer.add(serverFtp);
            }
        }
        Collections.sort(listaServer);
        return listaServer;
    }


    /**
     * Imposta il tipo di server (FTP, FTPS)
     * @param tipo Una delle costanti TIPO di questa classe
     */
    public void setTipo(int tipo){
        this.tipo = tipo;
    }



    /**
     * Imposta la username
     * @param username Username
     */
    public void setUsername(String username) {
        if(username == null || username.trim().isEmpty()){
            this.username = null;
        } else {
            this.username = username.trim();
        }
    }


    /**
     * Imposta la password
     * @param password Password
     */
    public void setPassword(String password) {
        if(password == null || password.trim().isEmpty()){
            this.password = null;
        } else {
            this.password = password.trim();
        }
    }


    /**
     * Imposta il nome del server (se presente sarà visualizzato al posto dell'indirizzo)
     * @param nomeVisualizzato Nome del server
     */
    public void setNomeVisualizzato(String nomeVisualizzato) {
        if(nomeVisualizzato != null && nomeVisualizzato.trim().isEmpty()){
            this.nomeVisualizzato = null;
        } else {
            this.nomeVisualizzato = nomeVisualizzato.trim();
        }
    }


    /**
     * Imposta la porta del server
     * @param porta Porta ftp
     */
    public void setPorta(int porta) {
        this.porta = porta;
    }


    /**
     * Imposta la modalità attiva o passiva del server
     * @param modalita Una delle costanti MODALITA di questa classe
     */
    public void setModalita(int modalita) {
        this.modalita = modalita;
    }


    /**
     * Imposta la codifica dei caratteri.
     * @param codifica Nome codifica
     */
    public void setCodifica(String codifica){
        this.codifica = codifica;
    }


    /**
     * Restituisce il tipo di server (FTP, FTPS)
     * @return Tipo di server: una delle costanti TIPO di questa classe
     */
    public int getTipo(){
        return this.tipo;
    }


    /**
     * Restituisce l'indirizzo del server
     * @return Indirizzo del server
     */
    public String getHost() {
        return host;
    }


    /**
     * Restituisce la username del server
     * @return Username del server. Null se non è stata impostata o se è ad accesso anonimo
     */
    public String getUsername() {
        return username;
    }


    /**
     * Restituisce la password del server
     * @return Password del server. Null se non è stata impostata o se è ad accesso anonimo
     */
    public String getPassword() {
        return password;
    }


    /**
     * Restituisce il nome del server
     * @return Nome del server. Null se non è stato impostato.
     */
    public String getNomeVisualizzato() {
        return nomeVisualizzato;
    }


    /**
     * Restituisce la porta del server
     * @return Porta
     */
    public int getPorta() {
        return porta;
    }


    /**
     * Restituyisce la modalità attiva o passiva del server
     * @return Una delle costanti MODALITA di questa classe
     */
    public int getModalita() {
        return modalita;
    }


    /**
     * Restituisce la codifica caratteri impostata
     * @return Nome codifica
     */
    public String getCodifica(){
        return this.codifica;
    }


    /**
     * Salva il server nelle preferences
     */
    public void saveToPreferences(){
        if(host != null) {
            prefs.edit().putString(host, toString()).apply();
        }
    }


    /**
     * Rimuove il server dall preferences
     */
    public void removeFromPreferences(){
        if(host != null) {
            prefs.edit().remove(host).apply();
        }
    }


    /**
     * Verifica se nelle preferences esiste già un server con lo stesso indirizzo
     * @return True se nelle preferences esiste già un server con lo stesso indirizzo
     */
    public boolean hostAlreadyExists(){
        final List<ServerFtp> listaServer = getAllSavedServers(context);
        for(ServerFtp server : listaServer){
            if(server.getHost().equals(this.getHost())){
                return true;
            }
        }
        return false;
    }


    /**
     * Restituisce una stringa che rappresenta il Json del server
     * @return Stringa che rappresenta il Json del server
     */
    @Override
    public String toString(){
        final JSONObject json = new JSONObject();
        try {
            json.put(JSONKEY_TIPO, tipo);
            json.put(JSONKEY_HOST, host);
            json.put(JSONKEY_PORTA, porta);
            if(username != null && !username.isEmpty()){
                json.put(JSONKEY_USER, username);
            }
            if(password != null && !password.isEmpty()){
                try{
                    json.put(JSONKEY_PASSWORD, crypt.encrypt(password));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            json.put(JSONKEY_MODALITA, modalita);
            if(nomeVisualizzato != null && !nomeVisualizzato.isEmpty()){
                json.put(JSONKEY_NOME_VISUALIZZATO, nomeVisualizzato);
            }
            if(codifica != null){
                json.put(JSONKEY_CODIFICA, codifica);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }


    /**
     * Compara i server per l'ordinamento. Compara per nome server, se non presente compare per indirizzo
     * @param otherServerFtp Altro server da comparare
     * @return Ordinamento
     */
    @Override
    public int compareTo(@NonNull ServerFtp otherServerFtp) {
        String str1 = null, str2 = null;
        if(getNomeVisualizzato() != null){
            str1 = getNomeVisualizzato();
        } else if (getHost() != null){
            str1 = getHost();
        }
        if(otherServerFtp.getNomeVisualizzato() != null){
            str2 = otherServerFtp.getNomeVisualizzato();
        } else if (otherServerFtp.getHost() != null){
            str2 = otherServerFtp.getHost();
        }
        if(str1 != null && str2 != null){
            return str1.compareTo(str2);
        } else {
            return 0;
        }
    }


    /**
     * Restituisce una map con le codifiche supportate
     * @return Map con le codifiche supportate. Chiave = codifica, Valore = descrizione
     */
    public static Map<String, String> getMapCodifiche(){
        final Map<String, String> map = new LinkedHashMap<>();
        map.put( "UTF-8", "Unicode");
        map.put("ISO-8859-1", "Latin Alphabet No.1");
        map.put("ISO-8859-2", "Latin Alphabet No.2");
        map.put("ISO-8859-3", "Latin Alphabet No.3");
        map.put("ISO-8859-4", "Latin Alphabet No.4");
        map.put("ISO-8859-5", "Latin/Cyrillic Alphabet");
        map.put("ISO-8859-6", "Latin/Arabic Alphabet");
        map.put("ISO-8859-7", "Latin/Greek Alphabet");
        map.put("ISO-8859-8", "Latin/Hebrew Alphabet");
        map.put("ISO-8859-9", "Latin Alphabet No.5");
        map.put("ISO-8859-13", "Latin Alphabet No.7");
        map.put("ISO-8859-15", "Latin Alphabet No.9");
        map.put("KOI8-R", "Russian");
        map.put("KOI8-U", "Ukrainian");

        map.put("windows-1250", "Windows Eastern European");
        map.put("windows-1251", "Windows Cyrillic");
        map.put("windows-1252", "Windows Latin-1");
        map.put("windows-1253", "Windows Greek");
        map.put("windows-1254", "Windows Turkish");
        map.put("windows-1257", "Windows Baltic");
        map.put("windows-31j", "Windows Japanese");
        map.put("windows-1255", "Windows Hebrew");
        map.put("windows-1256", "Windows Arabic");
        map.put("windows-1258", "Windows Vietnamese");

        map.put("GBK", "Simplified Chinese");
        map.put("GB18030", "Simplified Chinese");
        map.put("GB2312", "Simplified Chinese");
        map.put("Big5", "Traditional Chinese");

        map.put("EUC-JP", "Japanese");
        map.put("Shift_JIS", "Japanese");
        map.put("ISO-2022-JP", "Japanese");

        map.put("EUC-KR", "Korean");
        //map.put("ISO-2022-KR", "Korean"); questa codifica manda la connessione in stallo e bisogna riavviare l'app

        map.put("US-ASCII", "Ansi");
        //map.put("UTF-16", "Unicode 16bit"); questa codifica manda la connessione in stallo e bisogna riavviare l'app
        //map.put("UTF-16BE", "Unicode 16bit big-endian");
        //map.put("UTF-16LE", "Unicode 16bit little-endian");
        //map.put("UTF-32", "Unicode 32bit");
        //map.put("UTF-32BE", "Unicode 32bit big-endian");
        //map.put("UTF-32LE", "Unicode 32bit little-endian");

        return map;
    }
}
