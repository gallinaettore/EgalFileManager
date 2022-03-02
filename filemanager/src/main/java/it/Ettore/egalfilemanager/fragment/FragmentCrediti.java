package it.Ettore.egalfilemanager.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.Ettore.androidutilsx.ui.infolibrerie.InfoLibrerie;
import it.Ettore.androidutilsx.ui.infolibrerie.Libreria;
import it.Ettore.androidutilsx.utils.DeviceUtils;
import it.Ettore.egalfilemanager.R;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione dei crediti e delle librerie
 */
public class FragmentCrediti extends GeneralFragment {

    /**
     * Costruttore di base (necessario)
     */
    public FragmentCrediti() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_crediti, container, false);

        final TextView textViewIconBy = v.findViewById(R.id.textview_iconby);
        textViewIconBy.setText(String.format("%s  %s", getString(R.string.icon_by), "db ART"));

        final LinearLayout layoutLibrerie = v.findViewById(R.id.layout_librerie);
        final InfoLibrerie infoLibrerie = new InfoLibrerie(getContext());
        boolean isAndroidTv = DeviceUtils.isAndroidTV(getContext());
        infoLibrerie.setClickableLink(!isAndroidTv);
        infoLibrerie.setResIdSelector(R.drawable.my_simple_selector);
        infoLibrerie.aggiungiLibreria(new Libreria("Glide", "https://github.com/bumptech/glide", R.raw.license_glide));
        infoLibrerie.aggiungiLibreria(new Libreria("Junrar-Android", "https://github.com/inorichi/junrar-android", R.raw.license_unrar));
        infoLibrerie.aggiungiLibreria(new Libreria("PhotoView", "https://github.com/chrisbanes/PhotoView", R.raw.license_photoview));
        infoLibrerie.aggiungiLibreria(new Libreria("Jcifs", "https://jcifs.samba.org/", R.raw.license_jcifs));
        infoLibrerie.creaLayout(layoutLibrerie);

        return v;
    }
}
