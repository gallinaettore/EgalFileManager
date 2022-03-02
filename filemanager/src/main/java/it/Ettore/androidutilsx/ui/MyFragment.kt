package it.Ettore.androidutilsx.ui

import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import it.Ettore.androidutilsx.utils.ViewUtils


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

open class MyFragment : Fragment() {

    val prefs: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext())

    fun nascondiTastiera() = (activity as? MyActivity?)?.nascondiTastiera()


    fun simpleDialog(title: String?, message: String) {
        ViewUtils.simpleDialog(context, title, message)
    }


    fun simpleDialog(@StringRes resIdTitle: Int, @StringRes resIdMessage: Int) {
        ViewUtils.simpleDialog(context, getString(resIdTitle), getString(resIdMessage))
    }


    fun simpleDialog(@StringRes resIdTitle: Int, message: String) {
        ViewUtils.simpleDialog(context, getString(resIdTitle), message)
    }


    open fun settaTitolo(@StringRes resId: Int) {
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(resId)
    }


    open fun settaTitolo(titolo: String?) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = titolo
    }
}
