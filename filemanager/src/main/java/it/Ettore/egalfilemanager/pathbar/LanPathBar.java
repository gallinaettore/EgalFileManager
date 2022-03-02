package it.Ettore.egalfilemanager.pathbar;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.Ettore.egalfilemanager.R;


/**
 * Path bar da visualizzare nel file explorer
 */
public class LanPathBar extends BasePathBar {
    private final PathClickListener listener;


    /**
     *
     * @param scrollView ScrollView orizzontale dentro cui posizionare la pathbar
     * @param listener Listener eseguito al click di un elemento della path bar
     */
    public LanPathBar(HorizontalScrollView scrollView, LanPathBar.PathClickListener listener){
        super(scrollView);
        this.listener = listener;
        setIcon(R.drawable.pathbar_lan);
    }


    /**
     * Visualizza l'albero di elementi relativo al file
     * @param filePath File
     */
    public void visualizzaPath(final String filePath){
        getRootLayout().removeAllViews();
        if(filePath == null) return;

        final List<String> alberoPaths = new ArrayList<>(); //l'albero dei file viene creato con ordine inverso
        List<String> nomi = new ArrayList<>();
        String pathCorrente = filePath;
        while (pathCorrente != null){
            alberoPaths.add(pathCorrente);
            nomi.add(getNomeFile(pathCorrente));
            pathCorrente = getParentPath(pathCorrente);
        }

        Collections.reverse(alberoPaths);
        Collections.reverse(nomi);
        final LayoutInflater inflater = LayoutInflater.from(getRootLayout().getContext());
        for(int i=0; i < nomi.size(); i++){
            final View view = inflater.inflate(R.layout.view_cartella_path, getRootLayout(), false);
            final TextView nomeTextView = view.findViewById(R.id.textview_nome_cartella);
            final String currentName = nomi.get(i);
            final String currentPath = alberoPaths.get(i);
            nomeTextView.setText(currentName);

            nomeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null){
                        listener.onPathItemClick(currentPath);
                    }
                }
            });
            if(i == nomi.size()-1){
                //nell'ultima view nascondo il chevron
                view.findViewById(R.id.imageview_chevron).setVisibility(View.GONE);
            }
            getRootLayout().addView(view);
        }
        //scroll alla fine
        scrollLayout();
    }


    /**
     * Ottiene il path della cartella genitore
     * @param path Path della cartella da analizzare
     * @return Path della cartella genitore
     */
    private String getParentPath(String path){
        if(path == null || path.equals("smb://")){
            return null;
        }
        String pathSenzaUltimoSlash = path;
        if(path.endsWith("/")){
            pathSenzaUltimoSlash = pathSenzaUltimoSlash.substring(0, path.length()-1);
        }
        int indiceUltimoSlash = pathSenzaUltimoSlash.lastIndexOf("/");
        String parent = pathSenzaUltimoSlash.substring(0, indiceUltimoSlash+1);
        if(!parent.equals("smb://")){
            return parent;
        } else {
            return null;
        }
    }


    /**
     * Ottiene il nome della cartella dall'intero path
     * @param path Path della cartella
     * @return Nome cartella
     */
    private String getNomeFile(String path){
        if(path == null || path.equals("smb://")){
            return null;
        }
        String pathSenzaUltimoSlash = path;
        if(path.endsWith("/")){
            pathSenzaUltimoSlash = pathSenzaUltimoSlash.substring(0, path.length()-1);
        }
        int indiceUltimoSlash = pathSenzaUltimoSlash.lastIndexOf("/");
        return  pathSenzaUltimoSlash.substring(indiceUltimoSlash+1);
    }


    /**
     *  Listener per la gestione del click sulla path bar
     */
    public interface PathClickListener {

        /**
         * Eseguito al click di un elemento della path bar
         * @param filePath Directory scelta
         */
        void onPathItemClick(String filePath);
    }

}
