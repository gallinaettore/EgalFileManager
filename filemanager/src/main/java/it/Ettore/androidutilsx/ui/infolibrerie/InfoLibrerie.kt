package it.Ettore.androidutilsx.ui.infolibrerie

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import it.Ettore.androidutilsx.utils.CompatUtils.fromHtml
import it.Ettore.androidutilsx.utils.DeviceUtils
import it.Ettore.androidutilsx.utils.MyUtils
import it.Ettore.egalfilemanager.R

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
class InfoLibrerie(private val context: Context) {
    private val listaLibrerie = mutableListOf<Libreria>()
    var isClickableLink = true
    var resIdSelector = 0 //selector da applicare alla TextView "License"


    fun aggiungiLibreria(libreria: Libreria) {
        listaLibrerie.add(libreria)
    }


    fun creaLayout(layout: LinearLayout) {
        for (libreria in listaLibrerie) {
            val rigaLibreria = LayoutInflater.from(context).inflate(R.layout.riga_libreria, layout, false)
            val nomeTextView = rigaLibreria.findViewById<TextView>(R.id.nomeTextView)
            nomeTextView.text = MyUtils.aggiungi2Punti(libreria.nome, context)
            val urlTextView = rigaLibreria.findViewById<TextView>(R.id.urlTextView)
            if (isClickableLink) {
                urlTextView.movementMethod = LinkMovementMethod.getInstance()
                val testoUrl = libreria.testoUrl ?: libreria.url
                val link = String.format("<a href=\"%s\">%s</a>", libreria.url, testoUrl)
                urlTextView.text = fromHtml(link)
            } else {
                urlTextView.text = libreria.url
            }
            val licenzaTextView = rigaLibreria.findViewById<TextView>(R.id.licenzaTextView)
            if (libreria.hasLicense) {
                val textLicense = "License"
                val content = SpannableString(textLicense)
                content.setSpan(UnderlineSpan(), 0, textLicense.length, 0)
                licenzaTextView.text = content
                licenzaTextView.setOnClickListener { creaDialog(libreria)?.show() }
            } else {
                licenzaTextView.visibility = View.GONE
            }
            if (resIdSelector != 0) {
                licenzaTextView.setBackgroundResource(resIdSelector)
            }
            layout.addView(rigaLibreria)
        }
    }

    private fun creaDialog(libreria: Libreria): AlertDialog? {
        return try {
            val webView = WebView(context)
            webView.setBackgroundColor(Color.TRANSPARENT)
            if (DeviceUtils.getApiLevel() >= 11) {
                LayerTypeHelper().setLayerTypeSoftware(webView)
            }
            val license: String = libreria.getLicense(context) ?: return null
            webView.loadDataWithBaseURL(null, license, "text/html", "UTF-8", null)

            // Dialog bianca con scritte nere, sia per tema scuso che per tema light
            val builder = AlertDialog.Builder(context, R.style.DialogCreditLicense)
            builder.setView(webView)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, null)
            builder.create()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    @SuppressLint("NewApi")
    private inner class LayerTypeHelper {
        fun setLayerTypeSoftware(view: View) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }
}