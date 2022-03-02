package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.utils.RootUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.mount.Mountpoint;
import it.Ettore.egalfilemanager.mount.MountpointManager;


/**
 * Fragment per la visualizzazione dei mountpoints
 */
public class FragmentMountpoints extends GeneralFragment {
    private ListView listView;


    /**
     * Costruttore di default (necessario)
     */
    public FragmentMountpoints(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_mountpoints, container, false);
        ((ActivityMain)getActivity()).setActionBarTitle(R.string.tool_mountpoints);
        listView = v.findViewById(R.id.listview);
        return v;
    }


    @Override
    public void onStart(){
        super.onStart();
        if(!RootUtils.isPhoneRooted()){
            CustomDialogBuilder.make(getContext(), R.string.no_permessi_root, CustomDialogBuilder.TYPE_ERROR).show();
        }
        final List<Mountpoint> mountpoints = new MountpointManager().getMountpointList();
        listView.setAdapter(new MountpointsAdapter(getContext(), mountpoints));
        new Handler().postDelayed(() -> listView.requestFocus(), 200);
    }


    /**
     * Adapter per la visualizzazione dei mountpoints
     */
    private static class MountpointsAdapter extends ArrayAdapter<Mountpoint> {
        private final static int LAYOUT = R.layout.riga_mountpoints;
        private final LayoutInflater inflater;

        /**
         *
         * @param context Context
         * @param objects Lista mountpoints da visualizzare
         */
        private MountpointsAdapter(@NonNull Context context, @NonNull List<Mountpoint> objects) {
            super(context, LAYOUT, objects);
            inflater = LayoutInflater.from(context);
        }


        @SuppressLint("ViewHolder")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //non uso il view holder perchè aggiungo righe alla tabella via codice

            convertView = inflater.inflate(LAYOUT, parent, false);
            final TextView mountpointTextView = convertView.findViewById(R.id.textview_mountpoint);
            final TextView fileSystemTextView = convertView.findViewById(R.id.textview_filesystem);
            final TextView permessiTextView = convertView.findViewById(R.id.textview_permessi);
            final TableLayout tableLayout = convertView.findViewById(R.id.tablelayout);

            final Mountpoint mountpoint = getItem(position);
            mountpointTextView.setText(mountpoint.getPath());
            fileSystemTextView.setText(mountpoint.getFilesystem());
            permessiTextView.setText(mountpoint.getStatoString());

            final Map<String, String> mapAltriParametri = mountpoint.getMapAltriParametri();
            if(mapAltriParametri != null){
                for(String key : mapAltriParametri.keySet()){
                    final TableRow tableRow = (TableRow)inflater.inflate(R.layout.riga_mountpoints_altri_parametri, tableLayout, false);
                    final TextView keyTextView = tableRow.findViewById(R.id.textview_chiave);
                    final TextView valueTextView = tableRow.findViewById(R.id.textview_valore);
                    keyTextView.setText(key);
                    valueTextView.setText(mapAltriParametri.get(key));
                    tableLayout.addView(tableRow);
                }
            }

            return convertView;
        }
    }
}
