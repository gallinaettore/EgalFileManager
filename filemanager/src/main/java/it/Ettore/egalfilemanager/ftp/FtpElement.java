package it.Ettore.egalfilemanager.ftp;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import org.apache.commons.net.ftp.FTPFile;

import java.io.Serializable;

import androidx.annotation.NonNull;


/**
 * Classe che astrae un elemento (file o directory) all'interno di un server FTP
 */
public class FtpElement implements Comparable<FtpElement>, Serializable {
    private final FTPFile ftpFile;
    private final String parentPath;
    private final String hostName;


    /**
     *
     * @param ftpFile FTPFile
     * @param parentPath Path della cartella che contiene l'FTP file
     * @param hostName Host del server FTP
     */
    public FtpElement(@NonNull FTPFile ftpFile, String parentPath, @NonNull String hostName){
        this.ftpFile = ftpFile;
        this.parentPath = parentPath != null && parentPath.isEmpty() ? null : parentPath;
        this.hostName = hostName;
    }


    /**
     * Restituisce il path assoluto del file
     * @return Path assoluto del file (nel formato: /dir1/dir2/dir3/file1)
     */
    public String getAbsolutePath(){
        if(parentPath == null){
            return "/" + getName();
        } else {
            return parentPath + "/" + getName();
        }
    }


    /**
     * Restituisce il path completo di nome host
     * @return Path completo di nome host (nel formato: ftp.nomehost.com/dir1/dir2/dir3/file1)
     */
    public String getFullPath(){
        return hostName + getAbsolutePath();
    }


    /**
     * Restituisce il path della cartella genitore
     * @return Path della cartella genitore
     */
    public String getParent(){
        return parentPath;
    }


    /**
     * Restituisce l'FTPFile settato
     * @return FTPFile
     */
    public FTPFile getFTPFile(){
        return this.ftpFile;
    }


    /**
     * Restituisce il nome del file o della cartella
     * @return Nome del file o della cartella
     */
    public String getName(){
        return this.ftpFile.getName();
    }


    /**
     * Restituisce un boolean per indicare se l'elemento è una cartella
     * @return True se è una cartella
     */
    public boolean isDirectory(){
        return this.ftpFile.isDirectory();
    }


    /**
     * Controlla se è un file nascosto (valido solo per Unix)
     * @return True se è un file nascosto (valido solo per Unix)
     */
    public boolean isHidden(){
        return getName().startsWith(".");
    }


    /**
     * Restituisce la dimensione del file
     * @return Dimensione del file
     */
    public long getSize(){
        return this.ftpFile.getSize();
    }


    /**
     * Restituisce la data del file
     * @return Data del file
     */
    public long getDate(){
        return this.ftpFile.getTimestamp().getTimeInMillis();
    }


    /**
     * Restituisce il nome host settato
     * @return Hostname settato
     */
    public String getHostName(){
        return this.hostName;
    }


    /**
     * Verifica se il file è su un server Unix
     * @return True se il file è su un server Unix
     */
    public boolean isUnix(){
        try {
            final String info = ftpFile.toString().substring(0, 1);
            return info.equals("-") || info.equals("d");
        } catch (Exception ignored){
            return false;
        }
    }


    /**
     * Restituisce una stringa che rappresenta i permessi su server Unix
     * @return Stringa che rappresenta i permessi (nel formato: rwx rwx rwx)
     */
    public String getUnixPermissions(){
        final String infos = ftpFile.toString();
        try {
            return infos.substring(1, 4) + " " + infos.substring(4, 7) + " " + infos.substring(7, 10);
        } catch (Exception ignored){
            return "";
        }
    }


    /**
     * Restituisce il path completo di nome host
     * @return Path completo di nome host
     */
    @Override
    public String toString(){
        return getFullPath();
    }


    /**
     * Compara due oggetti di tipo FtpElement
     * @param ftpElement Altro oggetto da comparare
     * @return comparazione
     */
    @Override
    public int compareTo(@NonNull FtpElement ftpElement) {
        return getAbsolutePath().compareTo(ftpElement.getAbsolutePath());
    }
}
