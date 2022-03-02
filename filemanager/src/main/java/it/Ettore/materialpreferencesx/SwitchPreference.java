package it.Ettore.materialpreferencesx;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import it.Ettore.egalfilemanager.R;

public class SwitchPreference extends CheckedPreference {

    public SwitchPreference(Context context, String title, String keyPreference) {
        super(context, title, keyPreference);
    }

    public SwitchPreference(Context context, String title, SharedPreferences prefs, String keyPreference) {
        super(context, title, prefs, keyPreference);
    }

    public SwitchPreference(Context context, int resIdTitle, String keyPreference) {
        super(context, resIdTitle, keyPreference);
    }

    public SwitchPreference(Context context, int resIdTitle, SharedPreferences prefs, String keyPreference) {
        super(context, resIdTitle, prefs, keyPreference);
    }


    @Override
    protected void init(String title, SharedPreferences prefs, String keyPreference) {
        inflate(getContext(), R.layout.switch_preference, this);
        final Switch switchbutton = findViewById(R.id.switchbutton);
        setCompoundButton(switchbutton);
        setTitleTextView((TextView) findViewById(R.id.titleTextView));
        setTitle(title);
        setSummaryTextView((TextView) findViewById(R.id.summaryTextView));
        nascondiSummary(true);
        setIconImageView((ImageView) findViewById(R.id.iconaImageView));
        setKeyPreference(keyPreference);
        switchbutton.setOnClickListener(onclickListener);
        setPrefs(prefs);
    }
}
