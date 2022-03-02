package it.Ettore.androidutilsx.ext

import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import it.Ettore.egalfilemanager.R


private fun Spinner.popola(values: Array<String>, @LayoutRes resIdLayout: Int){
    var indiceDaSelezionare = -1
    selectedItem?.let {
        val oldSelectedValue = selectedItem.toString()
        for (i in values.indices) {
            if (values[i] == oldSelectedValue) {
                indiceDaSelezionare = i
                break
            }
        }
    }

    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(context, resIdLayout, values)
    spinnerArrayAdapter.setDropDownViewResource(R.layout.myspinner_dropdown)
    adapter = spinnerArrayAdapter

    if (indiceDaSelezionare >= 0) {
        setSelection(indiceDaSelezionare, true)
    }
}


fun Spinner.popola(vararg values: String){
    popola(arrayOf(*values), R.layout.myspinner)
}

fun Spinner.popola(values: List<String>){
    popola(values.toTypedArray(), R.layout.myspinner)
}


fun Spinner.popola(@StringRes vararg valuesIds: Int){
    val values: List<String> = valuesIds.map { context.getString(it) }
    popola(values.toTypedArray(), R.layout.myspinner)
}


fun ScrollView.scrollToTop() = post { smoothScrollTo(0, 0) }
