package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import static it.Ettore.egalfilemanager.Costanti.ACTION_FRAGMENT_FILE_EXPL;
import static it.Ettore.egalfilemanager.Costanti.ACTION_RISULTATI_FILES_DUPLICATI;
import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_DIRECTORY_TO_SHOW;
import static it.Ettore.egalfilemanager.Costanti.MAIN_BACKSTACK;
import static it.Ettore.egalfilemanager.Costanti.VALUE_BUNDLE_AZIONE_FRAGMENT_FILE_EXPL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.PermissionsManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.StorageStatusReceiver;
import it.Ettore.egalfilemanager.fileutils.Clipboard;
import it.Ettore.egalfilemanager.fileutils.FileOpener;
import it.Ettore.egalfilemanager.fragment.FragmentFilesExplorer;
import it.Ettore.egalfilemanager.fragment.FragmentImpostazioni;
import it.Ettore.egalfilemanager.fragment.FragmentMain;
import it.Ettore.egalfilemanager.fragment.FragmentMostraFilesDuplicati;
import it.Ettore.egalfilemanager.fragment.FragmentServerFtp;
import it.Ettore.egalfilemanager.fragment.FragmentServerLan;
import it.Ettore.egalfilemanager.fragment.FragmentTools;
import it.Ettore.egalfilemanager.ftp.FtpSession;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;


/**
 * Activity principale dell'applicazione
 */
public class ActivityMain extends BaseActivity {
    private static ActivityMain activityMainInstance; //salvo l'istanza della classe per recuperarla dall'esterno
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private Clipboard clipboard;
    private StorageStatusReceiver storageStatusReceiver;
    private TextView nomeAppTextView;
    private HomeNavigationManager homeNavigationManager;
    private static final int MENU_GROUP_ARCHIVIO_LOCALE = 1;
    private SparseArray<HomeItem> itemsArchivioLocale;
    private FtpSession ftpSession;



    @SuppressLint("RtlHardcoded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainInstance = this;
        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        navigationView = findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(item -> {
            showFragmentFromNavigation(item);
            drawerLayout.closeDrawer(navigationView);
            return false;
        });

        this.homeNavigationManager = new HomeNavigationManager(this);
        this.itemsArchivioLocale = new SparseArray<>();
        aggiornaMenuArchivioLocale();

        //Mostro nome app e versione nell'header della navigation view
        final View header = navigationView.getHeaderView(0);
        nomeAppTextView = header.findViewById(R.id.textview_nome_app);
        final TextView versioneAppTextView = header.findViewById(R.id.textview_versione_app);
        try {
            final String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versioneAppTextView.setText(String.format("v%s", versionName));
        } catch (PackageManager.NameNotFoundException ignored) { }
        if (LayoutDirectionHelper.isRightToLeft(this)){
            nomeAppTextView.setGravity(Gravity.RIGHT);
            versioneAppTextView.setGravity(Gravity.RIGHT);
        }

        // Setup initial fragment
        if (savedInstanceState == null) {
            final FragmentMain fragmentMain = new FragmentMain();
            getSupportFragmentManager().beginTransaction().add(R.id.anchor_point, fragmentMain).commit();
        }

        //Gestione dei permessi, li mostro dopo un secondo ed evito i problemi di chiusura della dialog di alcuni telefoni
        if (!getPermissionsManager().hasPermissions()) {
            new Handler().postDelayed(() -> getPermissionsManager().requestPermissions(), 1000);
        }

        this.clipboard = new Clipboard();
        this.storageStatusReceiver = new StorageStatusReceiver();

        gestisciAzioniDaIntent(getIntent());
    }




    @Override
    public void onStart(){
        super.onStart();
        //registro il receiver
        registerReceiver(storageStatusReceiver, StorageStatusReceiver.getIntentFilter());
        //mostro il nome dell'app
        nomeAppTextView.setText(getString(R.string.app_name));
    }


    @Override
    public void onStop(){
        super.onStop();
        //deregistro il receiver
        unregisterReceiver(storageStatusReceiver);
    }



    /**
     * Mostra il fragment selezionato nella navigation bar
     * @param menuItem MenuItem della Navigation Bar
     */
    private void showFragmentFromNavigation(final MenuItem menuItem) {
        final int itemId = menuItem.getItemId();
        if (itemsArchivioLocale.get(itemId) != null) {
            //se l'itemid è contenuto nella map è item dell'archivio locale
            final HomeItem homeItem = itemsArchivioLocale.get(itemId);
            final Fragment fragment = FragmentFilesExplorer.getInstance(homeItem.titolo, homeItem.startDirectory, homeItem.titolo, homeItem.startDirectory);
            showFragment(fragment);
        } else {
            switch (itemId) {
                case R.id.lan:
                    showFragment(new FragmentServerLan());
                    break;
                case R.id.ftp:
                    showFragment(new FragmentServerFtp());
                    break;
                case android.R.id.home:
                    drawerLayout.openDrawer(navigationView);
                    break;
                case R.id.main_page:
                    showFragment(new FragmentMain());
                    break;
                case R.id.tools:
                    showFragment(new FragmentTools());
                    break;
                case R.id.impostazioni:
                    showFragment(new FragmentImpostazioni());
                    break;
                case R.id.about:
                    startActivity(new Intent(this, ActivityAbout.class));
                    break;
                default:
                    throw new IllegalArgumentException("Nessun fragment associato a questo navigation item");
            }
        }
    }


    /**
     * Mostra il fragment
     * @param fragment Fragment da mostrare
     */
    public void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.anchor_point, fragment).addToBackStack(MAIN_BACKSTACK).commitAllowingStateLoss(); //effettua il commit dopo che lo stato dell'activity è salvato
    }


    /**
     * Rimuove il fragment dal backstack. Da chiamare prima di mostrare il fragment successivo.
     * @param fragment Fragment da rimuovere dal backstack
     */
    public void removeFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        finishCurrentFragment();
    }


    /**
     * Chiude il fragment corrente
     */
    public void finishCurrentFragment(){
        getSupportFragmentManager().popBackStack();
    }


    /**
     * Restituisce la clipboard
     * @return Clipboard da utilizzare per operazioni di copia/incolla
     */
    public Clipboard getClipboard(){
        return this.clipboard;
    }


    /**
     * Aggiorna il menù archivio locale nella navigation bar
     */
    public void aggiornaMenuArchivioLocale(){
        final List<HomeItem> homeItems = homeNavigationManager.listaItemsArchivioLocale();
        final Menu menu = navigationView.getMenu();
        //elimino i menu item creati in precedenza
        for(int i = 0; i < itemsArchivioLocale.size(); i++) {
            int key = itemsArchivioLocale.keyAt(i);
            final HomeItem homeItem = itemsArchivioLocale.get(key);
            menu.removeItem(homeItem.menuItemId);
        }
        itemsArchivioLocale.clear();
        //aggiungo i nuovi menu
        for(HomeItem homeItem : homeItems){
            final MenuItem menuItem = menu.add(MENU_GROUP_ARCHIVIO_LOCALE, homeItem.menuItemId, Menu.NONE, homeItem.titolo);
            menuItem.setIcon(homeItem.resIdIconaNav);
            itemsArchivioLocale.put(homeItem.menuItemId, homeItem);
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void invalidateOptionsMenu(){
        //ritardo il cambiamento del menu, per evitare che al click dell'item si intravede il nuovo menu per qualche istante
        new Handler().postDelayed(() -> ActivityMain.super.invalidateOptionsMenu(), 100);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionsManager.REQ_PERMISSION_WRITE_EXTERNAL:
                // If request is cancelled, the result arrays are empty.
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    //permessi storage non garantiti
                    getPermissionsManager().manageNotGuaranteedPermissions();
                } else {
                    //quando vengono dati i permessi storage, aggiorno il numero di elementi delle categorie (se visualizzato il fragment main)
                    final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.anchor_point);
                    if(currentFragment instanceof FragmentMain){
                        ((FragmentMain)currentFragment).mostraNumeroElementiMultimediali();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * E' possibile creare una sola ActivityMain, se l'activity è già esistente viene chiamato onNewIntent() invece di onCreate()
     * @param intent Intent
     */
    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        gestisciAzioniDaIntent(intent);
    }


    @Override
    public void onDestroy(){
        //chiudo il server ft se aperto
        if(ftpSession != null){
            ftpSession.disconnect();
        }
        //svuoto la cache
        final File[] listaFiles = getCacheDir().listFiles();
        for(File file : listaFiles){
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        super.onDestroy();
    }


    /**
     * Gestisce le azioni che deve compiere l'activity main quando chiamata da altre activity
     * @param intent Intent
     */
    private void gestisciAzioniDaIntent(@NotNull Intent intent){
        final String action = intent.getAction();
        if(action == null) return;
        if(action.equals(VALUE_BUNDLE_AZIONE_FRAGMENT_FILE_EXPL)){
            //azione explora file
            try {
                final File file = new File(intent.getStringExtra(KEY_BUNDLE_DIRECTORY_TO_SHOW));
                if (file.isDirectory()) {
                    //mostra il fragment file explorer
                    showFragment(FragmentFilesExplorer.getInstance(file));
                } else {
                    //apro il file
                    new FileOpener(this).openFile(file);
                }
                if (!file.exists()) { //potrebbe esistere ma solo con i permessi di root
                    ColoredToast.makeText(this, R.string.elemento_non_trovato, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if(action.equals(ACTION_RISULTATI_FILES_DUPLICATI)){
            //azione mostra files duplicati
            final Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.anchor_point);
            if(currentFragment != null && currentFragment.getClass().equals(FragmentMostraFilesDuplicati.class)){
                removeFragment(currentFragment); //se un fragment uguale è già mostrato lo rimuovo per evitare di mostrare 2 fragment
            }
            showFragment(new FragmentMostraFilesDuplicati());
        }/* else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
            //azione inserimento usb
            //al momento non eseguo alcuna azione, mi limito solo ad aprire l'activity main
            final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        }*/
    }



    /**
     * Usato per aprire files o directories da altre activities
     * @param activity Activity chiamante
     * @param file File o cartella da aprire
     */
    public static void openFileExplorer(@NonNull final Activity activity, @NonNull File file){
        final Intent intent = new Intent(activity, ActivityMain.class);
        intent.setAction(ACTION_FRAGMENT_FILE_EXPL);
        intent.putExtra(KEY_BUNDLE_DIRECTORY_TO_SHOW, file.getAbsolutePath());
        activity.startActivity(intent);
    }



    /**
     * Instanza corrente dell'activity. Utilizzato per accedere all'activity dall'esterno come del caso dello StorageStatusReceiver
     * @return ActivityMain se è stata istanziata in precedenza. Null se ancora non istanziata.
     */
    public static ActivityMain getExistingInstance(){
        return activityMainInstance;
    }


    /**
     * Imposta i dati della sessione FTP corrente
     * @param ftpSession Sessione FTP
     */
    public void setFtpSession(FtpSession ftpSession){
        this.ftpSession = ftpSession;
    }


    /**
     * Restituisce i dati della sessione FTP corrente
     * @return Sessione FTP
     */
    public FtpSession getFtpSession(){
        return this.ftpSession;
    }


}
