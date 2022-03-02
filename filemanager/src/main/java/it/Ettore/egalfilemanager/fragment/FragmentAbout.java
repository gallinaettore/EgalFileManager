package it.Ettore.egalfilemanager.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.Ettore.androidutilsx.utils.CompatUtils;
import it.Ettore.androidutilsx.utils.DeviceUtils;
import it.Ettore.androidutilsx.utils.PageOpener;
import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per la visualizzazione delle informazioni sull'app
 */
public class FragmentAbout extends GeneralFragment implements View.OnClickListener {

    /**
     * Costruttore di base (necessario)
     */
    public FragmentAbout() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_about, container, false);

        boolean isAndroidTv = DeviceUtils.isAndroidTV(getContext());

        //intestazione
        final TextView appNameTextView = v.findViewById(R.id.appNameTextView);
        final TextView creatoDaTextView = v.findViewById(R.id.creatoDaTextView);
        String versionName = "";
        try {
            versionName = "v" + getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignore) {	}
        appNameTextView.setText(String.format("%s %s", getString(R.string.app_name), versionName));
        creatoDaTextView.setText(getString(R.string.sviluppato_da, "Ettore Gallina"));

        //egalnet
        final ImageView egalnetImageView = v.findViewById(R.id.egalnetImageView);
        egalnetImageView.setOnClickListener(this);
        final TextView sitoTextView = v.findViewById(R.id.sitoTextView);
        if (!isAndroidTv){
            sitoTextView.setMovementMethod(LinkMovementMethod.getInstance());
            sitoTextView.setText(CompatUtils.fromHtml("<a href=\"https://www.gallinaettore.com\">www.gallinaettore.com</a>"));
        } else {
            sitoTextView.setText("www.gallinaettore.com");
        }

        return v;
    }



    /**
     * Gestione dei click
     * @param view View cliccata
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.egalnetImageView:
                new PageOpener(getContext()).openWebSite();
                break;
        }
    }
}
