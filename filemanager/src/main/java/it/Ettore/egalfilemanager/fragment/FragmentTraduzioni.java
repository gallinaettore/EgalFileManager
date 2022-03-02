package it.Ettore.egalfilemanager.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.lang.Lingua;
import it.Ettore.egalfilemanager.Lingue;
import it.Ettore.egalfilemanager.R;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione della lista traduttori
 */
public class FragmentTraduzioni extends GeneralFragment {

    /**
     * Costruttore di base (necessario)
     */
    public FragmentTraduzioni() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_traduzioni, container, false);

        final LinearLayout layoutTraduzioni = (TableLayout)v.findViewById(R.id.layout_traduzioni);
        final List<Lingua> lingue = Lingue.getValues();
        Collections.sort(lingue);
        for(Lingua linguaCorrente : lingue){
            final TableRow tableRow = (TableRow)inflater.inflate(R.layout.riga_traduttori, layoutTraduzioni, false);
            final TextView linguaTextView = tableRow.findViewById(R.id.linguaTextView);
            linguaTextView.setText(linguaCorrente.getNome());
            final TextView traduttoreTextView = tableRow.findViewById(R.id.traduttoreTextView);
            traduttoreTextView.setText(linguaCorrente.getTraduttoriACapo());
            layoutTraduzioni.addView(tableRow);
        }

        return v;
    }
}
