package it.Ettore.materialpreferencesx;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public class CheckedPreference extends GeneralPreference {
    private boolean defaultChecked;
    private String keyPreference;
    private CompoundButton compoundButton;
    private SharedPreferences prefs;
    private CheckedPreferenceListener listener;

    public CheckedPreference(Context context, String title, String keyPreference) {
        super(context);
        init(title, null, keyPreference);
    }


    public CheckedPreference(Context context, String title, SharedPreferences prefs, String keyPreference) {
        super(context);
        init(title, prefs, keyPreference);
    }


    public CheckedPreference(Context context, int resIdTitle, String keyPreference) {
        this(context, context.getString(resIdTitle), keyPreference);
    }


    public CheckedPreference(Context context, int resIdTitle, SharedPreferences prefs, String keyPreference) {
        this(context, context.getString(resIdTitle), prefs, keyPreference);
    }


    protected void init(String title, SharedPreferences prefs, String keyPreference) {
        inflate(getContext(), R.layout.checked_preference, this);
        setCompoundButton((CheckBox)findViewById(R.id.checkBox));
        setTitleTextView((TextView) findViewById(R.id.titleTextView));
        setTitle(title);
        setSummaryTextView((TextView) findViewById(R.id.summaryTextView));
        nascondiSummary(true);
        setIconImageView((ImageView) findViewById(R.id.iconaImageView));
        setKeyPreference(keyPreference);
        compoundButton.setOnClickListener(onclickListener);
        setPrefs(prefs);
    }


    protected void setCompoundButton(CompoundButton compoundButton){
        this.compoundButton = compoundButton;
    }


    protected void setPrefs(SharedPreferences prefs){
        if (prefs == null){
            this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        } else {
            this.prefs = prefs;
        }
    }


    public void setCheckedPreferenceListener(CheckedPreferenceListener listener) {
        this.listener = listener;
    }

    public String getKeyPreference() {
        return keyPreference;
    }


    protected void setKeyPreference(String keyPreference) {
        this.keyPreference = keyPreference;
    }


    public void setDefaultChecked(boolean defaultChecked) {
        this.defaultChecked = defaultChecked;
    }


    public void changeChecked(boolean checked){
        compoundButton.setChecked(checked);
        savePreferences();
    }


    @Override
    protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        final boolean statoSalvato = prefs.getBoolean(keyPreference, defaultChecked);
        //Uso l'handler perchè se viene usato all'interno di un fragment quando si visualizza nuovamente il fragment
        // (in seguito al tasto back) i valori non sarebbero aggiornati correttamente
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                compoundButton.setChecked(statoSalvato);
            }
        });
    }



    protected final View.OnClickListener onclickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //Salvo le preferenze
            savePreferences();
            if(listener != null){
                listener.onCheckedPreference(compoundButton.isChecked());
            }
        }
    };


    private void savePreferences(){
        prefs.edit().putBoolean(keyPreference, compoundButton.isChecked()).apply();
    }



    public interface CheckedPreferenceListener{
        void onCheckedPreference(boolean isChecked);
    }
}
