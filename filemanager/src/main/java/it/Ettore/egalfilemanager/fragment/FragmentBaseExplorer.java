package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.BaseActivity;
import it.Ettore.egalfilemanager.recycler.MultiSelectable;


/**
 * Fragment per l'esplorazione files di base con la selezione multipla
 */
public abstract class FragmentBaseExplorer extends GeneralFragment {
    private MultiSelectable multiselectableAdapter;
    private BaseActivity activityMain;
    private String titoloFragment;


    /**
     * Imposta l'adapter che gestisce la selezione multipla
     * Ricordarsi di usarlo ogni volta che l'adapter viene creato d'accapo
     * @param multiselectableAdapter Adapter
     */
    protected void setMultiselectableAdapter(MultiSelectable multiselectableAdapter){
        this.multiselectableAdapter = multiselectableAdapter;
    }


    /**
     * Imposta l'ActivityMain e prepara il fragment per poter mostrare il menu. Da chiamare nell'onCreateView del Fragment
     * @param activityMain ActivityMain
     */
    protected void setActivityMain(@NonNull BaseActivity activityMain){
        this.activityMain = activityMain;
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();
    }


    /**
     * Imposta il titolo da mostrare nel fragment. Da chiamare nell'onCreateView del Fragment
     * @param title Titolo da mostrare
     */
    protected void setTitle(@StringRes int title){
        setTitle(getString(title));
    }


    /**
    * Imposta il titolo da mostrare nel fragment. Da chiamare nell'onCreateView del Fragment
    * @param title Titolo da mostrare
    */
    protected void setTitle(String title){
        this.titoloFragment = title;
        activityMain.setActionBarTitle(titoloFragment);
    }


    /**
     * Configura il tasto "indietro" in modo da disattivare la selezione multipla se attiva. Da chiamare nell'onCreateView del Fragment
     * @param fragmentView View del fragment.
     */
    protected void configuraBackButton(@NonNull View fragmentView){
        fragmentView.setFocusableInTouchMode(true);
        fragmentView.requestFocus();
        fragmentView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey( View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK && multiselectableAdapter != null && multiselectableAdapter.modalitaSelezioneMultipla()) {
                    disattivaSelezioneMultipla();
                    return true;
                }
                return false;
            }
        } );
    }


    /**
     * Disattiva la modalità selezione multipla
     */
    public void disattivaSelezioneMultipla(){
        multiselectableAdapter.disattivaSelezioneMultipla();
        mostraNumeroElementiSelezionati(false);
    }


    /**
     * Mostra sull'action bar il titolo del fragment o il numero di elementi selezionati
     * @param mostraNumElementi True mostra gli elementi. False mostra il titolo.
     */
    public void mostraNumeroElementiSelezionati(boolean mostraNumElementi){
        String nuovoTitolo;
        if(mostraNumElementi) {
            int allItems = 0;
            if(multiselectableAdapter instanceof RecyclerView.Adapter){
                allItems = ((RecyclerView.Adapter)multiselectableAdapter).getItemCount();
            } else if (multiselectableAdapter instanceof ArrayAdapter){
                allItems = ((ArrayAdapter)multiselectableAdapter).getCount();
            }
            nuovoTitolo = String.format(Locale.ENGLISH, "%s/%s", multiselectableAdapter.numElementiSelezionati(), allItems);
        } else {
            nuovoTitolo = titoloFragment;
        }
        activityMain.setActionBarTitle(nuovoTitolo);
        activityMain.invalidateOptionsMenu();
    }


    /**
     * Aggiungi al menu gli item base necessari alla selezione multipla
     * @param menu .
     * @param inflater .
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        //selezione multipla default
        if(multiselectableAdapter != null && multiselectableAdapter.modalitaSelezioneMultipla()){
            inflater.inflate(R.menu.menu_base_selezione_multipla, menu);
        }
    }


    /**
     * Funzionamento dei menu item base
     * @param item .
     * @return .
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.seleziona_tutto:
                multiselectableAdapter.selezionaTutto(true);
                mostraNumeroElementiSelezionati(true);
                return true;
            case R.id.deseleziona_tutto:
                multiselectableAdapter.selezionaTutto(false);
                mostraNumeroElementiSelezionati(true);
                return true;
            case R.id.disattiva_selezione_multipla:
                disattivaSelezioneMultipla();
                return true;
            case R.id.filtro:
                return true;
            default:
                return getActivity().onOptionsItemSelected(item);
        }
    }
}
