package it.Ettore.egalfilemanager.lan.thread;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.os.AsyncTask;

import androidx.annotation.NonNull;
import it.Ettore.egalfilemanager.lan.SmbFileData;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class SmbFileDataTask extends AsyncTask<SmbFile, Void, SmbFileData> {
    private final SmbFile smbFile;
    private final SmbFileDataListener listener;


    public SmbFileDataTask(SmbFile smbFile, @NonNull SmbFileDataListener listener){
        this.smbFile = smbFile;
        this.listener = listener;
    }


    @Override
    protected SmbFileData doInBackground(SmbFile... params) {
        if(smbFile == null) return null;
        final SmbFileData smbFileData = new SmbFileData();
        try {
            smbFileData.size = smbFile.length();
            smbFileData.isHidden = smbFile.isHidden();
        } catch (SmbException e) {
            e.printStackTrace();
            return null;
        }
        return smbFileData;
    }


    @Override
    protected void onPostExecute(SmbFileData smbFileData) {
        listener.onSmbFileDataFound(smbFileData);
    }


    public interface SmbFileDataListener {
        void onSmbFileDataFound(SmbFileData smbFileData);
    }
}
