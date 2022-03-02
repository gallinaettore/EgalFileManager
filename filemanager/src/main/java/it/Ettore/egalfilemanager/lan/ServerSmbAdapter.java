package it.Ettore.egalfilemanager.lan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import jcifs.smb.SmbFile;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Adaper per la visualizzazione dei server lan
 */
public class ServerSmbAdapter extends ArrayAdapter<SmbFile> {
    private final static int mIdRisorsaVista = R.layout.riga_server_lan;
    private final List<SmbFile> listaServer;
    private final List<String> nomiHost;


    /**
     *
     * @param context Context
     */
    public ServerSmbAdapter(@NonNull Context context) {
        super(context, mIdRisorsaVista, new ArrayList<>());
        listaServer = new ArrayList<>();
        nomiHost = new ArrayList<>();
    }


    /**
     * Aggiunge un server all'adapter
     * @param ipAddress Indirizzo ip o nome host
     * @param hostName Nome NETBios del server
     */
    public void addServer(@NonNull String ipAddress, @NonNull String hostName){
        try {
            final SmbFile server = new SmbFile("smb://" + ipAddress + "/");
            listaServer.add(server);
            nomiHost.add(hostName);
            notifyDataSetChanged();
        } catch (MalformedURLException ignored) {}
    }


    /**
     * Restituisce il numero di server presenti
     * @return Numero di server nell'adapter
     */
    @Override
    public int getCount() {
        return listaServer.size();
    }


    /**
     * Restituisce il server alla posizione richiesta
     * @param position Posizione nell'adapter
     * @return File che rappresenta il server
     */
    @Nullable
    @Override
    public SmbFile getItem(int position) {
        return listaServer.get(position);
    }



    @SuppressLint("RtlHardcoded")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(mIdRisorsaVista, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.iconaImageView = convertView.findViewById(R.id.imageview_icona);
            viewHolder.nomeTextView = convertView.findViewById(R.id.textview_nome);
            viewHolder.descrizioneTextView = convertView.findViewById(R.id.textview_descrizione);
            convertView.setTag(viewHolder);

            if(LayoutDirectionHelper.isRightToLeft(getContext())){
                viewHolder.nomeTextView.setGravity(Gravity.RIGHT);
                viewHolder.descrizioneTextView.setGravity(Gravity.RIGHT);
            }
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final SmbFile file = getItem(position);
        //viewHolder.iconaImageView.setImageResource(ris.file.isDirectory() ? R.drawable.ico_cartella : IconManager.iconForFile(ris.file));
        viewHolder.nomeTextView.setText(nomiHost.get(position));
        viewHolder.descrizioneTextView.setText(file.getServer());

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeTextView, descrizioneTextView;
    }

}
