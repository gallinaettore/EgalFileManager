package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import androidx.annotation.NonNull;


/**
 * OutputStream su un file con permessi di root
 */
public class RootFileOutputStream extends OutputStream {
    private final static int IN_STREAM_BUF_LEN = 1024;
    private File file;
    private Process process;
    private DataOutputStream outputStream;
    private InputStream inStream, errStream;


    /**
     *
     * @param file File su cui aprire l'output stream
     * @throws IOException Eccezione lanciata in caso di errore
     */
    public RootFileOutputStream(File file) throws IOException {
        this.file = file;
        process = Runtime.getRuntime().exec("su");
        outputStream = new DataOutputStream(process.getOutputStream());
        inStream = process.getInputStream();
        errStream = process.getErrorStream();

        //crea un file vuoto o se il file esiste già lo svuota
        final String comandoSvuotaFile = "> \"" + file.getAbsolutePath() + "\"\n";
        sendCommand(comandoSvuotaFile);
    }


    /**
     * Aggiunge un byte sul file
     * @param n Byte
     * @throws IOException Eccezione lanciata in caso di errore
     */
    @Override
    public void write(int n) throws IOException {
        final String command = creaComandoPutBytes(creaByteString(n));
        sendCommand(command);
    }


    /**
     * Aggiunge un insieme di bytes (buffer) sul file
     * @param buffer Insieme di bytes. Non usare buffer di dimensioni superiori a 1024 bytes
     * @param off Offset (passare sempre 0 perchè altri valori non sono gestiti)
     * @param len Lunghezza del buffer
     * @throws IOException Eccezione lanciata in caso di errore
     */
    @Override
    public void write(@NonNull byte[] buffer, int off, int len) throws IOException {
        if (buffer == null) {
            throw new NullPointerException();
        } else if (off != 0) {
            throw new IllegalArgumentException("Non sono gestiti offset diversi da 0");
        } else if (len == 0) {
            return;
        }
        final StringBuilder allBytes = new StringBuilder(4 * len);
        for(int i=0; i < len; i++){
            allBytes.append(creaByteString(buffer[i]));
        }
        final String command = creaComandoPutBytes(allBytes.toString());
        sendCommand(command);
    }


    /**
     * Aggiunge un insieme di bytes (buffer) sul file
     * @param b Insieme di bytes. Non usare buffer di dimensioni superiori a 1024 bytes
     * @throws IOException Eccezione lanciata in caso di errore
     */
    @Override
    public void write(@NonNull byte[] b) throws IOException {
        write(b, 0, b.length);
    }


    /**
     * Converte un intero in una stringa esadecimale
     * @param n Byte
     * @return Byte in formato esadecimale
     */
    private String creaByteString(int n){
        return String.format(Locale.ENGLISH, "\\x%02X", (byte)n);
    }


    /**
     * Crea la stringa da inviare alla console per scrivere i bytes sul file
     * @param byteString Strimga che rappresenta il byte (o l'insieme di bytes) da scrivere su file
     * @return Comando per scrivere i bytes
     */
    private String creaComandoPutBytes(String byteString){
        return String.format(Locale.ENGLISH, "printf '%s' >> \"%s\"\n", byteString, file.getAbsolutePath());
    }


    /**
     * Invia un comando alla console
     * @param command Comando da inviare
     * @throws IOException Eccezione lanciata in caso di errore
     */
    private void sendCommand(String command) throws IOException {
        //invio il comando
        final byte[] bytes = command.getBytes("UTF-8");
        outputStream.write(bytes);
        outputStream.flush();

        //analizzo la risposta
        final String consoleOutput = getConsoleOutput(inStream);
        final String consoleError = getConsoleOutput(errStream);
        if(!consoleOutput.isEmpty() || !consoleError.isEmpty()){
            //lancio un'eccezione se la risposta non è vuota (errore trovato)
            throw new IOException(consoleOutput + " " + consoleError);
        }
    }


    /**
     * Chiude tutte le risorse aperte
     * @throws IOException Eccezione lanciata in caso di errore
     */
    @Override
    public void close() throws IOException {
        //termino la console
        outputStream.writeBytes("exit\n");
        outputStream.flush();

        //attendo che tutto sia completato
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        //chiudo gli streams
        outputStream.close();
        inStream.close();
        errStream.close();
    }


    /**
     * Ottiene la risposta da parte della console
     * @param is InputStream
     * @return Risposta da parte della console. Stringa vuota se non c'è risposta.
     * @throws IOException Eccezione lanciata in caso di errore
     */
    private String getConsoleOutput(@NonNull InputStream is) throws IOException {
        if(is.available() == 0){
            return "";
        }
        byte[] buffer = new byte[IN_STREAM_BUF_LEN];
        int read;
        final StringBuilder sb = new StringBuilder();
        while (true) {
            read = is.read(buffer);
            sb.append(new String(buffer, 0, read));
            if (read < IN_STREAM_BUF_LEN) {
                // we have read everything
                break;
            }
        }
        return sb.toString();
    }
}
