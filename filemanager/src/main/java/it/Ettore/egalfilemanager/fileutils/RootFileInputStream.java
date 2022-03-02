package it.Ettore.egalfilemanager.fileutils;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;


/**
 * InputStream su un file con permessi di root
 */
public class RootFileInputStream extends InputStream {
    private InputStream inStream;


    /**
     *
     * @param file File su cui aprire l'input stream
     * @throws IOException Eccezione lanciata in caso di errore
     */
    public RootFileInputStream(File file) throws IOException {
        final Process process = Runtime.getRuntime().exec("su");
        final DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
        inStream = process.getInputStream();

        final String cmd = "cat \"" + file.getAbsolutePath() + "\"\n";
        outputStream.write(cmd.getBytes());
        outputStream.flush();
        outputStream.writeBytes("exit\n");
        outputStream.flush();
        outputStream.close();
    }


    /**
     * Chiude lo stream
     * @throws IOException Eccezione lanciata in caso di errore
     */
    @Override
    public void close() throws IOException {
        inStream.close();
    }


    /**
     * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.
     * If no byte is available because the end of the stream has been reached, the value -1 is returned.
     * This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
     * @return the next byte of data, or -1 if the end of the stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        return inStream.read();
    }


    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b.
     * The number of bytes actually read is returned as an integer. This method blocks until input data is available, end of file is detected, or an exception is thrown.
     * If the length of b is zero, then no bytes are read and 0 is returned; otherwise, there is an attempt to read at least one byte.
     * If no byte is available because the stream is at the end of the file, the value -1 is returned; otherwise, at least one byte is read and stored into b.
     * The first byte read is stored into element b[0], the next one into b[1], and so on. The number of bytes read is, at most, equal to the length of b.
     * Let k be the number of bytes actually read; these bytes will be stored in elements b[0] through b[k-1], leaving elements b[k] through b[b.length-1] unaffected.
     * The read(b) method for class InputStream has the same effect as:
     * read(b, 0, b.length)
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or -1 is there is no more data because the end of the stream has been reached.
     * @throws IOException IOException If the first byte cannot be read for any reason other than the end of the file, if the input stream has been closed, or if some other I/O error occurs.
       @throws java.lang.NullPointerException if b is null.
     */
    @Override
    public int read(@NonNull byte[] b) throws IOException {
        return inStream.read(b);
    }


    /**
     * Reads up to len bytes of data from the input stream into an array of bytes. An attempt is made to read as many as len bytes, but a smaller number may be read. The number of bytes actually read is returned as an integer.
     * This method blocks until input data is available, end of file is detected, or an exception is thrown.
     * If len is zero, then no bytes are read and 0 is returned; otherwise, there is an attempt to read at least one byte. If no byte is available because the stream is at end of file, the value -1 is returned; otherwise, at least one byte is read and stored into b.
     * The first byte read is stored into element b[off], the next one into b[off+1], and so on. The number of bytes read is, at most, equal to len. Let k be the number of bytes actually read; these bytes will be stored in elements b[off] through b[off+k-1], leaving elements b[off+k] through b[off+len-1] unaffected.
     * In every case, elements b[0] through b[off] and elements b[off+len] through b[b.length-1] are unaffected.
     * The read(b, off, len) method for class InputStream simply calls the method read() repeatedly. If the first such call results in an IOException, that exception is returned from the call to the read(b, off, len) method. If any subsequent call to read() results in a IOException, the exception is caught and treated as if it were end of file; the bytes read up to that point are stored into b and the number of bytes read before the exception occurred is returned. The default implementation of this method blocks until the requested amount of input data len has been read, end of file is detected, or an exception is thrown. Subclasses are encouraged to provide a more efficient implementation of this method.
     * @param b the buffer into which the data is read.
     * @param off the start offset in array b at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException IOException If the first byte cannot be read for any reason other than end of file, or if the input stream has been closed, or if some other I/O error occurs.
     * @throws java.lang.NullPointerException If b is null.
     * @throws java.lang.IndexOutOfBoundsException If off is negative, len is negative, or len is greater than b.length - off
     */
    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        return inStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return inStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inStream.available();
    }


    @Override
    public synchronized void mark(int readlimit) {
        inStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        inStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inStream.markSupported();
    }
}
