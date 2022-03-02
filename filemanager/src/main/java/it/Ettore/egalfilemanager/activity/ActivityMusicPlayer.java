package it.Ettore.egalfilemanager.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.ViewUtils;
import it.Ettore.egalfilemanager.PermissionsManager;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.DialogInfoBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.SerializableFileList;
import it.Ettore.egalfilemanager.fileutils.UriUtils;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.mediastore.MediaInfo;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.musicplayer.MediaBrowserHelper;
import it.Ettore.egalfilemanager.musicplayer.MediaNotificationManager;
import it.Ettore.egalfilemanager.musicplayer.MusicService;
import it.Ettore.egalfilemanager.musicplayer.PlaylistManager;
import it.Ettore.egalfilemanager.recycler.LineItemDecoration;
import it.Ettore.egalfilemanager.recycler.PlaylistAdapter;
import it.Ettore.egalfilemanager.recycler.PlaylistTouchCallback;
import it.Ettore.egalfilemanager.view.PlayerTimeView;

import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE;



/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Activity per la riproduzione di brani musicali
 */
public class ActivityMusicPlayer extends BaseActivity implements View.OnClickListener, PlaylistAdapter.RecyclerViewListener {
    private boolean isPlaying;
    private MediaBrowserHelper mMediaBrowserHelper;

    private PlayerTimeView playerTimeView;
    private TextView artistaTextView, titoloTextView;
    private ImageView artImageView;
    private FloatingActionButton fabPlay;
    private PlaylistAdapter playlistAdapter;
    private List<File> filesFromIntent;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initUI();

        mMediaBrowserHelper = new MediaBrowserConnection(this);
        mMediaBrowserHelper.registerCallback(new MediaBrowserListener());

        boolean startedFromNotification = getIntent().getBooleanExtra(MediaNotificationManager.KEY_BUNDLE_STARTED_FROM_NOTIFICATION, false);
        if(startedFromNotification){
            return;
        }

        //non leggo nuovamente i dati dopo una rotazione dello schermo
        if(savedInstanceState != null){
            return;
        }

        this.filesFromIntent = getFilesFromIntent(getIntent());

        if(!getPermissionsManager().hasPermissions()) {
            getPermissionsManager().requestPermissions();
        }
    }


    /**
     * Inizializza gli elementi dell'interfaccia grafica
     */
    private void initUI(){
        fabPlay = findViewById(R.id.fab_play);
        fabPlay.setOnClickListener(this);
        findViewById(R.id.fab_previous).setOnClickListener(this);
        findViewById(R.id.fab_next).setOnClickListener(this);
        findViewById(R.id.fab_aggiungi).setOnClickListener(this);
        playerTimeView = findViewById(R.id.player_time_view);
        artImageView = findViewById(R.id.art_image_view);
        artistaTextView = findViewById(R.id.artista_text_view);
        titoloTextView = findViewById(R.id.titolo_text_view);
        ViewUtils.aggiungiMarqueeAlleTextView(artistaTextView, titoloTextView);

        recyclerView = findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new LineItemDecoration());
        playlistAdapter = new PlaylistAdapter(this, this);
        recyclerView.setAdapter(playlistAdapter);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new PlaylistTouchCallback(playlistAdapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    /**
     * Ottiene la lista di files da riprodurre passati tramite intent
     * @param intent Intent con dati
     * @return Lista di files da riprodurre (può essere anche vuota ma non null)
     */
    private List<File> getFilesFromIntent(@NonNull Intent intent){
        List<File> filesFromIntent = new ArrayList<>();

        final Serializable elementiPassati = intent.getSerializableExtra(KEY_BUNDLE_ELEMENTI_DA_RIPRODURRE);
        if(elementiPassati != null){
            //all'activity viene passata una lista di filesFromIntent da riprodurre
            final SerializableFileList filesDaIntent = (SerializableFileList)elementiPassati;
            for(File file : filesDaIntent){
                if(FileUtils.getFileExtension(file).equalsIgnoreCase("m3u")){
                    filesFromIntent.addAll(PlaylistManager.parseM3uPlaylist(file));
                } else {
                    filesFromIntent.add(file);
                }
            }
        } else {
            //all'activity viene passata un file da riprodurre
            final String type = getIntent().getType();
            if (type != null && type.startsWith("audio/")) {
                final Uri fileUri = getIntent().getData();
                final File file = UriUtils.uriToFile(this, fileUri);
                if (file != null) {
                    if(FileUtils.getFileExtension(file).equalsIgnoreCase("m3u")){
                        filesFromIntent.addAll(PlaylistManager.parseM3uPlaylist(file));
                    } else {
                        filesFromIntent.add(file);
                    }
                }
            }
        }
        return filesFromIntent;
    }


    /**
     * Gestione dei tocchi
     * @param view View toccata
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fab_previous:
                mMediaBrowserHelper.getTransportControls().skipToPrevious();
                break;
            case R.id.fab_play:
                if (isPlaying) {
                    mMediaBrowserHelper.getTransportControls().pause();
                } else {
                    mMediaBrowserHelper.getTransportControls().play();
                }
                break;
            case R.id.fab_next:
                mMediaBrowserHelper.getTransportControls().skipToNext();
                break;
            case R.id.fab_aggiungi:
                startActivity(new Intent(this, ActivityAggiungiFileAPlaylist.class));
                break;
        }
    }


    /**
     * Mostra l'errore nalla textview
     */
    private void mostraErrore(){
        artistaTextView.setText(R.string.impossibile_riprodurre);
        titoloTextView.setText(null);
    }


    @Override
    public void onStart() {
        super.onStart();
        mMediaBrowserHelper.onStart();
        new Handler().postDelayed(()-> fabPlay.requestFocus(), 200);
    }


    @Override
    public void onStop() {
        super.onStop();
        playerTimeView.disconnectController();
        mMediaBrowserHelper.onStop();
    }


    @Override
    protected void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationChannel notificationChannel = notificationManager.getNotificationChannel(MediaNotificationManager.MUSICPLAYER_CHANNEL);
            if (notificationChannel != null && notificationChannel.getImportance() == 0) {
                //notifica disattivata, termino l'esecuzione al termine dell'activity
                final Intent serviceIntent = new Intent(this, MusicService.class);
                stopService(serviceIntent);
            }
        }
        super.onDestroy();
    }

    /**
     * Chiamato al posto di onCreate se l'activity è già presente (ad esempio quando si aggiungono files alla playlist)
     * @param intent Intent passato
     */
    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        this.filesFromIntent = getFilesFromIntent(intent);
    }


    /**
     * Chiamato quando si clicca su un item della recycler view
     * @param file File contenuto nell'item
     */
    @Override
    public void onRecyclerViewItemClick(File file) {
        try {
            mMediaBrowserHelper.getTransportControls().playFromMediaId(file.getAbsolutePath(), null);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }


    /**
     * Chiamato quando si sposta un item della recycler view
     * @param fromPosition Posisione originale
     * @param toPosition Nuova posizione
     */
    @Override
    public void onRecyclerViewItemMoved(int fromPosition, int toPosition) {
        final Bundle bundle = new Bundle();
        bundle.putInt(MusicService.KEY_BUNDLE_MOVE_ITEM_FROM_POSITION, fromPosition);
        bundle.putInt(MusicService.KEY_BUNDLE_MOVE_ITEM_TO_POSITION, toPosition);
        mMediaBrowserHelper.getMediaController().getTransportControls().sendCustomAction(MusicService.CALLBACK_ACTION_MOVE_ITEM, bundle);
    }


    /**
     * Chiamato quando si cancella un item dalla recycler view
     * @param position Posizione dell'item cancellato
     */
    @Override
    public void onRecyclerViewItemDeleted(int position) {
        final MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
        mediaDescriptionBuilder.setMediaId(String.valueOf(position));
        mMediaBrowserHelper.getMediaController().removeQueueItem(mediaDescriptionBuilder.build());
    }


    /**
     * Classe che gestisce la connessione al media browser
     * Customize the connection to our {@link MediaBrowserServiceCompat} and implement our app specific desires.
     */
    private class MediaBrowserConnection extends MediaBrowserHelper {
        private MediaBrowserConnection(Context context) {
            super(context, MusicService.class);
        }

        @Override
        protected void onConnected(@NonNull MediaControllerCompat mediaController) {
            playerTimeView.setMediaController(mediaController);

            //aggiunge i filesFromIntent alla coda
            if(filesFromIntent != null && !filesFromIntent.isEmpty()) {
                for(File file : filesFromIntent){
                    mediaController.addQueueItem(createMediaDescription(file));
                }

                //notifico di aver finito di aggiungere tutti i files
                final Bundle bundleItemsAddedFinished = new Bundle();
                bundleItemsAddedFinished.putInt(MusicService.KEY_BUNDLE_NUM_ITEMS_ADDED, filesFromIntent.size());
                mediaController.getTransportControls().sendCustomAction(MusicService.CALLBACK_ACTION_ADD_ITEMS_FINISHED, bundleItemsAddedFinished);

                mediaController.getTransportControls().prepare();
                mediaController.getTransportControls().play();
            }

            //aggiorno la playlist della recyclerview
            playlistAdapter.setPlaylist(mediaController.getQueue(), filesFromIntent);
            filesFromIntent = null;

            //se il player è già in riproduzione (quindi l'activity è stata avviata dalla notifica)
            isPlaying = mediaController.getPlaybackState() != null && mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;
            if(isPlaying){
                //aggiorno tempo e seek bar
                playerTimeView.updateView();
            }

            //a connessione avvenuta ricreo il menu per aggiornare la checkbox shuffle
            invalidateOptionsMenu();
        }
    }


    /**
     * Crea un MediaDescription a partire da un file
     * @param file File
     * @return MediaDescription
     */
    private MediaDescriptionCompat createMediaDescription(File file){
        final MediaDescriptionCompat.Builder mediaDescriptionBuilder = new MediaDescriptionCompat.Builder();
        mediaDescriptionBuilder.setDescription(file.getAbsolutePath());
        mediaDescriptionBuilder.setMediaId(file.getAbsolutePath());
        return mediaDescriptionBuilder.build();
    }



    /**
     * Callback per interagine con le azioni che avvengono nel media browser
     */
    private class MediaBrowserListener extends MediaControllerCompat.Callback {

        /**
         * Chimato quando cambia lo stato della riproduzione
         * @param playbackState Stato della riproduzione
         */
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
            //aggiorno i pulsanti play/pausa
            isPlaying = playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING;
            if(isPlaying){
                fabPlay.setImageResource(R.drawable.ic_pause_white_24dp);
            } else {
                fabPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            }

            //se in stato di errore lo mostro nella textview (se la playlist contiene più files viene riprodotto quello successivo)
            if(playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_ERROR){
                mostraErrore();
            }
        }


        /**
         * Chiamato quando i metadati cambiano (viene riprodotto un nuovo file)
         * @param mediaMetadata Metadati
         */
        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadata) {
            if (mediaMetadata == null) {
                return;
            }
            //aggiorno la UI con le informazioni del nuovo file in riproduzione
            titoloTextView.setText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            artistaTextView.setText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            if(mediaMetadata.containsKey(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON)){
                artImageView.setImageBitmap(mediaMetadata.getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON));
            } else {
                artImageView.setImageResource(R.drawable.coverart_audio);
            }
            int selectedIndex = (int)mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
            playlistAdapter.setSelectedIndex(selectedIndex);
            if(selectedIndex >= 0) {
                recyclerView.scrollToPosition(selectedIndex);
            }
        }


        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }


        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
        }
    }





    /* MENU */


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_music_player, menu);

        //quando creo l'activity il media controller è ancora null, ricreo il menu dopo la connessione al media browser
        if(mMediaBrowserHelper.getMediaController() != null) {
            final MenuItem shuffleMenuItem = menu.findItem(R.id.shuffle);
            final boolean shuffleEnabled = mMediaBrowserHelper.getMediaController().getShuffleMode() != PlaybackStateCompat.SHUFFLE_MODE_NONE;
            shuffleMenuItem.setChecked(shuffleEnabled);
        }

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.rimuovi_tutto:
                rimuoviBrani();
                return true;
            case R.id.media_info:
                final MediaMetadataCompat metadata = mMediaBrowserHelper.getMediaController().getMetadata();
                if (metadata != null) {
                    final String filePath = metadata.getDescription().getMediaId();
                    final Map<String, String> mapMediaInfo = MediaInfo.getMetadata(this, new File(filePath));
                    final DialogInfoBuilder dialogInfoBuilder = new DialogInfoBuilder(this, R.string.media_info, mapMediaInfo);
                    dialogInfoBuilder.create().show();
                }
                return true;
            case R.id.shuffle:
                item.setChecked(!item.isChecked());
                if(item.isChecked()){
                    mMediaBrowserHelper.getMediaController().getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                } else {
                    mMediaBrowserHelper.getMediaController().getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                }
                return true;
            case R.id.repeat_mode:
                final String[] repeatModeNames = {getString(R.string.repeat_none), getString(R.string.repeat_all), getString(R.string.repeat_track)};
                final List<Integer> repeatModeValues = Arrays.asList(PlaybackStateCompat.REPEAT_MODE_NONE, PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_ONE);
                int selectedRepeatMode = mMediaBrowserHelper.getMediaController().getRepeatMode();
                int indexRepeatMode = repeatModeValues.indexOf(selectedRepeatMode);
                final CustomDialogBuilder builder = new CustomDialogBuilder(this);
                builder.setTitle(R.string.repeat_mode);
                builder.hideIcon(true);
                builder.setSingleChoiceItems(repeatModeNames, indexRepeatMode, (dialogInterface, i) -> {
                    int newRepeatMode = repeatModeValues.get(i);
                    mMediaBrowserHelper.getMediaController().getTransportControls().setRepeatMode(newRepeatMode);
                    dialogInterface.dismiss();
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
                return true;
            case R.id.salva_playlist:
                mostraDialogStorageESalva();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Rimuove tutti i brani dal service e dalla playlist
     */
    private void rimuoviBrani(){
        mMediaBrowserHelper.getMediaController().getTransportControls().sendCustomAction(MusicService.CALLBACK_ACTION_REMOVE_ALL, null);
        playlistAdapter.removeAll();
    }


    /**
     * Mostra la dialog per la scelta dello storage
     */
    private void mostraDialogStorageESalva(){
        final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(this);
        builder.setTitle(R.string.seleziona_destinazione);
        builder.hideIcon(true);
        builder.setStorageItems(new HomeNavigationManager(this).listaItemsArchivioLocale());
        builder.setCancelable(false);
        builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
            @Override
            public void onSelectStorage(File storagePath) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(ActivityMusicPlayer.this, DialogFileChooserBuilder.TYPE_SAVE_FILE);
                fileChooser.setTitle(R.string.seleziona_destinazione);
                fileChooser.setCancelable(false);
                fileChooser.setStartFolder(storagePath);
                fileChooser.setFileName("Playlist.m3u");
                fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                    @Override
                    public void onFileChooserSelected(final File selected) {
                        if(!selected.exists()){
                            salvaPlaylist(selected);
                        } else {
                            final CustomDialogBuilder builder = new CustomDialogBuilder(ActivityMusicPlayer.this);
                            builder.setType(CustomDialogBuilder.TYPE_WARNING);
                            builder.setMessage(getString(R.string.sovrascrivi_file, selected.getName()));
                            builder.setPositiveButton(R.string.sovrascrivi, (dialogInterface, i) -> salvaPlaylist(selected));
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.create().show();
                        }
                    }

                    @Override
                    public void onFileChooserCanceled() {}
                });
                fileChooser.create().show();

                //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                new ChiediTreeUriTask(ActivityMusicPlayer.this, storagePath, true).execute();
            }

            @Override
            public void onCancelStorageSelection(){}
        });
        builder.showSelectDialogIfNecessary();
    }


    /**
     * Salva la playlist sul file m3u
     * @param file File m3u
     */
    private void salvaPlaylist(File file){
        boolean success = PlaylistManager.salvaPlaylistM3u(ActivityMusicPlayer.this, playlistAdapter.getPlaylist(), file);
        if(success) {
            ColoredToast.makeText(ActivityMusicPlayer.this, R.string.file_salvato, Toast.LENGTH_LONG).show();
                final MediaUtils mediaUtils = new MediaUtils(this);
                mediaUtils.addFileToMediaLibrary(file, null);
        } else {
            ColoredToast.makeText(ActivityMusicPlayer.this, R.string.errore_salvataggio, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionsManager.REQ_PERMISSION_WRITE_EXTERNAL:
                // If request is cancelled, the result arrays are empty.
                if(grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    //permessi non garantiti
                    getPermissionsManager().manageNotGuaranteedPermissions();
                } else {
                    //permessi garantiti, tolgo tutto perchè non è possibile riprodurli visto che sono stati aggiunti quando ancora non c'erano i permessi
                    if(mMediaBrowserHelper.getMediaController() != null) {
                        mMediaBrowserHelper.getMediaController().getTransportControls().sendCustomAction(MusicService.CALLBACK_ACTION_REMOVE_ALL, null);
                    }
                    playlistAdapter.removeAll();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
