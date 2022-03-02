package it.Ettore.egalfilemanager.activity


import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import it.Ettore.egalfilemanager.PermissionsManager
import it.Ettore.egalfilemanager.R
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder.DialogFileChooserListener
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder.SelectStorageListener
import it.Ettore.egalfilemanager.filemanager.FileManager
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask
import it.Ettore.egalfilemanager.home.HomeNavigationManager
import it.Ettore.egalfilemanager.widget.MyWidgetManager
import java.io.File


class ActivityWidgetConfig : BaseActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUI()
        appWidgetId = getAppWidgetIdDelWidgetDaConfigurare()
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
        mostraDialogStorage()
    }


    private fun initUI() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        window.setBackgroundDrawable(ColorDrawable(0))
    }


    private fun mostraDialogStorage() {
        val builder = SelectStorageDialogBuilder(this)
        builder.setTitle(R.string.seleziona_destinazione)
        builder.hideIcon(true)
        builder.setStorageItems(HomeNavigationManager(this).listaItemsArchivioLocale())
        builder.setCancelable(false)
        builder.setSelectStorageListener(object : SelectStorageListener {
            override fun onSelectStorage(storagePath: File) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                val fileManager = FileManager(this@ActivityWidgetConfig)
                fileManager.ottieniStatoRootExplorer()
                val dialogType = DialogFileChooserBuilder.TYPE_SELECT_FILE_FOLDER
                val fileChooser = DialogFileChooserBuilder(this@ActivityWidgetConfig, dialogType)
                fileChooser.setTitle(R.string.seleziona_destinazione)
                fileChooser.setCancelable(false)
                fileChooser.setStartFolder(storagePath)
                fileChooser.setChooserListener(object : DialogFileChooserListener {
                    override fun onFileChooserSelected(selected: File) {
                        val myWidgetManager = MyWidgetManager(this@ActivityWidgetConfig)
                        myWidgetManager.updateWidget(appWidgetId, selected)
                        myWidgetManager.saveToPrefs(appWidgetId, selected)
                        val result = Intent()
                        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(RESULT_OK, result)
                        finish()
                    }

                    override fun onFileChooserCanceled() {
                        finishWithResultCanceled()
                    }
                })
                fileChooser.create().show()

                //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                ChiediTreeUriTask(this@ActivityWidgetConfig, storagePath, true).execute()
            }

            override fun onCancelStorageSelection() {
                finishWithResultCanceled()
            }
        })
        builder.showSelectDialogIfNecessary()
    }


    private fun getAppWidgetIdDelWidgetDaConfigurare(): Int {
        val launchIntent = intent
        val extras = launchIntent.extras
        if (extras != null) {
            return extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        return AppWidgetManager.INVALID_APPWIDGET_ID
    }


    private fun finishWithResultCanceled() {
        val cancelResultValue = Intent()
        cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, cancelResultValue)
        finish()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            PermissionsManager.REQ_PERMISSION_WRITE_EXTERNAL ->                 // If request is cancelled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //permessi non garantiti
                    permissionsManager.manageNotGuaranteedPermissions()
                } else {
                    //permessi garantiti
                    mostraDialogStorage()
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}