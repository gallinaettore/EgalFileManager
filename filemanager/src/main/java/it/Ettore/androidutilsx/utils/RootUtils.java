package it.Ettore.androidutilsx.utils;



import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Classe di utilità per la gestione dei permessi di root
 */
public class RootUtils {


    /**
     * Chiede i permessi di root e verifica se il dispositivo è rootato
     * @return True se il dispositivo è rootato
     */
    public static boolean isPhoneRooted(){
        boolean rooted = false;
        try{
            Runtime.getRuntime().exec("su");
            rooted = true;
        } catch (Exception ignored) {}
        return rooted;
    }


    /**
     * Invia una serie di comandi da eseguire. Nei risultati mostra anche lo stream con gli errori.
     * @param commands Lista di comandi da eseguire
     * @return Lista di risultati (contentente anche gli errori se si verificano)
     */
    public static List<String> sendCommands(String... commands){
        return sendCommands(true, commands);
    }


    /**
     * Invia una serie di comandi da eseguire
     * @param includeErrorStream TRue se nei risultati si vogliono includere anche eventuali errori accaduti
     * @param commands Lista di comandi da eseguire
     * @return Lista di risultati
     */
    public static List<String> sendCommands(boolean includeErrorStream, String... commands){
        final List<String> stdout = new ArrayList<>();
        DataOutputStream outputStream = null;
        BufferedReader inStream = null;
        BufferedReader errStream = null;
        try{
            final Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            inStream = new BufferedReader(new InputStreamReader(su.getInputStream(), "UTF-8"));
            errStream = new BufferedReader(new InputStreamReader(su.getErrorStream(), "UTF-8"));

            for(String command : commands){
                command += "\n";
                byte[] bytes = command.getBytes("UTF-8");
                outputStream.write(bytes);
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //raccogli i dati dopo che ogni operazione è terminata (wait for)
            try {
                String inLine;
                while ((inLine = inStream.readLine()) != null) {
                    stdout.add(inLine);
                }
            } catch (IOException ignored) {}

            if(includeErrorStream) {
                try {
                    String errLine;
                    while ((errLine = errStream.readLine()) != null) {
                        stdout.add(errLine);
                    }
                } catch (IOException ignored) {}
            }

        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try{
                outputStream.close();
            } catch (Exception ignored){}
            try{
                inStream.close();
            } catch (Exception ignored){}
            try{
                errStream.close();
            } catch (Exception ignored){}
        }
        return stdout;
    }
}
