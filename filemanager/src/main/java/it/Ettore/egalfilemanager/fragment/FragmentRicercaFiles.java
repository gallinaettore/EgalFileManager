package it.Ettore.egalfilemanager.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.Ettore.androidutilsx.ext.ViewsExtKt;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.tools.ricercafiles.ParametriRicerca;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Fragment per l'immissione dei dati riguardanti il file da cercare
 */
public class FragmentRicercaFiles extends GeneralFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String KEY_BUNDLE_STORAGES_CHECKED = "storages_checked";
    private ActivityMain activityMain;
    private Map<CheckBox, HomeItem> mapStorages;
    private EditText cercaEditText, editTextDimensioniMinori, editTextDimensioniMaggiori;
    private CheckBox checkboxCercaImmagini, checkboxCercaVideo, checkboxCercaAudio, checkboxCercaAltri, checkboxIgnoreCase;
    private RadioButton radioDimensioniTutte, radioDimensioniMinori, radioDimensioniMaggiori;
    private Spinner spinnerDimensioniMinori, spinnerDimensioniMaggiori;
    private boolean[] storagesChecked;
    private LinearLayout layoutStorages;


    /**
     * Costruttore di base (necessario)
     */
    public FragmentRicercaFiles(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creo la view del fragment
        final View v = inflater.inflate(R.layout.fragment_ricerca_files, container, false);
        activityMain = (ActivityMain)getActivity();
        activityMain.setActionBarTitle(R.string.tool_cerca_file);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        //storages (escludo il percorso root perchè la ricerca potrebbe durare anche più di 10 minuti)
        final List<HomeItem> storages = new HomeNavigationManager(getActivity()).listaItemsSdCards();
        mapStorages = new HashMap<>(storages.size());
        layoutStorages = v.findViewById(R.id.layout_storages);

        if(savedInstanceState != null){
            storagesChecked = savedInstanceState.getBooleanArray(KEY_BUNDLE_STORAGES_CHECKED);
        }

        for(int i = 0; i < storages.size(); i++){
            //Creo la Checkbox con l'inflater perchè se la creo tramite codice da problemi con Android 4
            final CheckBox checkBox = (CheckBox)inflater.inflate(R.layout.checkbox, layoutStorages, false);
            checkBox.setText(storages.get(i).titolo);
            if(storagesChecked == null) {
                //al primo avvio del fragment imposto la memoria interna come già selezionata
                checkBox.setChecked(i == 0);
            } else if(i < storagesChecked.length){
                checkBox.setChecked(storagesChecked[i]);
            }
            layoutStorages.addView(checkBox);
            mapStorages.put(checkBox, storages.get(i));
        }

        //ricerca
        cercaEditText = v.findViewById(R.id.edittext_cerca);
        cercaEditText.requestFocus();
        final ImageButton buttonCerca = v.findViewById(R.id.button_cerca);
        buttonCerca.setOnClickListener(this);
        activityMain.nascondiTastiera();

        //checkbox
        checkboxCercaImmagini = v.findViewById(R.id.checkbox_cerca_immagini);
        checkboxCercaVideo = v.findViewById(R.id.checkbox_cerca_video);
        checkboxCercaAudio = v.findViewById(R.id.checkbox_cerca_audio);
        checkboxCercaAltri = v.findViewById(R.id.checkbox_cerca_altri);
        checkboxIgnoreCase = v.findViewById(R.id.checkbox_ignore_case);

        //dimensione
        radioDimensioniTutte = v.findViewById(R.id.radio_dimensioni_tutte);
        radioDimensioniTutte.setOnCheckedChangeListener(this);
        radioDimensioniTutte.setOnClickListener(this);
        radioDimensioniMinori = v.findViewById(R.id.radio_dimensioni_minori);
        radioDimensioniMinori.setOnCheckedChangeListener(this);
        radioDimensioniMinori.setOnClickListener(this);
        radioDimensioniMaggiori = v.findViewById(R.id.radio_dimensioni_maggiori);
        radioDimensioniMaggiori.setOnCheckedChangeListener(this);
        radioDimensioniMaggiori.setOnClickListener(this);
        spinnerDimensioniMinori = v.findViewById(R.id.spinner_umisura_dimensione_minori);
        ViewsExtKt.popola(spinnerDimensioniMinori, Costanti.ARRAY_BYTES_IDS);
        spinnerDimensioniMinori.setEnabled(false);
        spinnerDimensioniMaggiori = v.findViewById(R.id.spinner_umisura_dimensione_maggiori);
        ViewsExtKt.popola(spinnerDimensioniMaggiori, Costanti.ARRAY_BYTES_IDS);
        spinnerDimensioniMaggiori.setEnabled(false);
        editTextDimensioniMinori = v.findViewById(R.id.edittext_dimensione_minori);
        editTextDimensioniMaggiori = v.findViewById(R.id.edittext_dimensione_maggiori);

        return v;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_cerca:
                //configuro i parametri di ricerca e avvio la ricerca
                final String query = cercaEditText.getText().toString().trim();
                final List<File> percorsiDiRicerca = new ArrayList<>();
                for(CheckBox checkBox : mapStorages.keySet()){
                    if(checkBox.isChecked()){
                        percorsiDiRicerca.add(mapStorages.get(checkBox).startDirectory);
                    }
                }
                final ParametriRicerca parametriRicerca = new ParametriRicerca(query, percorsiDiRicerca);
                parametriRicerca.setTipiFiles(checkboxCercaImmagini.isChecked(), checkboxCercaVideo.isChecked(), checkboxCercaAudio.isChecked(), checkboxCercaAltri.isChecked());
                parametriRicerca.setCaseSensistive(!checkboxIgnoreCase.isChecked());
                try {
                    if (radioDimensioniTutte.isChecked()) {
                        parametriRicerca.setDimensione(ParametriRicerca.DIMENSIONI_TUTTE, 0, 0);
                    } else if (radioDimensioniMinori.isChecked()) {
                        parametriRicerca.setDimensione(ParametriRicerca.DIMENSIONI_MINORI, Long.parseLong(editTextDimensioniMinori.getText().toString()), spinnerDimensioniMinori.getSelectedItemPosition());
                    } else if (radioDimensioniMaggiori.isChecked()) {
                        parametriRicerca.setDimensione(ParametriRicerca.DIMENSIONI_MAGGIORI, Long.parseLong(editTextDimensioniMaggiori.getText().toString()), spinnerDimensioniMaggiori.getSelectedItemPosition());
                    }
                } catch (Exception ignored){}
                activityMain.nascondiTastiera();

                if(parametriRicerca.isValid()) {
                    activityMain.showFragment(FragmentRisultatiRicercaFiles.getInstance(parametriRicerca));
                } else {
                    CustomDialogBuilder.make(getContext(), R.string.controlla_parametri_inseriti, CustomDialogBuilder.TYPE_ERROR).show();
                }
                break;
            case R.id.radio_dimensioni_tutte:
                //imposto manualmente la possibilità di essere selezionata solo una radio per volta (perchè le radio si trovano dentro un linearlayout)
                radioDimensioniTutte.setChecked(true);
                radioDimensioniMinori.setChecked(false);
                radioDimensioniMaggiori.setChecked(false);
                break;
            case R.id.radio_dimensioni_minori:
                //imposto manualmente la possibilità di essere selezionata solo una radio per volta (perchè le radio si trovano dentro un linearlayout)
                radioDimensioniTutte.setChecked(false);
                radioDimensioniMinori.setChecked(true);
                radioDimensioniMaggiori.setChecked(false);
                break;
            case R.id.radio_dimensioni_maggiori:
                //imposto manualmente la possibilità di essere selezionata solo una radio per volta (perchè le radio si trovano dentro un linearlayout)
                radioDimensioniTutte.setChecked(false);
                radioDimensioniMinori.setChecked(false);
                radioDimensioniMaggiori.setChecked(true);
                break;
        }
    }


    /**
     * Quando la radio viene clickata abilita tutte le view relative allo stesso layout disabilitando le altre
     * @param compoundButton RadioButton
     * @param checked Checked
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        editTextDimensioniMinori.setEnabled(radioDimensioniMinori.isChecked());
        spinnerDimensioniMinori.setEnabled(radioDimensioniMinori.isChecked());
        spinnerDimensioniMinori.setClickable(radioDimensioniMinori.isChecked());
        editTextDimensioniMaggiori.setEnabled(radioDimensioniMaggiori.isChecked());
        spinnerDimensioniMaggiori.setEnabled(radioDimensioniMaggiori.isChecked());
        spinnerDimensioniMaggiori.setClickable(radioDimensioniMaggiori.isChecked());
    }


    /**
     * Chiamato prima di distruggere la view, ad esempio quando un altro fragment viene visualizzato al posto suo
     */
    @Override
    public void onDestroyView() {
        //prima di distruggere la view (quando non è più visibile) salvo gli storages selezionati per ripristinarli quando la view diventa di nuovo visibile
        storagesChecked = getStoragesChecked();
        super.onDestroyView();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            outState.putBooleanArray(KEY_BUNDLE_STORAGES_CHECKED, getStoragesChecked());
        } catch (Exception e){
            e.printStackTrace();
        }
        super.onSaveInstanceState(outState);
    }


    /**
     * Ottiene un array con il "valore checked" delle checkbox relative agli storage. Da ripristinare in seguito alla rotazione del dispositivo o quando la view ritorna nuovamente visibile
     * @return Valori delle checkbox
     */
    private boolean[] getStoragesChecked(){
        final boolean[] storagesChecked = new boolean[layoutStorages.getChildCount()];
        for(int i=0; i < layoutStorages.getChildCount(); i++){
            storagesChecked[i] = ((CheckBox)layoutStorages.getChildAt(i)).isChecked();
        }
        return storagesChecked;
    }
}
