package it.Ettore.androidutilsx.utils;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    private IvParameterSpec ivspec;
    private SecretKeySpec keyspec;
    private Cipher cipher;


    public Crypt(String key) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if(key == null || key.length() > 16){
            throw new IllegalArgumentException("Invalid key. (Max key length = 16)");
        }
        final String iv = "587rxsanjzmnej00"; // sale (max 16 cifre)
        ivspec = new IvParameterSpec(iv.getBytes());
        keyspec = new SecretKeySpec(key.getBytes(), "AES");
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }


    public byte[] encryptToByte(String text) throws Exception {
        if (text == null || text.length() == 0) {
            throw new Exception("Empty string");
        }
        byte[] encrypted;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            encrypted = cipher.doFinal(padString(text).getBytes());
        } catch (Exception e) {
            throw new Exception("[encryptToByte] " + e.getMessage());
        }
        return encrypted;
    }


    public String encrypt(String text) throws Exception {
        return byteArrayToHexString(encryptToByte(text));
    }


    public byte[] decryptToByte(String text) throws Exception {
        if (text == null || text.length() == 0) {
            throw new Exception("Empty string");
        }
        byte[] decrypted;
        try {
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            decrypted = cipher.doFinal(hexToBytes(text));
        } catch (Exception e) {
            throw new Exception("[decryptToByte] " + e.getMessage());
        }
        return decrypted;
    }


    public String decrypt(String encryptedString) throws Exception {
        return new String(decryptToByte(encryptedString)).trim();
    }


    public static String byteArrayToHexString(byte[] array) {
        final StringBuilder hexString = new StringBuilder();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }


    public static byte[] hexToBytes(String str) {
        if (str == null) {
            return null;
        } else if (str.length() < 2) {
            return null;
        } else {

            int len = str.length() / 2;
            byte[] buffer = new byte[len];
            for (int i = 0; i < len; i++) {
                buffer[i] = (byte) Integer.parseInt(
                        str.substring(i * 2, i * 2 + 2), 16);

            }
            return buffer;
        }
    }


    private static String padString(String source) {
        char paddingChar = 0;
        int size = 16;
        int x = source.length() % size;
        int padLength = size - x;
        for (int i = 0; i < padLength; i++) {
            source += paddingChar;
        }
        return source;
    }
}
