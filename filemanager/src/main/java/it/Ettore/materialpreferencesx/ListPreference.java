package it.Ettore.materialpreferencesx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;

import java.util.List;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public class ListPreference extends Preference implements View.OnClickListener {
    private String[] entries;
    private Object[] entryValues;
    private String keyPreference;
    private int defaultIndex = -1;
    private Object valoreDaSelezionare;
    private OnPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences prefs;
    private boolean updateSummaryOnPreferenceChange = false;



    public ListPreference(Context context, String title, String keyPreference) {
        super(context, title);
        init(null, keyPreference);
    }


    public ListPreference(Context context, String title, SharedPreferences prefs, String keyPreference) {
        super(context, title);
        init(prefs, keyPreference);
    }


    public ListPreference(Context context, int resIdTitle, String keyPreference) {
        super(context, resIdTitle);
        init(null, keyPreference);
    }


    public ListPreference(Context context, int resIdTitle, SharedPreferences prefs, String keyPreference) {
        super(context, resIdTitle);
        init(prefs, keyPreference);
    }


    private void init(SharedPreferences prefs, String keyPreference) {
        setKeyPreference(keyPreference);
        setOnClickListener(this);
        if (prefs == null){
            this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        } else {
            this.prefs = prefs;
        }
    }


    public String[] getEntries() {
        return entries;
    }


    /**
     * Imposta i valori da visulizzare nella lista.
     * @param entries Array contenente i valori da visualizzare nella lista
     */
    public void setEntries(String[] entries) {
        this.entries = entries;
    }


    /**
     * Imposta i valori da visulizzare nella lista.
     * @param entries Array contenente i valori da visualizzare nella lista
     */
    public void setEntries(List<String> entries) {
        this.entries = entries.toArray(new String[0]);
    }


    public Object[] getEntryValues() {
        return entryValues;
    }



    /**
     * Imposta i valori che saranno salvati nella preferences.
     * Se non vengono impostati questi valori vengono utilizzati quelli passati come entries
     * @param entryValues Array con i valori da salvare nelle preferences
     */
    public void setEntryValues(Object[] entryValues) {
        this.entryValues = entryValues;
    }


    public void setEntryValues(List<?> entryValues){
        this.entryValues = entryValues.toArray(new Object[0]);
    }



    public String getKeyPreference() {
        return keyPreference;
    }


    public void setKeyPreference(String keyPreference) {
        this.keyPreference = keyPreference;
    }


    public int getDefaultIndex() {
        return defaultIndex;
    }


    public void setDefaultIndex(int defaultIndex) {
        this.defaultIndex = defaultIndex;
    }


    public void setValue(Object value){
        valoreDaSelezionare = value;
    }


    public Object getSettedValue(){
        try {
            if (entryValues != null && entryValues.length > 0) {
                final Object primoValore = entryValues[0];
                if(prefs.contains(keyPreference)) {
                    if (primoValore instanceof Integer) {
                        return prefs.getInt(keyPreference, 0);
                    } else if (primoValore instanceof Float || primoValore instanceof Double) {
                        return prefs.getFloat(keyPreference, 0.0f);
                    } else if (primoValore instanceof Long) {
                        return prefs.getLong(keyPreference, 0L);
                    } else if (primoValore instanceof Boolean) {
                        return prefs.getBoolean(keyPreference, false);
                    }
                } else {
                    return null;
                }
            }
            return prefs.getString(keyPreference, null);
        } catch (ClassCastException e){
            return null;
        }
    }


    public void setPreferenceChangeListener(OnPreferenceChangeListener preferenceChangeListener) {
        this.preferenceChangeListener = preferenceChangeListener;
    }


    public void showSettedValueInSummary(){
        if(entries == null || entries.length == 0){
            Log.w("ListPreference", "La list preference non contiene entries validi");
            return;
        }

        int indice;
        //Se è stato impostato un valore da selezionare
        if(valoreDaSelezionare != null) {
            indice = indiceDaValore(valoreDaSelezionare);
        } else {
            //Leggo dalle preferenze
            final Object valoreSalvato = getSettedValue();
            indice = indiceDaValore(valoreSalvato);
        }

        //Se non è stato settato un valore e nessun valore è stato ancora salvato nelle preferenze imposto indice come defaultIndex
        if(indice == -1 ){
            indice = defaultIndex;
        }

        if(indice != -1 && indice < entries.length){
            setSummary(entries[indice]);
        } else {
            setSummary(null);
        }

        this.updateSummaryOnPreferenceChange = true;
    }




    @Override
    public void onClick(View v) {
        if(entryValues == null && entries != null){
            entryValues = entries;
        }
        if(entries == null || keyPreference == null || entries.length != entryValues.length){
            Log.w("ListPreference", "Errore nella creazione della dialog");
        }

        int checkedIndex;

        //Se è stato impostato un valore da selezionare
        if(valoreDaSelezionare != null) {
            checkedIndex = indiceDaValore(valoreDaSelezionare);
        } else {
            //Leggo dalle preferenze
            final Object valoreSalvato = getSettedValue();
            checkedIndex = indiceDaValore(valoreSalvato);
        }

        //Se non è stato settato un valore e nessun valore è stato ancora salvato nelle preferenze imposto indice come defaultIndex
        if(checkedIndex == -1 ){
            checkedIndex = defaultIndex;
        }

        //Creo la dialog
        createDialog(checkedIndex).show();
    }


    private int indiceDaValore(Object valore){
        int indice = -1;
        final Object[] arrayDoveCercare;
        if(entryValues != null){
            arrayDoveCercare = entryValues;
        } else {
            arrayDoveCercare = entries;
        }
        for (int i = 0; i < arrayDoveCercare.length; i++) {
            if (arrayDoveCercare[i].equals(valore)) {
                indice = i;
                break;
            }
        }
        return indice;
    }


    /**
     * Crea la dialog da visualizzare. Fare override di questo metodo per usare dialog custom.
     * @param checkedIndex Indice dell'elemento selezionato
     * @return Dialog custom
     */
    public AlertDialog createDialog(int checkedIndex){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getTitle());
        builder.setSingleChoiceItems(entries, checkedIndex, listPreferenceClickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }


    public DialogInterface.OnClickListener listPreferenceClickListener = new DialogInterface.OnClickListener() {
        @SuppressLint("ApplySharedPref")
        public void onClick(DialogInterface dialog, int which) {
            //Salvo le preferenze
            final SharedPreferences.Editor editor = prefs.edit();
            final Object valoreDaSalvare = entryValues[which];
            if (valoreDaSalvare instanceof Integer) {
                editor.putInt(keyPreference, (Integer) valoreDaSalvare);
            } else if (valoreDaSalvare instanceof Float || valoreDaSalvare instanceof Double) {
                editor.putFloat(keyPreference, (Float) valoreDaSalvare);
            } else if (valoreDaSalvare instanceof Long) {
                editor.putLong(keyPreference, (Long) valoreDaSalvare);
            } else if (valoreDaSalvare instanceof Boolean){
                editor.putBoolean(keyPreference, (Boolean) valoreDaSalvare);
            } else {
                editor.putString(keyPreference, valoreDaSalvare.toString());
            }
            editor.commit(); //se uso apply() e dopo riavvio l'app potrei avere problemi

            if(updateSummaryOnPreferenceChange){
                showSettedValueInSummary();
            }

            //Listener al cambiamento delle preferenze
            if(preferenceChangeListener != null){
                preferenceChangeListener.onPreferenceChange(ListPreference.this, valoreDaSalvare);
            }

            dialog.dismiss();
        }
    };
}
