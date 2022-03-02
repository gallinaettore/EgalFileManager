package it.Ettore.egalfilemanager.pathbar;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.Ettore.egalfilemanager.R;


/**
 * Path bar da visualizzare nel file explorer
 */
public class FilePathBar extends BasePathBar {
    private File startFolder;
    private String startFolderName;
    private final PathClickListener listener;



    /**
     *
     * @param scrollView ScrollView orizzontale dentro cui posizionare la pathbar
     * @param listener Listener eseguito al click di un elemento della path bar
     */
    public FilePathBar(HorizontalScrollView scrollView, FilePathBar.PathClickListener listener){
        super(scrollView);
        this.listener = listener;
        setStartFolderName(new File("/"), "/");
        setIcon(R.drawable.pathbar_sd);
    }


    /**
     * Setta la cartella da cui cominciare a mostrare gli elementi
     * @param startFolder Cartella di inizio (esempio: /storage/emulated/0)
     * @param startFolderName Nome della cartella (esempio: Memoria interna). Il nome della cartella root sarà sempre '/' ignorando il nome passato.
     */
    public void setStartFolderName(File startFolder, String startFolderName) {
        this.startFolder = startFolder;
        if(startFolder != null && startFolder.getAbsolutePath().equals("/")){
            this.startFolderName = "/";
        } else {
            this.startFolderName = startFolderName;
        }
    }


    /**
     * Visualizza l'albero di elementi relativo al file
     * @param file File
     */
    public void visualizzaPath(final File file){
        getRootLayout().removeAllViews();
        if(file == null) return;
        List<File> alberoFiles = new ArrayList<>(); //l'albero dei file viene creato con ordine inverso
        File fileCorrente = file;
        while (fileCorrente != null){
            alberoFiles.add(fileCorrente);
            if(!fileCorrente.equals(startFolder)){
                fileCorrente = fileCorrente.getParentFile();
            } else {
                fileCorrente = null;
            }
        }

        Collections.reverse(alberoFiles);
        final LayoutInflater inflater = LayoutInflater.from(getRootLayout().getContext());
        for(int i=0; i < alberoFiles.size(); i++){
            final View view = inflater.inflate(R.layout.view_cartella_path, getRootLayout(), false);
            final TextView nomeTextView = view.findViewById(R.id.textview_nome_cartella);
            final File currentFile = alberoFiles.get(i);
            final String nomeFile = currentFile.getName();
            if(i == 0){
                //cartella principale
                nomeTextView.setText(startFolderName);
            } else {
                nomeTextView.setText(nomeFile);
            }

            nomeTextView.setOnClickListener(view1 -> {
                if(listener != null){
                    listener.onPathItemClick(currentFile);
                }
            });
            if(i == alberoFiles.size()-1){
                //nell'ultima view nascondo il chevron
                view.findViewById(R.id.imageview_chevron).setVisibility(View.GONE);
            }
            getRootLayout().addView(view);
        }
        //scroll alla fine
        scrollLayout();
    }


    /**
     *  Listener per la gestione del click sulla path bar
     */
    public interface PathClickListener {

        /**
         * Eseguito al click di un elemento della path bar
         * @param file Directory scelta
         */
        void onPathItemClick(File file);
    }
}
