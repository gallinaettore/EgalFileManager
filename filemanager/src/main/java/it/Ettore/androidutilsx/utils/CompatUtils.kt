package it.Ettore.androidutilsx.utils

import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
@Suppress("DEPRECATION")
object CompatUtils {

    @JvmStatic
    fun fromHtml(html: String?): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }


    /**
     * Imposta la trasparenza della view
     * @param imageView ImageView
     * @param alpha 0 completamente trasparente, 255 completamente opaco
     */
    @JvmStatic
    fun setAlpha(imageView: ImageView, alpha: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageView.imageAlpha = alpha
        } else {
            imageView.setAlpha(alpha)
        }
    }
}