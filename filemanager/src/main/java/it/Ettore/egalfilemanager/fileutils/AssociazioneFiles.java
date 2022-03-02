package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.activity.ActivityEditorTesti;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;
import it.Ettore.egalfilemanager.activity.ActivityZipViewer;

/**
 * Classe per gestire l'associazione extenzione / programma per l'apertura
 */
public class AssociazioneFiles {
    public static final String PREFS_ASSOCIAZIONI = "files_associations";
    private static final String JSON_KEY_PACKAGE_NAME = "package_name";
    private static final String JSON_KEY_ACTIVITY_NAME = "activity_name";
    private final SharedPreferences prefs;
    private final Context context;



    /**
     *
     * @param context Context
     */
    public AssociazioneFiles(@NonNull Context context){
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_ASSOCIAZIONI, Context.MODE_PRIVATE);
        //se non sono presenti associazioni vengono impostate quelle predefinite
        if(prefs.getAll().isEmpty()){
            scriviAssociazioniPredefinite();
        }
    }



    /**
     * Associa l'estenzione ad una determinata ActivityInfo (app per aprire il file)
     * @param estenzione Estenzione da associare
     * @param activityInfo ActivityInfo associata
     */
    public void associaEstenzione(String estenzione, ActivityInfo activityInfo){
        if(estenzione == null || activityInfo == null) return;
        prefs.edit().putString(estenzione.toLowerCase(), activityInfoToJson(activityInfo)).apply();
    }



    /**
     * Rimuove l'associazione per quella estenzione
     * @param estenzione Estenzione da cui rimuovere l'associazione
     */
    public void eliminaAssociazione(String estenzione){
        if(estenzione == null) return;
        prefs.edit().remove(estenzione).apply();
    }



    /**
     * Map contenente le associazioni effettuate.
     * @return Map con le associazioni: chiave=estenzione, valore=nomepackage
     */
    public Map<String,String> getMapAssociazioni(){
        final Map<String,?> allEntries = prefs.getAll();
        final Map<String,String> associazioni = new HashMap<>(allEntries.size());
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            final String packageName = getPackageNameAssociato(entry.getKey());
            associazioni.put(entry.getKey(), packageName);
        }
        return associazioni;
    }



    /**
     * Ricava il component name associato ad una determinata estenzione
     * @param estenzione Estenzione file
     * @return Component name associato. Null se l'estenzione non è associata o se non è possibile leggere l'associazione
     */
    public ComponentName getComponentNameAssociato(String estenzione){
        if(estenzione == null) return null;
        final String jsonString = prefs.getString(estenzione.toLowerCase(), null);
        if(jsonString == null){
            //estenzione non associata a nessun programma
            return null;
        } else {
            try {
                final JSONObject json = new JSONObject(jsonString);
                final String nomePackage = json.getString(JSON_KEY_PACKAGE_NAME);
                final String nomeActivity = json.getString(JSON_KEY_ACTIVITY_NAME);
                return new ComponentName(nomePackage, nomeActivity);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }



    /**
     * Ricava il nome del package associato ad una determinata estenzione
     * @param estenzione Estenzione
     * @return Nome del package. Null se l'estenzione non è associata o se non è possibile leggere l'associazione
     */
    private String getPackageNameAssociato(String estenzione){
        if(estenzione == null) return null;
        final String jsonString = prefs.getString(estenzione.toLowerCase(), null);
        if(jsonString == null){
            //estenzione non associata a nessun programma
            return null;
        } else {
            try {
                final JSONObject json = new JSONObject(jsonString);
                return json.getString(JSON_KEY_PACKAGE_NAME);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }



    /**
     * Converte l'activity info in json
     * @param activityInfo ActivityInfo da convertire
     * @return Stringa che rappresenta il json
     */
    private String activityInfoToJson(@NonNull ActivityInfo activityInfo){
        final JSONObject json = new JSONObject();
        try {
            json.put(JSON_KEY_PACKAGE_NAME, activityInfo.applicationInfo.packageName);
            json.put(JSON_KEY_ACTIVITY_NAME, activityInfo.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }


    /**
     * Cancella le associazioni esistenti e scrive sulle preferences le associazioni predefinite
     */
    @SuppressLint("ApplySharedPref")
    public void scriviAssociazioniPredefinite(){
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); //cancello il contenuto (se devo ripristinare le associazioni allo stato iniziale)
        final Map<String, String> associazioniPredefinite = getAssociazioniPredefinite();
        for(String ext : associazioniPredefinite.keySet()){
            final String activityName = associazioniPredefinite.get(ext);
            final String jsonString = fileManagerActivityToJson(activityName);
            editor.putString(ext.toLowerCase(), jsonString);
        }
        editor.commit();
    }


    /**
     * Crea il json che contiene il nome dell'activity (di quest'app)
     * @param activityName Nome dell'activity
     * @return Json che contiene il nome dell'activity
     */
    private String fileManagerActivityToJson(@NonNull String activityName){
        final JSONObject json = new JSONObject();
        try {
            json.put(JSON_KEY_PACKAGE_NAME, context.getPackageName());
            json.put(JSON_KEY_ACTIVITY_NAME, activityName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }


    /**
     * Restituisce una map con le associazioni predefinite. chiave=estenzione, valore=nomeactivity
     * @return Map con le associazioni predefinite
     */
    private Map<String, String> getAssociazioniPredefinite(){
        final Map<String, String> associazioniPredefinite = new LinkedHashMap<>();
        associazioniPredefinite.put("txt", ActivityEditorTesti.class.getName());
        associazioniPredefinite.put("xml", ActivityEditorTesti.class.getName());
        associazioniPredefinite.put("jpg", ActivityImageViewer.class.getName());
        associazioniPredefinite.put("jpeg", ActivityImageViewer.class.getName());
        associazioniPredefinite.put("png", ActivityImageViewer.class.getName());
        associazioniPredefinite.put("gif", ActivityImageViewer.class.getName());
        associazioniPredefinite.put("bmp", ActivityImageViewer.class.getName());
        associazioniPredefinite.put("zip", ActivityZipViewer.class.getName());
        associazioniPredefinite.put("zipx", ActivityZipViewer.class.getName());
        associazioniPredefinite.put("rar", ActivityZipViewer.class.getName());
        associazioniPredefinite.put("jar", ActivityZipViewer.class.getName());
        return associazioniPredefinite;
    }
}
