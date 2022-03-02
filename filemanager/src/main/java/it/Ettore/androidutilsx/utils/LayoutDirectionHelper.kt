package it.Ettore.androidutilsx.utils

import android.content.Context
import android.os.Build
import android.view.View

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


object LayoutDirectionHelper {

    @JvmStatic
    fun isRightToLeft(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        } else {
            false
        }
    }
}