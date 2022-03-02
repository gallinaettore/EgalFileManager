package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/



import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.egalfilemanager.ftp.FtpElement;
import it.Ettore.egalfilemanager.ftp.ServerFtp;
import jcifs.smb.SmbFile;


/**
 * Clipboard contenente i files locali da incollare
 */
public class Clipboard {
    public static final int TIPOFILE_NESSUNO = 0;
    public static final int TIPOFILE_LOCALE = 1;
    public static final int TIPOFILE_SMB = 2;
    public static final int TIPOFILE_FTP = 3;
    private List<File> listaFiles;
    private List<SmbFile> listaSmbFiles;
    private List<FtpElement> listaFtpFiles;
    private boolean cutMode;
    private int tipoFile;
    private String smbUser, smbPassword;
    private ServerFtp serverFtp;


    public Clipboard(){
        listaFiles = new ArrayList<>();
        listaSmbFiles = new ArrayList<>();
        listaFtpFiles = new ArrayList<>();
    }



    /**
     *
     * @return True se è in modalità taglia. False se è in modalità copia
     */
    public boolean isCutMode() {
        return cutMode;
    }



    /**
     *
     * @param cutMode Imposta la modalità taglia piuttosto che copia
     */
    public void setCutMode(boolean cutMode) {
        this.cutMode = cutMode;
    }



    /**
     *
     * @return Lista di files locali all'interno della clipboard
     */
    public List<File> getListaFiles(){
        return this.listaFiles;
    }


    /**
     *
     * @return Lista di files smb all'interno della clipboard
     */
    public List<SmbFile> getListaSmbFiles(){
        return this.listaSmbFiles;
    }


    /**
     *
     * @return Lista di files ftp all'interno della clipboard
     */
    public List<FtpElement> getListaFtpFiles(){
        return this.listaFtpFiles;
    }



    /**
     * Aggiorna la clipboard con una nuova lista files
     * @param nuovaListaFiles Nuova lista di files
     */
    public void aggiornaFilesLocali(List<File> nuovaListaFiles){
        svuotaListe();
        if(nuovaListaFiles != null){
            this.tipoFile = TIPOFILE_LOCALE;
            this.listaFiles = new ArrayList<>(nuovaListaFiles);
        }
    }


    /**
     * Aggiorna la clipboard con una nuova lista files
     * @param nuovaListaFiles Nuova lista di files
     */
    public void aggiornaFilesSmb(List<SmbFile> nuovaListaFiles, String smbUser, String smbPassword){
        svuotaListe();
        if(nuovaListaFiles != null){
            this.tipoFile = TIPOFILE_SMB;
            this.listaSmbFiles = new ArrayList<>(nuovaListaFiles);
            setCredenzialiSmb(smbUser, smbPassword);
        }
    }


    /**
     * Aggiorna la clipboard con una nuova lista files
     * @param nuovaListaFiles Nuova lista di files
     */
    public void aggiornaFilesFtp(List<FtpElement> nuovaListaFiles, ServerFtp serverFtp){
        svuotaListe();
        if(nuovaListaFiles != null){
            this.tipoFile = TIPOFILE_FTP;
            this.listaFtpFiles = new ArrayList<>(nuovaListaFiles);
            setServerFtp(serverFtp);
        }
    }



    /**
     * La clipboard viene ripristinata allo stato iniziale
     */
    public void clear(){
        svuotaListe();
        this.cutMode = false;
        setCredenzialiSmb(null, null);
        setServerFtp(null);
    }


    /**
     * Elimina il contenuto di tutte le liste
     */
    private void svuotaListe(){
        this.tipoFile = TIPOFILE_NESSUNO;
        this.listaFiles.clear();
        this.listaSmbFiles.clear();
        this.listaFtpFiles.clear();
    }


    /**
     *
     * @return True se la clipboard non contiene elementi
     */
    public boolean isEmpty(){
        switch (tipoFile){
            case TIPOFILE_NESSUNO:
                return true;
            case TIPOFILE_LOCALE:
                return this.listaFiles.isEmpty();
            case TIPOFILE_SMB:
                return this.listaSmbFiles.isEmpty();
            case TIPOFILE_FTP:
                return this.listaFtpFiles.isEmpty();
            default:
                throw new IllegalArgumentException("Tipo file non gestito");
        }
    }


    /**
     * Restituisce il tipo di file usato nella clipboard
     * @return Una della costanti TIPOFILE di questa classe
     */
    public int getTipoFile(){
        return this.tipoFile;
    }


    /**
     * Restituisce la lista di path dei files presenti nella clipboard.
     * @return Lista di path dei files presenti nella clipboard. Lista vuota se non ci sono files o in caso di errore.
     */
    public List<String> getListaPath(){
        final List<String> listaPath = new ArrayList<>();
        switch (tipoFile) {
            case TIPOFILE_LOCALE:
                for(File file : this.listaFiles){
                    listaPath.add(file.getAbsolutePath());
                }
                break;
            case TIPOFILE_SMB:
                for(SmbFile file : this.listaSmbFiles){
                    listaPath.add(file.getPath());
                }
                break;
            case TIPOFILE_FTP:
                for (FtpElement file : this.listaFtpFiles){
                    listaPath.add(file.getFullPath());
                }
                break;
        }
        return listaPath;
    }


    /**
     * Imposta le credenziali dei files smb
     * @param user Username del server smb
     * @param password Password del server smb
     */
    private void setCredenzialiSmb(String user, String password){
        this.smbUser = user;
        this.smbPassword = password;
    }


    /**
     * Restituisce l'username del serve smb
     * @return Username
     */
    public String getSmbUser() {
        return smbUser;
    }


    /**
     * Restituisce la password del server smb
     * @return Password
     */
    public String getSmbPassword() {
        return smbPassword;
    }


    /**
     * Restituisce i dati del server FTP di origine
     * @return Dati del server FTP
     */
    public ServerFtp getServerFtp() {
        return serverFtp;
    }


    /**
     * Imposta i dati del server FTP di origine
     */
    private void setServerFtp(ServerFtp serverFtp) {
        this.serverFtp = serverFtp;
    }
}
