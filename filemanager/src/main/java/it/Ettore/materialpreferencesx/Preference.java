package it.Ettore.materialpreferencesx;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public class Preference extends GeneralPreference {

    public Preference(Context context, int resIdTitle) {
        super(context);
        setup();
        setTitle(resIdTitle);
    }


    public Preference(Context context, String title) {
        super(context);
        setup();
        setTitle(title);
    }


    private void setup(){
        inflate(getContext(), R.layout.simple_preference, this);
        setTitleTextView((TextView) findViewById(R.id.titleTextView));
        setSummaryTextView((TextView) findViewById(R.id.summaryTextView));
        setIconImageView((ImageView) findViewById(R.id.iconaImageView));
        nascondiSummary(true);
    }


    /**
     * Listener delle list preferences
     */
    public interface OnPreferenceChangeListener {

        /**
         *
         * @param preference Preference che chiama il listener
         * @param newValue Nuovo valore impostato dalla preference
         * @return Booleano non utilizzato. Si può restituire false
         */
        boolean onPreferenceChange(Preference preference, Object newValue);
    }

}
