package it.Ettore.egalfilemanager.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fileutils.LocalFileUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.fragment.FragmentFilesExplorer;

import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ROOT_EXPLORER;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * View che rappresenta un preferito salvato nel main fragment
 */
public class ViewFavorite extends LinearLayout {
    private TextView nomeTextView, descrizioneTextView;
    private LinearLayout layoutPreferito;
    private FrameLayout cancellaLayout;


    public ViewFavorite(Context context) {
        super(context);
        init(null);
    }


    public ViewFavorite(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public ViewFavorite(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    /**
     * Inizializza la view
     * @param attrs Set di attributi usati per la gestione della view tramite xml
     */
    @SuppressLint("RtlHardcoded")
    private void init(AttributeSet attrs){
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.view_preferito, null);
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(param);
        addView(view);
        final FloatingActionButton fabIcona = view.findViewById(R.id.fabIcona);
        ViewUtils.correctFabMargin(fabIcona);
        nomeTextView = view.findViewById(R.id.textview_nome_file);
        descrizioneTextView = view.findViewById(R.id.textview_path_file);
        layoutPreferito = view.findViewById(R.id.layout_preferito);
        cancellaLayout = view.findViewById(R.id.layout_cancella_preferito);

        if (LayoutDirectionHelper.isRightToLeft(getContext())){
            nomeTextView.setGravity(Gravity.RIGHT);
            descrizioneTextView.setGravity(Gravity.RIGHT);
        }

        fabIcona.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                view.requestFocus(); //da il focus sempre alla vie generale per ottimizzare la visualizzazione su tv
            }
        });

        //Leggo gli attributi impostati da xml
        //Gli attributi sono specificati nel file values/attrs.xml
        if(attrs != null) {
            final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ViewFavorite, 0, 0);
            try {
                nomeTextView.setText(a.getString(R.styleable.ViewFavorite_name));
                descrizioneTextView.setText(a.getString(R.styleable.ViewFavorite_description));
                fabIcona.setImageResource(a.getResourceId(R.styleable.ViewFavorite_icon, 0));
                boolean showRemoveButton = a.getBoolean(R.styleable.ViewFavorite_showRemoveButton, true);
                if (!showRemoveButton) {
                    cancellaLayout.setVisibility(View.GONE);
                }
            } finally {
                a.recycle();
            }
        }
    }


    /**
     * Imposta il file preferito da visualizzare
     * @param file File preferito
     * @param activityMain Activity che mostrerà il fragment per la navigazione
     * @param listener Listener chiamato alla pressione del tasto di cancellazione (dopo la conferma della dialog)
     */
    public void setFile(@NonNull final File file, @NonNull final ActivityMain activityMain, @NonNull final ViewFavoriteListener listener){
        nomeTextView.setText(file.getName());
        descrizioneTextView.setText(file.getParent());

        layoutPreferito.setOnClickListener(view -> {
            if(file.exists()) {
                //verifico l'esistenza del file
                if (file.isDirectory()) {
                    activityMain.showFragment(FragmentFilesExplorer.getInstance(file));
                } else {
                    new FileOpener(getContext()).openFile(file);
                }
            } else if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(KEY_PREF_ROOT_EXPLORER, false) &&
                    new StoragesUtils(getContext()).isOnRootPath(file) && LocalFileUtils.rootFileExists(file)){
                //verifico l'esistenza qualora il file fosse su un percorso root e ne ho i permessi (il primo controllo fallisce)
                if(LocalFileUtils.rootFileIsDirectory(getContext(), file)){
                    activityMain.showFragment(FragmentFilesExplorer.getInstance(file));
                } else {
                    new FileOpener(getContext()).openFile(file);
                }
            } else {
                //mostro la dialog di rimozione se il file non esiste
                final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                builder.setType(CustomDialogBuilder.TYPE_WARNING);
                builder.setMessage(getContext().getString(R.string.preferito_non_esistente, file.getName()));
                builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> listener.onDeleteFavoriteButtonPressed(file));
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
            }
        });

        cancellaLayout.setOnClickListener(view -> {
            final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
            builder.setType(CustomDialogBuilder.TYPE_WARNING);
            builder.setMessage(getContext().getString(R.string.domanda_rimuovi_preferito, file.getName()));
            builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> listener.onDeleteFavoriteButtonPressed(file));
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        });
    }


    /**
     * Listener della View Favorite
     */
    public interface ViewFavoriteListener {

        /**
         * Chiamato alla pressione del tasto di cancellazione (dopo la conferma della dialog)
         * @param favorite File da rimuovere dai preferiti
         */
        void onDeleteFavoriteButtonPressed(File favorite);
    }
}


