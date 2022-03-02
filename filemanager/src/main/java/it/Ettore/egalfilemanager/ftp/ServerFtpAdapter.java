package it.Ettore.egalfilemanager.ftp;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;


/**
 * Adapter per la visualizzazione dei server FTP
 */
public class ServerFtpAdapter extends ArrayAdapter<ServerFtp> {
    private final static @LayoutRes int RES_ID_LAYOUT = R.layout.riga_server_ftp;


    /**
     *
     * @param context Context chiamante
     */
    public ServerFtpAdapter(@NonNull Context context) {
        super(context, RES_ID_LAYOUT, ServerFtp.getAllSavedServers(context));
    }


    @SuppressLint("RtlHardcoded")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(RES_ID_LAYOUT, parent, false);
            viewHolder = new ViewHolder();
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

        final ServerFtp serverFtp = getItem(position);
        if(serverFtp.getNomeVisualizzato() != null){
            viewHolder.nomeTextView.setText(serverFtp.getNomeVisualizzato());
            viewHolder.descrizioneTextView.setText(serverFtp.getHost());
            viewHolder.descrizioneTextView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.nomeTextView.setText(serverFtp.getHost());
            viewHolder.descrizioneTextView.setVisibility(View.GONE);
        }

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        TextView nomeTextView, descrizioneTextView;
    }
}



