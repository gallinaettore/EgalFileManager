package it.Ettore.egalfilemanager.mount;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;


/**
 * Classe wrapper con i dati relativi al moutpoint
 */
public class Mountpoint {

    /**
     * Mountpoint scrivibile
     */
    public static final int RW = 1;

    /**
     * Mountpoint sola lettura
     */
    public static final int RO = 2;

    private final String path;
    private String filesystem, altriParametri;
    private int stato;


    /**
     *
     * @param path Percorso del mountpoint
     * @param stato Una delle costanti RW o RO
     */
    protected Mountpoint(@NonNull String path, int stato){
        this.path = path;
        setStato(stato);
    }


    /**
     * Restituisce il percorso del mountpoint
     * @return Percorso del mountpoint
     */
    public String getPath() {
        return path;
    }


    /**
     * Restituisce lo stato del mountpoint
     * @return Una delle costanti RW o RO
     */
    public int getStato() {
        return stato;
    }


    /**
     * Imposta lo stato del mouuntpoint
     * @param stato Una delle costanti RW o RO
     */
    protected void setStato(int stato) {
        this.stato = stato;
    }


    /**
     * Restituisce il file system del mountpoint
     * @return File system del mountpoint
     */
    public String getFilesystem() {
        return filesystem;
    }


    /**
     * Imposta il file system del mountpoint
     * @param filesystem File system del mountpoint
     */
    protected void setFilesystem(String filesystem) {
        this.filesystem = filesystem;
    }


    /**
     * Imposta gli altri parametri letti dal mount
     * @param altriParametri Stringa che contiene tutti gli altri parametri (escluso ro o rw)
     */
    protected void setAltriParametri(String altriParametri) {
        this.altriParametri = altriParametri;
    }


    /**
     * Restituisce lo stato sottoforma di stringa
     * @return Stringa con "RO" o "RW". Null se lo stato non è stato impostato.
     */
    public String getStatoString(){
        switch (getStato()) {
            case RO:
                return "RO";
            case RW:
                return "RW";
            default:
                return null;
        }
    }


    /**
     * Restituisce una map che contiene tutti gli altri parametri già divisi sotto forma di chiave-valore
     * @return Map con gli altri parametri del mountpoint
     */
    public Map<String, String> getMapAltriParametri(){
        final Map<String, String> map = new LinkedHashMap<>();
        final String PARAMS = "Params:";
        if(!altriParametri.contains(",")){
            map.put(PARAMS, altriParametri);
        } else {
            final String[] split = altriParametri.split(",");
            final List<String> params = new ArrayList<>();
            for(String string : split){
                if(string.contains("=")){
                    final String[] keyValue = string.split("=");
                    map.put(String.format("%s%s", keyValue[0], ":"), keyValue[1]);
                } else {
                    params.add(string);
                }
            }
            if(!params.isEmpty()){
                final StringBuilder sb = new StringBuilder();
                for(int i=0; i < params.size(); i++){
                    sb.append(params.get(i));
                    if(i != params.size()-1){
                        sb.append(", ");
                    }
                }
                map.put(PARAMS, sb.toString());
            }
        }
        return map;
    }
}
