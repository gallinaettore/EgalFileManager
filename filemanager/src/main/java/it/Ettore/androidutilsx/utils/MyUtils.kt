package it.Ettore.androidutilsx.utils

import android.content.Context
import it.Ettore.egalfilemanager.R
import java.io.File
import java.util.*


object MyUtils {

    @JvmStatic
    fun deleteFile(file: File?): Boolean {
        if (file == null) return false
        if (file.isDirectory) {
            val children = file.list() ?: emptyArray()
            for (fileCorrente in children) {
                val success = deleteFile(File(file, fileCorrente))
                if (!success) {
                    return false
                }
            }
        }
        return try {
            file.delete()
        } catch (e: SecurityException) {
            false
        }
    }


    @JvmStatic
    fun aggiungi2Punti(str: String, context: Context): String {
        return String.format("%s%s", str, context.getString(R.string.punt_colon))
    }


    @JvmStatic
    fun getRandomString(size: Int): String {
        val candidateChars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        val random = Random()
        for (i in 0 until size) {
            sb.append(candidateChars[random.nextInt(candidateChars.length)])
        }
        return sb.toString()
    }


    @JvmStatic
    fun stringResToStringArray(context: Context, arrayIds: IntArray?): Array<String> {
        if (arrayIds == null) return emptyArray()
        return arrayIds.map { context.getString(it) }.toTypedArray()
    }

}