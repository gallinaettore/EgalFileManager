package it.Ettore.egalfilemanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import it.Ettore.egalfilemanager.Costanti
import it.Ettore.egalfilemanager.R
import it.Ettore.egalfilemanager.activity.ActivityMain
import it.Ettore.egalfilemanager.iconmanager.IconManager
import java.io.File


class MyWidgetManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val remoteViews = RemoteViews(context.packageName, R.layout.widget)


    fun updateWidget(appWidgetId: Int, file: File?) {
        buildRemoteViews(appWidgetId, file)
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }


    private fun buildRemoteViews(appWidgetId: Int, file: File?) {
        val text = if(file != null) file.name else "Not found!"
        remoteViews.setTextViewText(R.id.widget_textview, text)
        if (file == null) return

        if(file.isDirectory){
            remoteViews.setImageViewResource(R.id.widget_imageview, R.drawable.ico_cartella)
        } else {
            remoteViews.setImageViewResource(R.id.widget_imageview, R.drawable.ico_file)
            val imageSizePx = 150
            val appWidgetTarget = AppWidgetTarget(context, R.id.widget_imageview, remoteViews, appWidgetId)
            IconManager.showImageOnWidgetWithGlide(context, appWidgetTarget, file, imageSizePx, imageSizePx)
        }

        val intent = Intent(context, ActivityMain::class.java)
        intent.apply {
            intent.action = Costanti.ACTION_FRAGMENT_FILE_EXPL
            intent.putExtra(Costanti.KEY_BUNDLE_DIRECTORY_TO_SHOW, file.absolutePath)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP //Dopo il riavvio i widget multipli funzionano correttamente
        }
        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_frame, pendingIntent)
    }


    fun saveToPrefs(appWidgetId: Int, file: File) {
        prefs.edit().apply {
            putString(keyPrefFile(appWidgetId), file.path)
        }.apply()
    }


    fun readFromPrefs(appWidgetId: Int): File? {
        val filePath = prefs.getString(keyPrefFile(appWidgetId), null)
        return if(filePath != null) File(filePath) else null
    }



    fun deleteFromPrefs(appWidgetId: Int) {
        prefs.edit().apply {
            remove(keyPrefFile(appWidgetId))
        }.apply()
    }


    private fun keyPrefFile(appWidgetId: Int) = "$SUFFISSO_FILE$appWidgetId"


    fun addWidgetToHome(file: File?) {
        if(file != null && isRequestPinAppWidgetSupported) {
            // Create the PendingIntent object only if your app needs to be notified that the user allowed the widget to be pinned.
            // Note that, if the pinning operation fails, your app isn't notified.
            val pinnedWidgetCallbackIntent = Intent(context, WidgetPinnedReceiver::class.java)
            val bundle = Bundle().apply {
                putString(WidgetPinnedReceiver.KEY_BUNDLE_FILE_PATH, file.absolutePath)
            }
            pinnedWidgetCallbackIntent.putExtras(bundle)
            // Configure the intent so that your app's broadcast receiver gets the callback successfully.
            // This callback receives the ID of the newly-pinned widget (EXTRA_APPWIDGET_ID).
            val successCallback = PendingIntent.getBroadcast(context, 0, pinnedWidgetCallbackIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val widgetProvider = ComponentName(context, Widget::class.java)
            appWidgetManager.requestPinAppWidget(widgetProvider, null, successCallback)
        }
    }


    val isRequestPinAppWidgetSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported



    companion object {
        private const val PREFS_WIDGET = "widget_settings"
        private const val SUFFISSO_FILE = "file_"
    }
}