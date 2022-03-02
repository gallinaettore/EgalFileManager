package it.Ettore.androidutilsx.ext

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import it.Ettore.androidutilsx.ui.ColoredToast


fun Activity.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()


fun Activity.toast(@StringRes resIsMessage: Int) = Toast.makeText(this, resIsMessage, Toast.LENGTH_LONG).show()


fun Activity.coloredToast(message: String) = ColoredToast.makeText(this, message, Toast.LENGTH_LONG).show()


fun Activity.coloredToast(@StringRes resIsMessage: Int) = ColoredToast.makeText(this, resIsMessage, Toast.LENGTH_LONG).show()

