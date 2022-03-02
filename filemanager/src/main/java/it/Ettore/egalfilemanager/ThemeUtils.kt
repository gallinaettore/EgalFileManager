package it.Ettore.egalfilemanager


import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat

class ThemeUtils(val context: Context) {

    val primaryTextColor: Int  get() = obtainStyledAttribute(android.R.attr.textColorPrimary)


    val secondaryTextColor: Int  get() = obtainStyledAttribute(android.R.attr.textColorSecondary)


    val hiddenFileTextColor: Int  get() = ContextCompat.getColor(context, R.color.file_nascosto)


    @ColorInt
    private fun obtainStyledAttribute(@AttrRes attrResId: Int): Int {
        val color: Int
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attrResId, typedValue, true)
        val arr = context.obtainStyledAttributes(typedValue.data, intArrayOf(attrResId))
        color = arr.getColor(0, -1)
        arr.recycle()
        return color
    }
}