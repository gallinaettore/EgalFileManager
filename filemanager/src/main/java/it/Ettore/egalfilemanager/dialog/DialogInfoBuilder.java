package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;


/**
 * Builder di creazione della Dialog per mostrare le informazioni passate da una map (Chiave: Valore)
 */
public class DialogInfoBuilder {

    private final Context context;
    private final int resIdTitle;
    private final Map<String, String> mapInfo;


    /**
     *
     * @param context Context
     * @param mapInfo Map contenente le informazioni da visualizzare
     */
    public DialogInfoBuilder(@NonNull Context context, @StringRes int resIdTitle, Map<String, String> mapInfo){
        this.context = context;
        this.resIdTitle = resIdTitle;
        this.mapInfo = mapInfo;
    }


    /**
     * Crea la dialog
     * @return Dialog da visualizzare
     */
    public AlertDialog create(){
        final CustomDialogBuilder mediaInfoBuilder = new CustomDialogBuilder(context);
        mediaInfoBuilder.hideIcon(true);
        mediaInfoBuilder.setTitle(resIdTitle);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_media_info, null);
        final TableLayout tableLayout = view.findViewById(R.id.tablelayout);
        final TextView noInfoTextView = view.findViewById(R.id.textview_no_info);
        if(mapInfo != null && !mapInfo.isEmpty()) {
            noInfoTextView.setVisibility(View.GONE);
            for (String key : mapInfo.keySet()) {
                final TableRow tableRow = (TableRow) inflater.inflate(R.layout.riga_media_info, tableLayout, false);
                final TextView keyTextView = tableRow.findViewById(R.id.textview_key);
                keyTextView.setText(key);
                final TextView valueTextView = tableRow.findViewById(R.id.textview_value);
                valueTextView.setText(mapInfo.get(key));
                tableLayout.addView(tableRow);
            }
        }
        mediaInfoBuilder.setView(view);
        mediaInfoBuilder.setNeutralButton(android.R.string.ok, null);
        return mediaInfoBuilder.create();
    }
}
