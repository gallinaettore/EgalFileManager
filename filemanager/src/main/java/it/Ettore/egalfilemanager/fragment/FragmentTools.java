package it.Ettore.egalfilemanager.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.tools.Tool;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment di selezione tools
 */
public class FragmentTools extends GeneralFragment {
    private ListView listView;
    private ActivityMain activityMain;

    /**
     * Costruttore di base (necessario)
     */
    public FragmentTools(){}


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_tools, container, false);
        listView = v.findViewById(R.id.listview);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.tools);
        return v;
    }


    @Override
    public void onStart(){
        super.onStart();

        final AdapterTools adapterTools = new AdapterTools(getContext());
        listView.setAdapter(adapterTools);
        listView.requestFocus();

        listView.setOnItemClickListener((adapterView, view, position, id) -> {
            final Tool selectedTool = adapterTools.getItem(position);
            Fragment fragment;
            switch (selectedTool){
                case SPAZIO_OCCUPATO:
                    fragment = new FragmentAnalisiSpazio();
                    break;
                case RICERCA_FILES:
                    fragment = new FragmentRicercaFiles();
                    break;
                case BACKUP_APP:
                    fragment = new FragmentBackupApp();
                    break;
                case FILES_RECENTI:
                    fragment = new FragmentFilesRecenti();
                    break;
                case FILES_DUPLICATI:
                    fragment = new FragmentAvviaRicercaDuplicati();
                    break;
                case MOUNTPOINTS:
                    fragment = new FragmentMountpoints();
                    break;
                default:
                    throw new IllegalArgumentException("Nessun fragment associato a questo tool!");
            }
            activityMain.showFragment(fragment);
        });
    }



    /**
     * Adapter per la visualizzazione della lista tools
     */
    private class AdapterTools extends ArrayAdapter<Tool> {
        private final static int mIdRisorsaVista = R.layout.riga_tools;
        private final LayoutInflater mInflater;

        private AdapterTools(Context ctx) {
            super(ctx, mIdRisorsaVista, Tool.values());
            this.mInflater = LayoutInflater.from(ctx);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null){
                convertView = mInflater.inflate(mIdRisorsaVista, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.iconaImageView = convertView.findViewById(R.id.imageview_icona);
                viewHolder.nomeTextView = convertView.findViewById(R.id.textview_nome);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final Tool tool = getItem(position);
            viewHolder.iconaImageView.setImageResource(tool.resIdIcon);
            viewHolder.nomeTextView.setText(tool.resIdNome);

            return convertView;
        }
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeTextView;
    }
}
