package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fragment.FragmentAggiungiAPlaylistAlbum;

import static it.Ettore.egalfilemanager.Costanti.MAIN_BACKSTACK;


/**
 * Activity per l'esplorazione dei files audio da aggiungere alla playlist del music player
 */
public class ActivityAggiungiFileAPlaylist extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aggiungi_a_playlist);
        setActionBarTitle(R.string.aggiungi_a_playlist);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Setup initial fragment
        if (savedInstanceState == null) {
            final FragmentAggiungiAPlaylistAlbum fragment = new FragmentAggiungiAPlaylistAlbum();
            getSupportFragmentManager().beginTransaction().add(R.id.frame_layout, fragment).commit();
        }
    }


    /**
     * Mostra il fragment
     * @param fragment Fragment da mostrare
     */
    public void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).addToBackStack(MAIN_BACKSTACK).commitAllowingStateLoss(); //effettua il commit dopo che lo stato dell'activity è salvato
    }
}
