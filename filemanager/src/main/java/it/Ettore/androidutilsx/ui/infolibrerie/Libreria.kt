package it.Ettore.androidutilsx.ui.infolibrerie

import android.content.Context
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

class Libreria @JvmOverloads constructor(val nome: String, val url: String, @RawRes val resIdRawLicenza: Int = 0, val testoUrl: String? = null) {

    val hasLicense = resIdRawLicenza != 0


    fun getLicense(context: Context): String? {
        if (!hasLicense) return null
        var inputStream: InputStream? = null
        var inputreader: InputStreamReader? = null
        var buffreader: BufferedReader? = null
        var sb: StringBuilder?
        var line: String?
        try {
            inputStream = context.resources.openRawResource(resIdRawLicenza)
            inputreader = InputStreamReader(inputStream)
            buffreader = BufferedReader(inputreader)
            sb = StringBuilder("<tt>")
            while (buffreader.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("<br />")
            }
            sb.append("</tt>")
        } catch (e: IOException) {
            sb = null
            e.printStackTrace()
        } finally {
            try {
                buffreader?.close()
            } catch (ignored: Exception) { }
            try {
                inputreader?.close()
            } catch (ignored: Exception) { }
            try {
                inputStream?.close()
            } catch (ignored: Exception) { }
        }
        return sb?.toString()
    }
}