package it.Ettore.androidutilsx.utils

import android.R
import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/



/**
 * Classe di utilità di gestione delle views
 */
object ViewUtils {

    /**
     * Sposta il cursore alla fine di ogni EditText
     * @param editTexts EditText
     */
    @JvmStatic
    fun cursoreAllaFine(vararg editTexts: EditText?) {
        for (editText in editTexts) {
            editText?.setSelection(editText.text.toString().length)
        }
    }


    /**
     * Imposta il testo scorrevole nelle TextView
     * @param textViews TextViews
     */
    @JvmStatic
    fun aggiungiMarqueeAlleTextView(vararg textViews: TextView) {
        for (tv in textViews) {
            tv.isSelected = true
            tv.ellipsize = TextUtils.TruncateAt.MARQUEE
            tv.setSingleLine()
            tv.marqueeRepeatLimit = -1
            tv.setHorizontallyScrolling(true)
            tv.isFocusableInTouchMode = true
        }
    }


    /**
     * Restituisce la view dell'action bar
     * @param activity Activity
     * @return View dell'Action Bar, null se non è possibile trovare l'action bar
     */
    @JvmStatic
    fun getActionBarView(activity: Activity): ViewGroup? {
        return getActionBar(activity.window.decorView)
    }


    /**
     * Restituisce la view dell'action bar
     * @param view View in cui è presente l'action bar
     * @return View dell'Action Bar, null se non è possibile trovare l'action bar
     */
    private fun getActionBar(view: View): ViewGroup? {
        try {
            if (view is ViewGroup) {
                if (view is Toolbar) {
                    return view
                }
                for (i in 0 until view.childCount) {
                    val actionBar = getActionBar(view.getChildAt(i))
                    if (actionBar != null) {
                        return actionBar
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return null
    }


    @JvmStatic
    fun simpleDialog(context: Context?, title: String?, message: String) {
        if (context == null) return
        try {
            //è possibile chiamare la dialog da un thread quando l'activity è già chiusa
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(R.string.ok, null)
            builder.create().show()
        } catch (ignored: Exception) {
        }
    }
}