package it.Ettore.egalfilemanager.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.fragment.FragmentFilesExplorer;
import it.Ettore.egalfilemanager.home.HomeItem;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * View che rappresenta una storage nel main fragment
 */
public class ViewStorage extends LinearLayout {
    private View view;
    private TextView nomeTextView, pathTextView, spazioStorageTextView;
    private ProgressBar progressBar;
    private FloatingActionButton fabIcona;
    private FrameLayout settingsLayout;

    public ViewStorage(Context context) {
        super(context);
        init();
    }


    public ViewStorage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public ViewStorage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    /**
     * Inizializza la view
     */
    @SuppressLint("RtlHardcoded")
    private void init(){
        view = LayoutInflater.from(getContext()).inflate(R.layout.view_storage, null);
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(param);
        addView(view);
        fabIcona = view.findViewById(R.id.fabIcona);
        ViewUtils.correctFabMargin(fabIcona);
        settingsLayout = view.findViewById(R.id.layout_impostazioni);
        nomeTextView = view.findViewById(R.id.textview_nome);
        pathTextView = view.findViewById(R.id.textview_path);
        spazioStorageTextView = view.findViewById(R.id.textview_spazio_storage);
        progressBar = view.findViewById(R.id.progressBar);

        if (LayoutDirectionHelper.isRightToLeft(getContext())){
            nomeTextView.setGravity(Gravity.RIGHT);
            pathTextView.setGravity(Gravity.RIGHT);
            spazioStorageTextView.setGravity(Gravity.LEFT);
        }
    }


    /**
     * Imposta l'item con i dati da visualizzare nella view
     * @param item Item da visualizzare
     * @param activityMain Activity Main
     */
    public void setHomeItem(@NonNull final HomeItem item, @NonNull final ActivityMain activityMain){
        fabIcona.setImageResource(item.resIdIcona);
        fabIcona.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                this.view.requestFocus(); //da il focus sempre alla view generale per ottimizzare la visualizzazione su tv
            }
        });
        nomeTextView.setText(item.titolo);
        pathTextView.setText(item.startDirectory.getAbsolutePath());
        view.setOnClickListener(view -> {
            final FragmentFilesExplorer fragment = FragmentFilesExplorer.getInstance(item.titolo, item.startDirectory, item.titolo, item.startDirectory);
            activityMain.showFragment(fragment);
        });
        if(!item.startDirectory.getAbsolutePath().equals("/")){
            //imposto il colore della barra
            final TypedValue typedValue = new TypedValue();
            final Resources.Theme theme = activityMain.getTheme();
            theme.resolveAttribute(R.attr.colorIconeDispositivi, typedValue, true);
            @ColorRes int coloreIconeDispositivi = typedValue.resourceId;
            progressBar.getProgressDrawable().setColorFilter(ContextCompat.getColor(getContext(), coloreIconeDispositivi), android.graphics.PorterDuff.Mode.SRC_IN);
            //calcolo lo spazio utilizzato
            long spazioTotale = item.startDirectory.getTotalSpace();
            long spazioUtilizzato = spazioTotale - item.startDirectory.getUsableSpace();
            final String[] arrayBytes = MyUtils.stringResToStringArray(getContext(), Costanti.ARRAY_BYTES_IDS);
            spazioStorageTextView.setText(String.format("%s / %s", MyMath.humanReadableByte(spazioUtilizzato, arrayBytes), MyMath.humanReadableByte(spazioTotale, arrayBytes)));
            if(spazioTotale > 0) {
                progressBar.setProgress((int) (spazioUtilizzato * 100 / spazioTotale));
            }
            //configuro l'icona impostazioni
            settingsLayout.setOnClickListener(view -> {
                try {
                    final Intent i = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);
                    activityMain.startActivity(i);
                } catch (Exception ignored){}
            });
        } else {
            //nascondo la progress nel percorso root
            progressBar.setVisibility(View.GONE);
            spazioStorageTextView.setVisibility(View.GONE);
            //nascondo l'icona impostazioni
            settingsLayout.setVisibility(View.GONE);
        }
    }


    /**
     * Impostat i valori da visualizzare (utilizzato solo nella modalità screenshot
     * @param resIdIcona
     * @param titolo
     * @param path
     * @param spazioTotale
     * @param spazioUtilizzato
     * @param memoriaInterna
     * @param activityMain
     */
    public void setValues(int resIdIcona, String titolo, String path, long spazioTotale, long spazioUtilizzato, HomeItem memoriaInterna, @NonNull final ActivityMain activityMain){
        fabIcona.setImageResource(resIdIcona);
        nomeTextView.setText(titolo);
        pathTextView.setText(path);
        final TextView spazioStorageTextView = view.findViewById(R.id.textview_spazio_storage);
        view.setOnClickListener(view -> {
            final FragmentFilesExplorer fragment = FragmentFilesExplorer.getInstance(memoriaInterna.titolo, memoriaInterna.startDirectory, memoriaInterna.titolo, memoriaInterna.startDirectory);
            activityMain.showFragment(fragment);
        });
        if(!path.equals("/")) {
            //imposto il colore della barra
            final TypedValue typedValue = new TypedValue();
            final Resources.Theme theme = activityMain.getTheme();
            theme.resolveAttribute(R.attr.colorIconeDispositivi, typedValue, true);
            @ColorRes int coloreIconeDispositivi = typedValue.resourceId;
            progressBar.getProgressDrawable().setColorFilter(ContextCompat.getColor(getContext(), coloreIconeDispositivi), android.graphics.PorterDuff.Mode.SRC_IN);
            //calcolo lo spazio utilizzato
            final String[] arrayBytes = MyUtils.stringResToStringArray(getContext(), Costanti.ARRAY_BYTES_IDS);
            spazioStorageTextView.setText(String.format("%s / %s", MyMath.humanReadableByte(spazioUtilizzato, arrayBytes), MyMath.humanReadableByte(spazioTotale, arrayBytes)));
            progressBar.setProgress((int) (spazioUtilizzato * 100 / spazioTotale));
        } else {
            //nascondo la progress nel percorso root
            progressBar.setVisibility(View.GONE);
            spazioStorageTextView.setVisibility(View.GONE);
            //nascondo l'icona impostazioni
            settingsLayout.setVisibility(View.GONE);
        }
    }
}
