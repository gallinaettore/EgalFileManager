package it.Ettore.egalfilemanager.widget


import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context


class Widget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val widgetManager = MyWidgetManager(context)

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (appWidgetId in appWidgetIds) {
            val element = widgetManager.readFromPrefs(appWidgetId)
            widgetManager.updateWidget(appWidgetId, element)
        }
    }


    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        //Cancellando il widget pulisco il file che conteneva le sue impostazioni
        super.onDeleted(context, appWidgetIds)
        val widgetManager = MyWidgetManager(context)
        for (appWidgetId in appWidgetIds) {
            widgetManager.deleteFromPrefs(appWidgetId)
        }
    }
}