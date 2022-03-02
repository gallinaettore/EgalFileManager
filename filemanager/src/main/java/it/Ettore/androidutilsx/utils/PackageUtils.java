package it.Ettore.androidutilsx.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.core.content.pm.PackageInfoCompat;

import java.io.File;
import java.util.List;


public class PackageUtils {
	private final Context context;
	private final PackageManager pm;
	
	public PackageUtils(Context context){
		this.context = context;
		this.pm = context.getPackageManager();
	}
	

	public void restartCurrentPackage(){
        //Individuo il pid del processo FirebaseCrash
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        int pidFirebaseCrash = 0;
        for (int i = 0; i < processInfos.size(); i++) {
            final ActivityManager.RunningAppProcessInfo info = processInfos.get(i);
            final String firebaseCrashProcess = context.getPackageName() + ":background_crash";
            if(info.processName.equals(firebaseCrashProcess)){
                pidFirebaseCrash = info.pid;
                break;
            }
        }

        //Creo un pending intent per il nuovo avvio dell'app nel futuro
        final Intent mStartActivity = pm.getLaunchIntentForPackage(context.getPackageName());
        mStartActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final int mPendingIntentId = 1984;
        final PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        final AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

        //Termino il processo FirebaseCrash e poi l'applicazione
        if(pidFirebaseCrash != 0){
            android.os.Process.killProcess(pidFirebaseCrash);
        }

        terminaApp(0);
	}


	public void terminaApp(int exitCode){
		System.exit(exitCode);
	}


	/**
	 * Restituisce il codice della versione del package
	 * @param packagename Nome del packed
	 * @return Version code
	 * @throws NameNotFoundException Eccezione lanciata se non è stato trovato il package
	 */
	public int getVersionCode(String packagename) throws NameNotFoundException {
		return (int)(PackageInfoCompat.getLongVersionCode(pm.getPackageInfo(packagename, 0)));
	}



	/**
	 * Restituisce il nome della versione del package
	 * @param packageName Nome del package
	 * @return Nome versione
	 * @throws NameNotFoundException Eccezione lanciata se non è stato trovato il package
	 */
	public String getVersionName(String packageName) throws NameNotFoundException {
		return pm.getPackageInfo(packageName, 0).versionName;
	}

	
	//Cancella tutti i dati dell'app compresi cache, shared preferences, files, ecc..
	public void deleteAllAppData(){
		final File cache = context.getCacheDir();
		final File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			clearSharedPreferences(false);
			final String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));				
				}
			}
		}
	}

	
	private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            final String[] children = dir.list();
            for (String child : children){
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
	
	//Azzera tutte le shared preferences dell'app, true per cancellare anche i files
	public void clearSharedPreferences(boolean delete){
		final File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
	    final String[] listaFiles = dir.list();
	    for (String file : listaFiles) {
	        context.getSharedPreferences(file.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().commit();
	        if(delete) new File(dir, file).delete();   
	    }
	}
}
