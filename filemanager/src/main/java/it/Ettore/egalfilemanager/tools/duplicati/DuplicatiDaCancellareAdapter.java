package it.Ettore.egalfilemanager.tools.duplicati;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.FileOpener;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Adapter per la visualizzazio dei files uguali, permettendone la selezione per la cancellazione
 */

public class DuplicatiDaCancellareAdapter extends ArrayAdapter<String> {
    private static final int RES_ID_LAYOUT = R.layout.riga_duplicati_da_cancellare;
    private final FileOpener fileOpener;
    private final List<Boolean> positionSelected;


    /**
     *
     * @param context Context
     * @param paths Lista di path di files che hanno lo stesso contenuto
     */
    public DuplicatiDaCancellareAdapter(@NonNull Context context, @NonNull List<String> paths) {
        super(context, RES_ID_LAYOUT, paths);
        this.fileOpener = new FileOpener(getContext());
        this.positionSelected = new ArrayList<>(paths.size());
        for(int i=0; i < paths.size(); i++){
            this.positionSelected.add(false);
        }
    }


    /**
     * Restituisce i files che sono stati spuntati
     * @return Lista files selezionati
     */
    public List<File> getSelectedFiles(){
        final List<File> selectedFiles = new ArrayList<>();
        for (int i=0; i < positionSelected.size(); i++){
            if(positionSelected.get(i)){
                final File selectedFile = new File(getItem(i));
                selectedFiles.add(selectedFile);
            }
        }
        return selectedFiles;
    }


    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(RES_ID_LAYOUT, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textViewPath = convertView.findViewById(R.id.textview_path);
            viewHolder.checkBoxSelected = convertView.findViewById(R.id.checkbox_selezionato);
            viewHolder.layoutApri = convertView.findViewById(R.id.layout_apri);
            viewHolder.rootLayout = convertView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final String path = getItem(position);
        viewHolder.textViewPath.setText(path);
        viewHolder.layoutApri.setOnClickListener(view -> fileOpener.openFile(new File(path)));
        viewHolder.checkBoxSelected.setChecked(positionSelected.get(position));
        viewHolder.checkBoxSelected.setOnClickListener(view -> positionSelected.set(position, ((CheckBox)view).isChecked()));

        viewHolder.rootLayout.setOnFocusChangeListener((view, hasFocus) -> {
            if(hasFocus) {
                viewHolder.layoutApri.requestFocus();
            }
        });

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        TextView textViewPath;
        CheckBox checkBoxSelected;
        LinearLayout layoutApri;
        View rootLayout;
    }
}
