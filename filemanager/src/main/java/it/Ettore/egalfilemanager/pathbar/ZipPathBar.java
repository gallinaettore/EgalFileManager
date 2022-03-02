package it.Ettore.egalfilemanager.pathbar;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.zipexplorer.ArchiveEntry;



/**
 * Path bar da visualizzare nel zip explorer
 */
public class ZipPathBar extends BasePathBar {
    private final String startEntryName;
    private final PathClickListener listener;


    /**
     *
     * @param scrollView ScrollView orizzontale dentro cui posizionare la pathbar
     * @param startEntryName Cartella (Entry) di inizio
     * @param listener Listener eseguito al click di un elemento della path bar
     */
    public ZipPathBar(HorizontalScrollView scrollView, String startEntryName, PathClickListener listener){
        super(scrollView);
        this.listener = listener;
        this.startEntryName = startEntryName;
        setIcon(R.drawable.pathbar_zip);
    }


    /**
     * Visualizza l'albero di elementi relativo all'elemento
     * @param entry Elemento dell'archivio
     */
    public void visualizzaPath(final ArchiveEntry entry){
        getRootLayout().removeAllViews();
        final List<String> nomiEntry = new ArrayList<>();
        final List<String> pathsEntry = new ArrayList<>();

        if(entry != null) { //entry è null nella cartella root
            final List<String> strutturaDirectory = new ArrayList<>(Arrays.asList(entry.getStrutturaDirectory()));
            if(strutturaDirectory.get(0).isEmpty()){
                //se il primo elemento della lista è vuoto lo rimuovo
                strutturaDirectory.remove(0);
            }

            for (int i = 0; i < strutturaDirectory.size(); i++) {
                nomiEntry.add(strutturaDirectory.get(strutturaDirectory.size() - 1 - i));
                final StringBuilder sb = new StringBuilder();
                for (int k = 0; k < strutturaDirectory.size() - i; k++) {
                    sb.append(strutturaDirectory.get(k)).append("/");
                }
                pathsEntry.add(sb.toString());
            }
        }
        nomiEntry.add(startEntryName);
        pathsEntry.add(null);

        Collections.reverse(nomiEntry);
        Collections.reverse(pathsEntry);

        final LayoutInflater inflater = LayoutInflater.from(getRootLayout().getContext());
        for(int i=0; i < nomiEntry.size(); i++){
            final View view = inflater.inflate(R.layout.view_cartella_path, getRootLayout(), false);
            final TextView nomeTextView = view.findViewById(R.id.textview_nome_cartella);
            nomeTextView.setText(nomiEntry.get(i));
            final String pathEntry = pathsEntry.get(i);
            nomeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null){
                        listener.onPathItemClick(pathEntry);
                    }
                }
            });
            if(i == nomiEntry.size()-1){
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
         * @param entryPath Path della cartella (interna al zip) scelta
         */
        void onPathItemClick(String entryPath);
    }

}
