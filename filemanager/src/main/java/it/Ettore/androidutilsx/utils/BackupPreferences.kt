package it.Ettore.androidutilsx.utils

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import it.Ettore.egalfilemanager.R
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.lang.IllegalStateException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult









/**
 * Classe per la gestione dei backup delle preferences
 */
class BackupPreferences(private val context: Context) {
    private val dataFolder: File = context.cacheDir.parentFile
    private val sharedPrefsFolder: File
    private val filesFolder: File
    private val databasesFolder: File
    private val sharedPreferencesFiles: MutableSet<File>
    private val files: MutableSet<File>
    private val databases: Set<File>
    private val mapChiaviDaEscludere: MutableMap<String, Set<String>> //chiave: nome file, valore: chiavi da eliminare per quel file


    /**
     * Se impostato, in fase di ripristino del backup mostra un messaggio di errore se il backup è stato creato con una build inferirore a quella impostata
     */
    var minAppBuildVersion: Int? = null


    /**
     * Restituisce il nome del file preferences predefinito dell'applicazione
     * @return nomepackage_preferences.xml
     */
    val defaultPrefs: String
        get() = context.packageName + "_preferences"


    init {
        this.sharedPrefsFolder = File(dataFolder, FOLDER_SHARED_PREFS)
        this.filesFolder = File(dataFolder, FOLDER_FILES)
        this.databasesFolder = File(dataFolder, FOLDER_DATABASES)
        this.sharedPreferencesFiles = HashSet()
        this.files = HashSet()
        this.databases = HashSet()
        this.mapChiaviDaEscludere = HashMap()
    }


    /**
     * Imposta i nomi di files che si trovano nella cartella "shared_prefs" di cui fare il backup
     * @param files Nomi dei files da aggiungere (i nomi non devono avere l'estenzione finale .xml, sarà aggiunta automaticamente)
     */
    fun addSharedPreferencesFiles(vararg files: String) {
        for (nomeFile in files) {
            val file = File(sharedPrefsFolder, "$nomeFile.xml")
            if (file.exists()) {
                sharedPreferencesFiles.add(file)
            } else {
                Log.w(TAG, "File \"" + file.absolutePath + "\" non trovato!")
            }
        }
    }


    /**
     * Imposta le chiavi xml da non includere nel backup
     * @param fileName Nome del file xml in cui sono presenti le chiavi da escludere (il nome del file deve essere privo di estenzione)
     * @param keys Chiavi da escludere dal backup
     */
    fun addKeyToExclude(fileName: String, vararg keys: String) {
        var chiaviDelFile = mapChiaviDaEscludere[fileName]?.toMutableSet()
        if (chiaviDelFile == null) {
            chiaviDelFile = mutableSetOf()
        }
        Collections.addAll(chiaviDelFile, *keys)
        mapChiaviDaEscludere[fileName] = chiaviDelFile
    }


    /**
     * Effettua il backup su un file zip
     * @param destinationFile Percorso del file zip in cui eseguire il backup
     */
    fun performBackup(destinationFile: File) {
        try {
            val outputStream = FileOutputStream(destinationFile)
            performBackup(outputStream)
        } catch (e: FileNotFoundException){
            //il file system in cui si vuole creare il file potrebbe essere di sola lettura
            Toast.makeText(context, R.string.errore_creazione_backup, Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Effettua il backup su un file zip
     * @param uriBackupFile Uri del file zip in cui eseguire il backup (ottenuto tramite SAF)
     */
    fun performBackup(uriBackupFile: Uri?) {
        if(uriBackupFile == null){
            Toast.makeText(context, R.string.errore_creazione_backup, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val outputStream = context.contentResolver.openOutputStream(uriBackupFile)
            performBackup(outputStream)
        } catch (e: FileNotFoundException){
            Toast.makeText(context, R.string.errore_creazione_backup, Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Effettua il backup su un file zip
     * @param outputStream OutputStream del file di destinazione
     */
    private fun performBackup(outputStream: OutputStream?){
        val allFilesToBackup = HashSet<File>()
        allFilesToBackup.addAll(sharedPreferencesFiles)
        allFilesToBackup.addAll(files)
        allFilesToBackup.addAll(databases)
        val success = createZipArchive(outputStream, allFilesToBackup)
        if (success) {
            Toast.makeText(context, R.string.backup_effettuato_con_successo, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, R.string.errore_creazione_backup, Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Crea il file zip che contiene il backup di tutte le prefs
     * @param outpuStreamZipFile OutputStream del file di destinazione
     * @param filesDaInserire Lista di files da inserire all'interno del backup
     */
    private fun createZipArchive(outpuStreamZipFile: OutputStream?, filesDaInserire: Set<File>): Boolean {
        if(outpuStreamZipFile == null) return false
        var zos: ZipOutputStream? = null
        var inputStream: InputStream? = null
        var success = false
        try {
            zos = ZipOutputStream(outpuStreamZipFile)

            //processo il json
            val json = creaManifestJson()
            inputStream = ByteArrayInputStream(json.toString().toByteArray())
            zos.putNextEntry(ZipEntry(MANIFEST_JSON))
            val data = ByteArray(ZIP_BUFFER_SIZE)
            var count: Int
            do {
                count = inputStream.read(data, 0, ZIP_BUFFER_SIZE)
                if(count == -1) break
                zos.write(data, 0, count)
                zos.flush()
            } while (true)
            zos.closeEntry()
            inputStream.close()

            //processo tutti i files delle preferences
            for (file in filesDaInserire) {
                val nomeFilePulito = file.name.replace(".xml", "")
                if (mapChiaviDaEscludere.containsKey(nomeFilePulito)) {
                    val chiaviDaEscludere = mapChiaviDaEscludere[nomeFilePulito]
                    addToZipFileRemovingKey(file, zos, chiaviDaEscludere!!)
                } else {
                    addToZipFile(file, zos)
                }
            }
            success = true

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                zos?.close()
            } catch (ignored: Exception) {
            }

            try {
                inputStream?.close()
            } catch (ignored: Exception) { }

        }

        return success
    }



    /**
     * Aggiunge un file all'archivio zip
     * @param file File da aggiungere
     * @param zos OutputStream dello zip
     */
    private fun addToZipFile(file: File, zos: ZipOutputStream) {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            val relativePath = getEntryRelativePath(file)
            zos.putNextEntry(ZipEntry(relativePath))

            val bytes = ByteArray(ZIP_BUFFER_SIZE)
            var length: Int
            do {
                length = fis.read(bytes)
                if(length < 0){
                    break
                }
                zos.write(bytes, 0, length)
            } while (true)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                zos.closeEntry()
            } catch (ignored: Exception) { }

            try {
                fis?.close()
            } catch (ignored: Exception) { }

        }
    }


    /**
     * Aggiunge un file xml all'archivio zip rimuovendo le chiavi di cui non bisogna fare un backup
     * @param file File da aggiungere
     * @param zos OutputStream dello zip
     * @param keys Chiavi da eliminare
     */
    private fun addToZipFileRemovingKey(file: File, zos: ZipOutputStream, keys: Set<String>) {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isValidating = false
        val db: DocumentBuilder
        try {
            db = dbf.newDocumentBuilder()
            val doc = db.parse(FileInputStream(file))
            // retrieve the element
            val elementi = doc.getElementsByTagName("*")
            for (i in 0 until elementi.length) {
                val node = elementi.item(i)
                if (node is Element) {
                    if (node.hasAttribute("name") && keys.contains(node.getAttribute("name"))) {
                        //rimuove lo spazio vuoto che rimane quando si elimina un nodo
                        val prevElem = node.previousSibling
                        if (prevElem != null && prevElem.nodeType == Node.TEXT_NODE && prevElem.nodeValue.trim().isEmpty()) {
                            node.parentNode.removeChild(prevElem)
                        }
                        // remove the specific node
                        node.parentNode.removeChild(node)
                    }
                }
            }
            // Normalize the DOM tree, puts all text nodes in the full depth of the sub-tree underneath this node
            doc.normalize()
            // Print
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            val relativePath = getEntryRelativePath(file)
            zos.putNextEntry(ZipEntry(relativePath))
            val result = StreamResult(zos)
            transformer.transform(DOMSource(doc), result)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                zos.closeEntry()
            } catch (ignored: Exception) { }
        }
    }


    /**
     * Crea il percorso relativo da usare all'interno del file zip
     * @param file File originale
     * @return Percorso usato nell'entry del file zip (tipo nomecartella/nomealtracartella/nomefile.ext)
     */
    private fun getEntryRelativePath(file: File): String {
        return file.absolutePath.replace(dataFolder.absolutePath, "").substring(1) //ignora il primo slash
    }


    /**
     * Crea il Json che contiene i dati generali del backup
     * @return Json che contiene i dati generali del backup
     * @throws JSONException .
     * @throws PackageManager.NameNotFoundException .
     */
    @Throws(JSONException::class, PackageManager.NameNotFoundException::class)
    private fun creaManifestJson(): JSONObject {
        val packageUtils = PackageUtils(context)
        return JSONObject().apply {
            put(JSON_KEY_PACKAGE, context.packageName)
            put(JSON_KEY_MANIFEST_VERSION, 1)
            put(JSON_KEY_APP_VERSION, packageUtils.getVersionName(context.packageName))
            put(JSON_KEY_APP_BUILD, packageUtils.getVersionCode(context.packageName))
            put(JSON_KEY_DATE, System.currentTimeMillis())
        }
    }


    /**
     * Ripristina il backup da un file zip. Se il file è valido viene mostrata una dialog riepilogativa
     * @param backupFile File zip contenente il backup
     */
    fun restoreBackup(backupFile: File) {
        var manifest: JSONObject? = null
        try {
            manifest = getManifest(FileInputStream(backupFile))
        } catch (e: FileNotFoundException){
            e.printStackTrace()
        }
        if (manifest != null) {
            showManifestDialog(manifest, FileInputStream(backupFile)) //riapro l'input stream perchè dopo l'utilizzo viene chiuso
        } else {
            Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Ripristina il backup da un file zip. Se il file è valido viene mostrata una dialog riepilogativa
     * @param uriBackupFile Uri del file zip contenente il backup (ottenuto tramite SAF)
     */
    fun restoreBackup(uriBackupFile: Uri?) {
        if(uriBackupFile == null){
            Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
            return
        }
        var inputStream : InputStream?
        try {
            inputStream = context.contentResolver.openInputStream(uriBackupFile)
        } catch (e: FileNotFoundException){
            Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
            return
        }
        val manifest = getManifest(inputStream)
        inputStream = try {
            context.contentResolver.openInputStream(uriBackupFile)//riapro l'input stream perchè dopo l'utilizzo viene chiuso
        } catch (e: FileNotFoundException){
            null
        }
        if (manifest != null && inputStream != null) {
            if(minAppBuildVersion != null && manifest.getInt(JSON_KEY_APP_BUILD) < minAppBuildVersion!!){
                showBuildNotSupportedDialog()
            } else {
                showManifestDialog(manifest, inputStream)
            }
        } else {
            Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
        }
    }


    private fun showBuildNotSupportedDialog() {
        AlertDialog.Builder(context).apply {
            setMessage(R.string.backup_creato_con_versione_non_compatibile)
            setPositiveButton(android.R.string.ok, null)
        }.create().show()
    }


    /**
     * Estrae il manifest dal file di backup
     * @param inputStreamBackupFile Stream del file di backup
     * @return Json con il manifest. Null in caso di errore
     */
    private fun getManifest(inputStreamBackupFile: InputStream?): JSONObject? {
        if(inputStreamBackupFile == null) return null
        var zipInputStream: ZipInputStream? = null
        val fos: OutputStream? = null
        val bos: BufferedOutputStream? = null
        var jsonObject: JSONObject? = null

        try {
            zipInputStream = ZipInputStream(inputStreamBackupFile)
            var entry: ZipEntry?
            do {
                entry = zipInputStream.nextEntry
                if(entry == null){
                    break
                }
                val name = validateZipEntryFilename(entry.name, ".")
                if (name.endsWith(MANIFEST_JSON)) {
                    val sb = StringBuilder()
                    val buffer = ByteArray(ZIP_BUFFER_SIZE)
                    do {
                        val i = zipInputStream.read(buffer)
                        if(i == -1) break
                        sb.append(String(buffer, 0, i))
                    } while (true)
                    jsonObject = JSONObject(sb.toString())
                }
                zipInputStream.closeEntry()
            } while (true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bos?.close()
            } catch (ignored: Exception) { }

            try {
                fos?.close()
            } catch (ignored: Exception) { }

            try {
                zipInputStream?.close()
            } catch (ignored: Exception) { }

        }
        return jsonObject
    }


    @Throws(IOException::class)
    private fun validateZipEntryFilename(filename: String, intendedDir: String): String {
        val f = File(filename)
        val canonicalPath = f.canonicalPath
        val iD = File(intendedDir)
        val canonicalID = iD.canonicalPath
        return if (canonicalPath.startsWith(canonicalID)) {
            canonicalPath
        } else {
            throw IllegalStateException("File is outside extraction target directory.")
        }
    }



    /**
     * Mostra una dialog riepilogativa se il manifest è corretto
     * @param json Json che contiene il manifest
     * @param inputStreamBackupFile Stream del file di backup
     */
    private fun showManifestDialog(json: JSONObject, inputStreamBackupFile: InputStream) {
        val builder = AlertDialog.Builder(context)
        try {
            if (json.getString(JSON_KEY_PACKAGE) == context.packageName) {
                //mostro la dialog se non ci sono errori e il package del backup corrisponde al package dell'app
                val message = "\n" + JSON_KEY_PACKAGE + ":  " + json.getString(JSON_KEY_PACKAGE) + "\n" +
                        JSON_KEY_MANIFEST_VERSION + ":  " + json.getInt(JSON_KEY_MANIFEST_VERSION).toString() + "\n" +
                        JSON_KEY_APP_VERSION + ":  " + json.getString(JSON_KEY_APP_VERSION) + "\n" +
                        JSON_KEY_APP_BUILD + ":  " + json.getInt(JSON_KEY_APP_BUILD).toString() + "\n" +
                        JSON_KEY_DATE + ":  " + DateFormat.getDateTimeInstance().format(json.getLong(JSON_KEY_DATE)) + "\n"
                builder.setMessage(message)
                builder.setTitle(R.string.ripristina_backup_impostazioni)
                builder.setPositiveButton(R.string.ripristina_backup) { _, _ ->
                    //avvio il ripristino dei dati
                    val success = ripristinaTuttiIFilesDiBackup(inputStreamBackupFile)
                    if (success) {
                        mostraDialogRipristinoEseguito()
                    } else {
                        Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
                    }
                }
                builder.setNegativeButton(android.R.string.cancel, null)
                builder.create().show()
            } else {
                Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
            }
        } catch (e: JSONException) {
            Toast.makeText(context, R.string.errore_ripristino_backup, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

    }


    /**
     * Mostra la dialog "Ripristino eseguito" e riavvia l'app al termine
     */
    private fun mostraDialogRipristinoEseguito() {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.ripristino_effettuato_con_successo)
        builder.setPositiveButton(android.R.string.ok) { _, _ -> PackageUtils(context).restartCurrentPackage() }
        builder.create().show()
    }


    /**
     * Copia i files presenti nel backup all'interno della cartella dati dell'applicazione
     * @param inputStreamBackupFile Stream del file che contiene i backup
     * @return True se il ripristino va a buon fine
     */
    private fun ripristinaTuttiIFilesDiBackup(inputStreamBackupFile: InputStream): Boolean {
        var zipInputStream: ZipInputStream? = null
        var fos: OutputStream? = null
        var bos: BufferedOutputStream? = null
        var success = false

        try {
            zipInputStream = ZipInputStream(inputStreamBackupFile)
            var entry: ZipEntry?

            do {
                entry = zipInputStream.nextEntry
                if(entry == null) {
                    break
                }
                val name: String = validateZipEntryFilename(entry.name, ".")
                if (!name.endsWith(MANIFEST_JSON) && !entry.isDirectory) {
                    var pathCartella: String? = null
                    val nomeFile: String
                    if (name.contains("/")) {
                        val lastSlashIndex = name.lastIndexOf("/")
                        pathCartella = name.substring(0, lastSlashIndex + 1)
                        nomeFile = name.substring(lastSlashIndex + 1)
                    } else {
                        nomeFile = name
                    }

                    //creo la cartella se necessaria
                    var cartellaDestinazione = dataFolder
                    if (pathCartella != null) {
                        cartellaDestinazione = File(dataFolder, pathCartella)
                        if (!cartellaDestinazione.exists()) {

                            cartellaDestinazione.mkdirs()
                        }
                    }

                    //estraggo il file
                    fos = FileOutputStream(File(cartellaDestinazione, nomeFile))
                    bos = BufferedOutputStream(fos, ZIP_BUFFER_SIZE)
                    var b: Int
                    val buffer = ByteArray(ZIP_BUFFER_SIZE)
                    do {
                        b = zipInputStream.read(buffer, 0, ZIP_BUFFER_SIZE)
                        if(b == -1) break
                        bos.write(buffer, 0, b)
                    } while (true)
                    bos.flush()
                    bos.close()
                    zipInputStream.closeEntry()
                }
            } while (true)

            success = true

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bos?.close()
            } catch (ignored: Exception) { }

            try {
                fos?.close()
            } catch (ignored: Exception) { }

            try {
                zipInputStream?.close()
            } catch (ignored: Exception) { }

        }

        return success
    }



    companion object {
        private val TAG = BackupPreferences::class.java.simpleName
        private const val FOLDER_SHARED_PREFS = "shared_prefs"
        private const val FOLDER_FILES = "files"
        private const val FOLDER_DATABASES = "databases"

        private const val MANIFEST_JSON = "manifest.json"
        private const val JSON_KEY_PACKAGE = "Package"
        private const val JSON_KEY_MANIFEST_VERSION = "Manifest version"
        private const val JSON_KEY_APP_BUILD = "App build"
        private const val JSON_KEY_APP_VERSION = "App version"
        private const val JSON_KEY_DATE = "Date"
        private const val ZIP_BUFFER_SIZE = 1024


        /**
         * Crea il nome del file di destinazione del backup
         * @param appName Nome dell'applicazione (incluso nel nome del file
         * @return Nome del file ne formato Nome App_settings_data.zip
         */
        fun createSettingsFileName(appName: String): String {
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
            val dateString = dateFormat.format(now)
            return "$appName - Settings - $dateString.zip"
        }
    }
}
