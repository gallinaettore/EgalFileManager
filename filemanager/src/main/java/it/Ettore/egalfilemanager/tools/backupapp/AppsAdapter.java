package it.Ettore.egalfilemanager.tools.backupapp;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.Ettore.androidutilsx.ListFilter;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * Adapter per mostrare i risultati delle apps trovate
 */
public class AppsAdapter extends ArrayAdapter<AppInfo> implements ListFilter.Filterable {
    private final static int mIdRisorsaVista = R.layout.riga_backup_app;
    //private final ExecutorService executorService;
    private List<AppInfo> appInfos, backupListaCompleta;
    private boolean mostraAnteprime = true;
    private final SharedPreferences prefs;


    /**
     *
     * @param context Context
     * @param appInfos Lista apps da mostrare
     */
    public AppsAdapter(@NonNull Context context, @NonNull List<AppInfo> appInfos) {
        super(context, mIdRisorsaVista, appInfos);
        this.appInfos = appInfos;
        //this.executorService = Executors.newFixedThreadPool(8);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    /**
     * Aggiorna l'adapter con una nuova lista
     * @param appInfos Nuova lista di app info
     */
    public void update(List<AppInfo> appInfos){
        mostraAnteprime = prefs.getBoolean(Costanti.KEY_PREF_MOSTRA_ANTEPRIME, true);
        this.appInfos = appInfos;
        clear();
        addAll(appInfos);
        notifyDataSetChanged();
    }



    /**
     * Abilita/disabilita la modalità filtro files nell'adapter
     * @param filterMode True per abilitare la modalità filtro
     */
    @Override
    public void setFilterMode(boolean filterMode){
        if(filterMode){
            backupListaCompleta = new ArrayList<>(appInfos);
        } else {
            if(backupListaCompleta != null) {
                update(new ArrayList<>(backupListaCompleta));
                backupListaCompleta = null;
            }
        }
    }


    /**
     * Mostra solo i files il cui nome che contiene la strimga query
     * @param query Stringa di filtraggio files
     */
    @Override
    public void filter(String query){
        if(backupListaCompleta == null) return;
        final List<AppInfo> listaFiltrata;
        if(query == null || query.isEmpty()){
            listaFiltrata = new ArrayList<>(backupListaCompleta);
        } else {
            listaFiltrata = new ArrayList<>();
            for(AppInfo appInfo : backupListaCompleta){
                if(appInfo.name.toLowerCase().contains(query.toLowerCase())){
                    listaFiltrata.add(appInfo);
                }
            }
        }
        update(listaFiltrata);
        notifyDataSetChanged();
    }


    /**
     * View da mostrare
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(mIdRisorsaVista, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.iconaImageView = convertView.findViewById(R.id.imageview_icona);
            viewHolder.nomeTextView = convertView.findViewById(R.id.textview_nome);
            viewHolder.packageTextView = convertView.findViewById(R.id.textview_package);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final AppInfo appInfo = getItem(position);
        viewHolder.iconaImageView.setImageResource(R.drawable.ico_file_apk);
        if(mostraAnteprime) {
            //getExecutorService().execute(new LoadThumbnailThread(getContext(), viewHolder.iconaImageView, appInfo.apk, 40, 40));
            int imageSizePx = (int) getContext().getResources().getDimension(R.dimen.size_icona_lista_backup_app); //ritorna pixel anche se espresso in dp
            IconManager.showImageWithGlide(appInfo.apk, viewHolder.iconaImageView, imageSizePx, imageSizePx);
        }
        viewHolder.nomeTextView.setText(appInfo.name);
        viewHolder.packageTextView.setText(appInfo.packageName);

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeTextView, packageTextView;
    }
}
