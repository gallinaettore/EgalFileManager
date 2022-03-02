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
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.tools.backupapp.AppInfo;


/**
 * Builder di creazione della Dialog per mostrare le informazioni delle app installate
 */
public class DialogAppInfoBuilder {

    private final Context context;
    private final AppInfo appInfo;


    public DialogAppInfoBuilder(@NonNull Context context, @NonNull AppInfo appInfo){
        this.context = context;
        this.appInfo = appInfo;
    }


    public AlertDialog create(){
        final CustomDialogBuilder mediaInfoBuilder = new CustomDialogBuilder(context);
        mediaInfoBuilder.hideIcon(true);
        mediaInfoBuilder.setTitle(R.string.media_info);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_media_info, null);
        final TableLayout tableLayout = view.findViewById(R.id.tablelayout);
        final TextView noInfoTextView = view.findViewById(R.id.textview_no_info);
        final Map<String, String> mapMediaInfo = appInfo.toMap(context);
        if(mapMediaInfo != null && !mapMediaInfo.isEmpty()) {
            noInfoTextView.setVisibility(View.GONE);
            for (String key : mapMediaInfo.keySet()) {
                final TableRow tableRow = (TableRow) inflater.inflate(R.layout.riga_media_info, tableLayout, false);
                final TextView keyTextView = tableRow.findViewById(R.id.textview_key);
                keyTextView.setText(key);
                final TextView valueTextView = tableRow.findViewById(R.id.textview_value);
                valueTextView.setText(mapMediaInfo.get(key));
                tableLayout.addView(tableRow);
            }
        }
        mediaInfoBuilder.setView(view);
        mediaInfoBuilder.setNeutralButton(android.R.string.ok, null);
        return mediaInfoBuilder.create();
    }
}
