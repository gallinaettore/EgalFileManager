package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_AUDIO;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_DOCUMENTI;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_IMMAGINI;
import static it.Ettore.egalfilemanager.Costanti.CATEGORIA_VIDEO;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;
import java.util.Set;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.PreferitiManager;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.home.MostraNumElementiMultimedialiThread;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.view.ViewFavorite;
import it.Ettore.egalfilemanager.view.ViewStorage;
import it.Ettore.egalfilemanager.view.ViewUtils;


/**
 * Fragment con la visualizzazione della schermata principale
 */
public class FragmentMain extends GeneralFragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    @SuppressLint("StaticFieldLeak")
    private static FragmentMain fragmentInstance; //salvo l'istanza della classe per recuperarla dall'esterno
    private SwipeRefreshLayout swipeLayout;
    private LinearLayout layoutArchivioLocale, layoutPreferiti;
    private ActivityMain activityMain;
    private HomeNavigationManager homeNavigationManager;
    private List<HomeItem> homeItems;
    private PreferitiManager preferitiManager;
    private TextView numImmaginiTextView, numVideoTextView, numAudioTextView, numAltriFilesTextView;
    private ProgressBar progressBarNumAltriFiles;
    private MostraNumElementiMultimedialiThread mostraNumElementiMultimedialiThread; //thread per la visualizzazione del numero degli altri documenti, più lungo, da terminare quando l'activity non è visibile
    private LinearLayout layoutImmagini;


    public FragmentMain(){
        fragmentInstance = this;
    }



    /**
     * Creazione della view
     * @param inflater /
     * @param container /
     * @param savedInstanceState /
     * @return /
     */
    @SuppressLint("RtlHardcoded")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain = (ActivityMain)getActivity();
        activityMain.getOverflowMenu();

        homeNavigationManager = new HomeNavigationManager(getActivity());
        preferitiManager = new PreferitiManager(getPrefs());

        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_main, container, false);

        swipeLayout = v.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.colorAccent);

        //categorie
        final FloatingActionButton fabImmagini = v.findViewById(R.id.fabImmagini);
        fabImmagini.setOnClickListener(this);
        final FloatingActionButton fabVideo = v.findViewById(R.id.fabVideo);
        fabVideo.setOnClickListener(this);
        final FloatingActionButton fabAudio = v.findViewById(R.id.fabAudio);
        fabAudio.setOnClickListener(this);
        final FloatingActionButton fabAltro = v.findViewById(R.id.fabAltro);
        fabAltro.setOnClickListener(this);
        ViewUtils.correctFabMargin(fabImmagini, fabVideo, fabAudio, fabAltro,
                v.findViewById(R.id.fabRecenti), v.findViewById(R.id.fabLan), v.findViewById(R.id.fabFtp));
        layoutImmagini = v.findViewById(R.id.layout_immagini);
        layoutImmagini.setOnClickListener(this);
        v.findViewById(R.id.layout_video).setOnClickListener(this);
        v.findViewById(R.id.layout_audio).setOnClickListener(this);
        v.findViewById(R.id.layout_altri_files).setOnClickListener(this);
        numImmaginiTextView = v.findViewById(R.id.textview_num_immagini);
        numVideoTextView = v.findViewById(R.id.textview_num_video);
        numAudioTextView = v.findViewById(R.id.textview_num_audio);
        numAltriFilesTextView = v.findViewById(R.id.textview_num_altri);
        progressBarNumAltriFiles = v.findViewById(R.id.progressbar_num_altri);

        //archivio locale
        layoutArchivioLocale = v.findViewById(R.id.layout_archivio_locale);

        //preferiti
        v.findViewById(R.id.layout_aggiungi_preferito).setOnClickListener(this);
        layoutPreferiti = v.findViewById(R.id.layout_preferiti);

        //recenti
        v.findViewById(R.id.layout_recenti).setOnClickListener(this);

        //lan
        v.findViewById(R.id.layout_lan).setOnClickListener(this);

        //ftp
        v.findViewById(R.id.layout_ftp).setOnClickListener(this);

        if(LayoutDirectionHelper.isRightToLeft(getContext())){
            ((TextView)v.findViewById(R.id.textViewFilesRecenti)).setGravity(Gravity.RIGHT);
            ((TextView)v.findViewById(R.id.textViewFilesRecentiDescr)).setGravity(Gravity.RIGHT);
            ((TextView)v.findViewById(R.id.textViewLan)).setGravity(Gravity.RIGHT);
            ((TextView)v.findViewById(R.id.textViewLanDescr)).setGravity(Gravity.RIGHT);
            ((TextView)v.findViewById(R.id.textViewFtp)).setGravity(Gravity.RIGHT);
            ((TextView)v.findViewById(R.id.textViewFtpDescr)).setGravity(Gravity.RIGHT);
        }

        return v;
    }


    /**
     * Aggiorna i layout
     * @param savedInstanceState .
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        aggiornaLayoutPreferiti();
    }


    /**
     * Aggiorna i numeri di files contenuti nelle categorie
     */
    @Override
    public void onStart(){
        super.onStart();
        activityMain.setActionBarTitle(getString(R.string.app_name));
        //eseguo l'analisi del numero dei media
        if(activityMain.getPermissionsManager().hasPermissions()) {
            mostraNumeroElementiMultimediali();
        }
        aggiornaLayoutArchivioLocale();
    }


    @Override
    public void onStop(){
        super.onStop();
        if(mostraNumElementiMultimedialiThread != null){
            mostraNumElementiMultimedialiThread.setCanceled(true);
        }
    }


    /**
     * Eseguo l'analisi del numero dei media in thread separati e li visualizza
     */
    public void mostraNumeroElementiMultimediali(){
        new MostraNumElementiMultimedialiThread(getActivity(), MediaUtils.MEDIA_TYPE_IMAGE, numImmaginiTextView).start();
        new MostraNumElementiMultimedialiThread(getActivity(), MediaUtils.MEDIA_TYPE_VIDEO, numVideoTextView).start();
        new MostraNumElementiMultimedialiThread(getActivity(), MediaUtils.MEDIA_TYPE_AUDIO, numAudioTextView).start();
        mostraNumElementiMultimedialiThread = new MostraNumElementiMultimedialiThread(getActivity(), MediaUtils.MEDIA_TYPE_FILES, numAltriFilesTextView);
        mostraNumElementiMultimedialiThread.setProgressBar(progressBarNumAltriFiles);
        mostraNumElementiMultimedialiThread.start();
    }


    /**
     * Aggiorna il layout archivio locale
     */
    public void aggiornaLayoutArchivioLocale(){
        homeItems = homeNavigationManager.listaItemsArchivioLocale();
        layoutArchivioLocale.removeAllViews();
        for (final HomeItem item : homeItems) {
            final ViewStorage viewStorage = new ViewStorage(getContext());
            viewStorage.setHomeItem(item, activityMain);
            layoutArchivioLocale.addView(viewStorage);
        }
        //il focus viene richiesto dall'icona impostazioni del layout archivio locale, lo reimposto
        new Handler().postDelayed(() -> layoutImmagini.requestFocus(), 100);
    }


    /**
     * Aggiorna il layout preferiti
     */
    private void aggiornaLayoutPreferiti(){
        final Set<File> preferiti = preferitiManager.getPreferiti();
        layoutPreferiti.removeAllViews();
        for(final File file : preferiti){
            final ViewFavorite viewFavorite = new ViewFavorite(getContext());
            viewFavorite.setFile(file, activityMain, favorite -> {
                preferitiManager.cancellaPreferito(file);
                aggiornaLayoutPreferiti();
            });
            layoutPreferiti.addView(viewFavorite);
        }
    }


    /**
     * Aggiorna tutto
     */
    @Override
    public void onRefresh() {
        aggiornaLayoutArchivioLocale();
        aggiornaLayoutPreferiti();
        activityMain.aggiornaMenuArchivioLocale();
        swipeLayout.setRefreshing(false);
    }


    /**
     * Gestisce i click sulle views
     * @param view View clickata
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fabImmagini:
            case R.id.layout_immagini:
                activityMain.showFragment(FragmentAlbums.getInstance(CATEGORIA_IMMAGINI));
                break;
            case R.id.fabVideo:
            case R.id.layout_video:
                activityMain.showFragment(FragmentAlbums.getInstance(CATEGORIA_VIDEO));
                break;
            case R.id.fabAudio:
            case R.id.layout_audio:
                activityMain.showFragment(FragmentAlbums.getInstance(CATEGORIA_AUDIO));
                break;
            case R.id.fabAltro:
            case R.id.layout_altri_files:
                activityMain.showFragment(FragmentAlbums.getInstance(CATEGORIA_DOCUMENTI));
                break;
            case R.id.layout_aggiungi_preferito:
                //dialog per la scelta dello storage
                final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(getContext());
                builder.setTitle(R.string.aggiungi_a_preferiti);
                builder.hideIcon(true);
                builder.setStorageItems(homeItems);
                builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
                    @Override
                    public void onSelectStorage(File storagePath) {
                        final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(getContext(), DialogFileChooserBuilder.TYPE_SELECT_FILE_FOLDER);
                        fileChooser.setTitle(R.string.aggiungi_a_preferiti);
                        fileChooser.setStartFolder(storagePath);
                        fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                            @Override
                            public void onFileChooserSelected(File selected) {
                                preferitiManager.aggiungiPreferito(selected);
                                aggiornaLayoutPreferiti();
                            }

                            @Override
                            public void onFileChooserCanceled() {}
                        });
                        fileChooser.create().show();
                    }

                    @Override
                    public void onCancelStorageSelection() {

                    }
                });
                builder.showSelectDialogIfNecessary();
                break;
            case R.id.layout_recenti:
                activityMain.showFragment(new FragmentFilesRecenti());
                break;
            case R.id.layout_lan:
                activityMain.showFragment(new FragmentServerLan());
                break;
            case R.id.layout_ftp:
                activityMain.showFragment(new FragmentServerFtp());
                break;
        }
    }


    /**
     * Instanza corrente del fragment. Utilizzato per accedere al fragment dall'esterno come del caso dello StorageStatusReceiver
     * @return Fragment se è stato istanziato in precedenza. Null se ancora non istanziato.
     */
    public static FragmentMain getExistingInstance(){
        return fragmentInstance;
    }
}
