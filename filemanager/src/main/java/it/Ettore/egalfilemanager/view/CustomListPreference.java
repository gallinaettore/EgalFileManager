package it.Ettore.egalfilemanager.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.materialpreferencesx.ListPreference;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Estensione della classe ListPreference per utilizzare la dialog customizzata
 */
public class CustomListPreference extends ListPreference {

    public CustomListPreference(Context context, String title, SharedPreferences prefs, String keyPreference) {
        super(context, title, prefs, keyPreference);
    }


    public CustomListPreference(Context context, int resIdTitle, SharedPreferences prefs, String keyPreference) {
        super(context, resIdTitle, prefs, keyPreference);
    }


    /**
     * Crea la nuova dialog customizzata
     * @param checkedIndex Indice dell'elemento selezionato
     * @return Custom dialog
     */
    @Override
    public AlertDialog createDialog(int checkedIndex) {
        final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
        builder.setTitle(getTitle());
        builder.hideIcon(true);

        //uso un adapter personalizzato perchè la lista del setSingleChoiceItems non mi mette a fuoco gli items per visualizzazione tv
        final ListAdapter adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_single_choice, android.R.id.text1, getEntries()){
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                final View v = super.getView(position, convertView, parent);
                final CheckedTextView tv = v.findViewById(android.R.id.text1);
                tv.setChecked(position == checkedIndex);
                return v;
            }
        };

        builder.setAdapter(adapter, listPreferenceClickListener);

        //builder.setSingleChoiceItems(getEntries(), checkedIndex, listPreferenceClickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
