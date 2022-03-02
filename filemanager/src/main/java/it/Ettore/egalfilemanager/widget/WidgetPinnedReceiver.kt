package it.Ettore.egalfilemanager.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File


/**
 * Broadcast receiver chiamato quando il widget viene aggionto alla home
 */
class WidgetPinnedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: return
        val filePath = intent.getStringExtra(KEY_BUNDLE_FILE_PATH) ?: return
        val widgetManager = MyWidgetManager(context)
        val file = File(filePath)
        widgetManager.updateWidget(appWidgetId, file)
        widgetManager.saveToPrefs(appWidgetId, file)
    }


    companion object {
        const val KEY_BUNDLE_FILE_PATH = "file_path"
    }
}