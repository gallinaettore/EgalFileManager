package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_MOSTRA_ANTEPRIME;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_MOSTRA_NASCOSTI;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ORDINA_DOWNLOAD_PER_DATA;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_ROOT_EXPLORER;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_TEMA;
import static it.Ettore.egalfilemanager.Costanti.PREF_VALUE_TEMA_DARK;
import static it.Ettore.egalfilemanager.Costanti.PREF_VALUE_TEMA_LIGHT;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.Ettore.androidutilsx.lang.LanguageManager;
import it.Ettore.androidutilsx.ui.MyActivity;
import it.Ettore.androidutilsx.utils.PackageUtils;
import it.Ettore.androidutilsx.utils.RootUtils;
import it.Ettore.egalfilemanager.BackupPreferencesUtils;
import it.Ettore.egalfilemanager.Lingue;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogGiorniFilesRecentiBuilder;
import it.Ettore.egalfilemanager.home.HomeItem;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.iconmanager.glide.ClearGlideCacheTask;
import it.Ettore.egalfilemanager.mediastore.ScanMediaLibraryService;
import it.Ettore.egalfilemanager.view.CustomListPreference;
import it.Ettore.egalfilemanager.view.ViewUtils;
import it.Ettore.materialpreferencesx.ListPreference;
import it.Ettore.materialpreferencesx.Preference;
import it.Ettore.materialpreferencesx.PreferenceCategory;
import it.Ettore.materialpreferencesx.PreferenceScreen;
import it.Ettore.materialpreferencesx.SwitchPreference;


/**
 * Fragment per la modifica delle impostazioni
 */
public class FragmentImpostazioni extends GeneralFragment {

    public FragmentImpostazioni(){}



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final ActivityMain activityMain = (ActivityMain)getActivity();
        activityMain.settaTitolo(R.string.impostazioni);

        final PreferenceScreen preferenceScreen = new PreferenceScreen(getContext());
        final PreferenceCategory generalCategory = new PreferenceCategory(getContext(), R.string.impostazioni_generali);
        final PreferenceCategory avanzateCategory = new PreferenceCategory(getContext(), R.string.avanzate);
        final PreferenceCategory backupCategory = new PreferenceCategory(getContext(), R.string.backup);

        //Lingue
        final ListPreference preferenceLingue = new ListPreference(getContext(), R.string.lingua, getPrefs(), LanguageManager.KEY_LANGUAGE);
        final LanguageManager languageManager = new LanguageManager(getContext(), Lingue.getValues());
        final List<String> lingue = languageManager.getNomiLingue();
        preferenceLingue.setEntries(lingue);
        preferenceLingue.setEntryValues(languageManager.getCodiciLocaleLingue());
        preferenceLingue.setValue(languageManager.getCurrentAppLanguage().getCodiceLocale());
        preferenceLingue.setPreferenceChangeListener((preference, newValue) -> {
            if(getActivity() != null) {
                getActivity().recreate();
            }
            return false;
        });
        preferenceLingue.showSettedValueInSummary();
        generalCategory.addPreference(preferenceLingue);

        //Tema
        final ListPreference preferenceTema = new CustomListPreference(getContext(), R.string.tema, getPrefs(), KEY_PREF_TEMA);
        preferenceTema.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        final String[] nomiTemi = {getString(R.string.tema_chiaro), getString(R.string.tema_scuro)};
        final String[] valoriTemi = {PREF_VALUE_TEMA_LIGHT, PREF_VALUE_TEMA_DARK};
        preferenceTema.setEntries(nomiTemi);
        preferenceTema.setEntryValues(valoriTemi);
        preferenceTema.setDefaultIndex(0);
        preferenceTema.setPreferenceChangeListener((preference, newValue) -> {
            //new PackageUtils(getContext()).restartCurrentPackage();
            getActivity().recreate();
            return false;
        });
        preferenceTema.showSettedValueInSummary();
        generalCategory.addPreference(preferenceTema);

        //Mostra anteprime
        final SwitchPreference preferenceMostraAnteprime = new SwitchPreference(getContext(), R.string.mostra_anteprime, getPrefs(), KEY_PREF_MOSTRA_ANTEPRIME);
        preferenceMostraAnteprime.setSummary(R.string.mostra_anteprime_descrizione);
        preferenceMostraAnteprime.setDefaultChecked(true);
        generalCategory.addPreference(preferenceMostraAnteprime);

        //Mostra nascosti
        final SwitchPreference preferenceMostraNascosti = new SwitchPreference(getContext(), R.string.mostra_nascosti, getPrefs(), KEY_PREF_MOSTRA_NASCOSTI);
        preferenceMostraNascosti.setSummary(R.string.mostra_nascosti_descrizione);
        preferenceMostraNascosti.setDefaultChecked(false);
        generalCategory.addPreference(preferenceMostraNascosti);

        //Ordina downloads per data
        final SwitchPreference preferenceDownloadPerData = new SwitchPreference(getContext(), R.string.ordina_download_per_data, getPrefs(), KEY_PREF_ORDINA_DOWNLOAD_PER_DATA);
        preferenceDownloadPerData.setSummary(R.string.ordina_download_per_data_descrizione);
        preferenceDownloadPerData.setDefaultChecked(true);
        generalCategory.addPreference(preferenceDownloadPerData);

        //Giorni files recenti
        final Preference preferenceGiorniRecenti = new Preference(getContext(), R.string.tool_recenti);
        preferenceGiorniRecenti.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceGiorniRecenti.setSummary(R.string.giorni_recenti_descrizione);
        preferenceGiorniRecenti.setOnClickListener(v -> new DialogGiorniFilesRecentiBuilder(getContext(), null).create().show());
        generalCategory.addPreference(preferenceGiorniRecenti);

        //Root Explorer
        final SwitchPreference preferenceRootExplorer = new SwitchPreference(getContext(), R.string.root_explorer, getPrefs(), KEY_PREF_ROOT_EXPLORER);
        preferenceRootExplorer.setSummary(R.string.root_explorer_descrizione);
        preferenceRootExplorer.setDefaultChecked(false);
        preferenceRootExplorer.setCheckedPreferenceListener(isChecked -> {
            if (isChecked) {
                //chiede e verifica i permessi di root
                boolean rooted = RootUtils.isPhoneRooted();
                if (!rooted) {
                    CustomDialogBuilder.make(getContext(), R.string.no_permessi_root, CustomDialogBuilder.TYPE_ERROR).show();
                    preferenceRootExplorer.changeChecked(false);
                }
            }
            //aggiorno la navigation mostrando o nascondendo la cartella root
            activityMain.aggiornaMenuArchivioLocale();
        });
        avanzateCategory.addPreference(preferenceRootExplorer);

        //Associazione files
        final Preference preferenceAssociazioneFiles = new Preference(getContext(), R.string.associazione_files);
        preferenceAssociazioneFiles.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceAssociazioneFiles.setSummary(R.string.associazione_files_descrizione);
        preferenceAssociazioneFiles.setOnClickListener(v -> {
            //final ActivityMain activityMain1 = (ActivityMain)getActivity();
            //activityMain1.showFragment(new FragmentAssociazioneFiles());
            activityMain.showFragment(new FragmentAssociazioneFiles());
        });
        avanzateCategory.addPreference(preferenceAssociazioneFiles);

        //Rescan Media Library
        final Preference preferenceRescanMediaLibrary = new Preference(getContext(), R.string.rescan_media_library);
        preferenceRescanMediaLibrary.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceRescanMediaLibrary.setSummary(R.string.rescan_media_library_descrizione);
        preferenceRescanMediaLibrary.setOnClickListener(v -> {
            final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
            builder.setTitle(R.string.rescan_media_library);
            builder.hideIcon(true);
            builder.removeTitleSpace(true);
            final LayoutInflater inflater1 = LayoutInflater.from(getContext());
            final View view = inflater1.inflate(R.layout.dialog_storage_scan_library, null);
            final TextView messageTextView = view.findViewById(R.id.text_view_message);
            messageTextView.setText(R.string.storage_to_scan);
            final TextView messageTextView2 = view.findViewById(R.id.text_view_message2);
            messageTextView2.setText(R.string.operazione_lunga);
            final LinearLayout storagesLayout = view.findViewById(R.id.layout_storages);
            final List<HomeItem> storages = new HomeNavigationManager(getActivity()).listaItemsSdCards();
            final Map<CheckBox, HomeItem> mapStorages = new LinkedHashMap<>(storages.size());
            for(int i = 0; i < storages.size(); i++){
                //Creo la Checkbox con l'inflater perchè se la creo tramite codice da problemi con Android 4
                final CheckBox checkBox = (CheckBox) inflater1.inflate(R.layout.checkbox, storagesLayout, false);
                checkBox.setText(storages.get(i).titolo);
                checkBox.setChecked(i == 0);
                storagesLayout.addView(checkBox);
                mapStorages.put(checkBox, storages.get(i));
            }
            builder.setView(view);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                if(!mapStorages.isEmpty() && !ScanMediaLibraryService.isRunning()){
                    final ArrayList<String> pathsStorages = new ArrayList<>();
                    for(Map.Entry<CheckBox, HomeItem> entry : mapStorages.entrySet()){
                        if(entry.getKey().isChecked()){
                            pathsStorages.add(entry.getValue().startDirectory.getAbsolutePath());
                        }
                    }
                    final Intent serviceIntent = ScanMediaLibraryService.createStartIntent(getContext(), pathsStorages);
                    ContextCompat.startForegroundService(activityMain, serviceIntent);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        });
        avanzateCategory.addPreference(preferenceRescanMediaLibrary);

        //Cancella cache
        final Preference preferenceSvuotaCache = new Preference(getContext(), R.string.svuota_cache);
        preferenceSvuotaCache.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceSvuotaCache.setSummary(R.string.svuota_cache_descrizione);
        preferenceSvuotaCache.setOnClickListener(view -> {
            //svuoto la cache dell'app
            final File[] listaFiles = getContext().getCacheDir().listFiles();
            for(File file : listaFiles){
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            //svuoto la cache di Glide
            new ClearGlideCacheTask(getContext()).execute();
        });
        avanzateCategory.addPreference(preferenceSvuotaCache);

        //Reset app
        final Preference preferenceReset = new Preference(getContext(), R.string.reset_app_titolo);
        preferenceReset.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceReset.setSummary(R.string.reset_app_descrizione);
        preferenceReset.setOnClickListener(v -> {
            final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
            builder.setType(CustomDialogBuilder.TYPE_WARNING);
            builder.setMessage(R.string.reset_app_messaggio);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                //Cancella dati
                final PackageUtils pu = new PackageUtils(getContext());
                pu.deleteAllAppData();
                pu.restartCurrentPackage();
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        });
        avanzateCategory.addPreference(preferenceReset);


        //Make Backup
        final Preference preferenceMakeBackup = new Preference(getContext(), R.string.effettua_backup_impostazioni);
        preferenceMakeBackup.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceMakeBackup.setOnClickListener(v -> {
            BackupPreferencesUtils.creaBackupPreferences(getContext());
        });
        backupCategory.addPreference(preferenceMakeBackup);


        //Restore backup
        final Preference preferenceRestoreBackup = new Preference(getContext(), R.string.ripristina_backup_impostazioni);
        preferenceRestoreBackup.setFocusTextColor(getResources().getColor(R.color.material_preference_title_focused_text_color));
        preferenceRestoreBackup.setOnClickListener(v -> {
            BackupPreferencesUtils.ripristinaBackupPreferences(getContext());
        });
        backupCategory.addPreference(preferenceRestoreBackup);


        preferenceScreen.addCategory(generalCategory);
        preferenceScreen.addCategory(avanzateCategory);
        preferenceScreen.addCategory(backupCategory);
        return preferenceScreen;

    }
}
