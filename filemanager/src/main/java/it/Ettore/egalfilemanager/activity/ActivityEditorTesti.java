package it.Ettore.egalfilemanager.activity;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.Ettore.androidutilsx.ext.ViewsExtKt;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.FocusUtils;
import it.Ettore.androidutilsx.utils.ViewUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.dialog.DialogFileChooserBuilder;
import it.Ettore.egalfilemanager.dialog.SelectStorageDialogBuilder;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.fileutils.ChiediTreeUriTask;
import it.Ettore.egalfilemanager.fileutils.SAFUtils;
import it.Ettore.egalfilemanager.fileutils.StoragesUtils;
import it.Ettore.egalfilemanager.fileutils.UriUtils;
import it.Ettore.egalfilemanager.home.HomeNavigationManager;
import it.Ettore.egalfilemanager.mediastore.MediaUtils;
import it.Ettore.egalfilemanager.texteditor.ReadTextFileTask;
import it.Ettore.egalfilemanager.texteditor.SaveTextFileTask;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE;
import static android.util.TypedValue.COMPLEX_UNIT_SP;
import static it.Ettore.egalfilemanager.Costanti.KEY_BUNDLE_NUOVO_FILE;
import static it.Ettore.egalfilemanager.Costanti.KEY_PREF_EDITOR_TEXT_SIZE;


/**
 * Activity per la visualizzazione e modifica dei files di testo
 */
public class ActivityEditorTesti extends BaseActivity implements View.OnClickListener, ReadTextFileTask.ReadTextFileListener, SaveTextFileTask.SaveTextFileListener {
    private static final int DEFAULT_TEXT_SIZE = 14;
    private EditText editTextFileContent, editTextRicerca;
    private ScrollView scrollView;
    private LinearLayout layoutRicerca;
    private boolean modalitaModifica, modificheNonSalvate, terminaDopoIlSalvataggio, nuovoFile;
    private List<Integer> listaIndiciRicerca;
    private int ultimoIndiceListaRicerca = -1;
    private File file;
    private ReadTextFileTask readTextFileTask;
    private StoragesUtils storagesUtils;



    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_testi);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        FocusUtils.startMonitoring(this);

        scrollView = findViewById(R.id.scrollview);
        editTextFileContent = findViewById(R.id.editText_file_content);
        int textSize = getPrefs().getInt(KEY_PREF_EDITOR_TEXT_SIZE, DEFAULT_TEXT_SIZE);
        editTextFileContent.setTextSize(COMPLEX_UNIT_SP, textSize);

        layoutRicerca = findViewById(R.id.layout_ricerca);
        editTextRicerca = findViewById(R.id.editText_ricerca);
        editTextRicerca.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        editTextRicerca.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //quando cambia il testo azzero gli indici
                listaIndiciRicerca = null;
                ultimoIndiceListaRicerca = -1;
            }
        });

        final LinearLayout layoutIndietro = findViewById(R.id.layout_ricerca_indietro);
        layoutIndietro.setOnClickListener(this);
        final LinearLayout layoutAvanti = findViewById(R.id.layout_ricerca_avanti);
        layoutAvanti.setOnClickListener(this);

        storagesUtils = new StoragesUtils(this);

        if(getIntent().getBooleanExtra(KEY_BUNDLE_NUOVO_FILE, false)){
            //se è un nuovo file
            nuovoFile = true;
            abilitaModalitaModifica();
            final File tmpFile = new File(getCacheDir(), getString(R.string.nuovo_file) + ".txt");
            settaTitolo(tmpFile.getName());
            onReadFile(tmpFile, true);
        } else {
            //lettura file
            final String type = getIntent().getType();
            if (type != null && type.startsWith("text/")) {
                final Uri fileUri = getIntent().getData();
                final File file = UriUtils.uriToFile(this, fileUri);
                if (file != null) {
                    settaTitolo(file.getName());
                    readTextFileTask = new ReadTextFileTask(this, file, editTextFileContent, this);
                    readTextFileTask.execute();
                } else {
                    notifyError(false);
                }
            } else {
                notifyError(false);
            }
        }
    }


    /**
     * Chiamato quando termina la lettura del file
     * @param file File letto
     * @param success Successo della lettura
     */
    @Override
    public void onReadFile(final File file, boolean success) {
        if(success){
            this.file = file;
            ViewsExtKt.scrollToTop(scrollView);
            invalidateOptionsMenu();

            editTextFileContent.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {}

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(!modificheNonSalvate){
                        modificheNonSalvate = true;
                        settaTitolo("*" + file.getName());
                    }
                }
            });

            final ViewGroup actionBarView = ViewUtils.getActionBarView(this);
            if(actionBarView != null && actionBarView.getChildCount() > 1){
                actionBarView.getChildAt(1).requestFocus(); //focus sul tasto home della barra
            }

        } else {
            notifyError(false);
        }
    }



    @Override
    protected void onDestroy() {
        if(readTextFileTask != null){
            readTextFileTask.dismissDialog();
        }
        super.onDestroy();
    }

    /**
     * Salva il file
     * @param file File da salvare
     */
    private void salvaFile(File file){
        if(file == null){
            return;
        }

        //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
        //non eseguo il ChiediTreeUriTask perchè il salvataggio deve avvenire dopo la richiesta
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && storagesUtils.isOnExtSdCard(file.getParentFile()) &&
                !SAFUtils.isWritableNormalOrSaf(ActivityEditorTesti.this, file.getParentFile())){
            chiediTreeUriSdEsterna(file.getParentFile());
        }

        new SaveTextFileTask(this, file, editTextFileContent.getText().toString(), this).execute();
    }


    /**
     * Chiamato al termine del salvataggio del file
     * @param savedFile File salvato
     * @param success True se il file è stato salvato correttamente
     */
    @Override
    public void onFileSaved(File savedFile, boolean success) {
        if(success){
            new MediaUtils(this).addFileToMediaLibrary(savedFile, null);
            modificheNonSalvate = false;
            file = savedFile;
            settaTitolo(file.getName());
            nuovoFile = false;
            if(terminaDopoIlSalvataggio){
                finish();
            }
        }
    }



    /**
     * Mostra la dialog per la scelta dello storage
     */
    private void mostraDialogStorage(){
        if(file == null){
            return;
        }
        final SelectStorageDialogBuilder builder = new SelectStorageDialogBuilder(this);
        builder.setTitle(R.string.seleziona_destinazione);
        builder.hideIcon(true);
        builder.setStorageItems(new HomeNavigationManager(this).listaItemsArchivioLocale());
        builder.setCancelable(false);
        builder.setSelectStorageListener(new SelectStorageDialogBuilder.SelectStorageListener() {
            @Override
            public void onSelectStorage(File storagePath) {
                //dopo aver selezionato lo storage, seleziono la destinazione
                final DialogFileChooserBuilder fileChooser = new DialogFileChooserBuilder(ActivityEditorTesti.this, DialogFileChooserBuilder.TYPE_SAVE_FILE);
                fileChooser.setTitle(R.string.seleziona_destinazione);
                fileChooser.setCancelable(false);
                fileChooser.setStartFolder(storagePath);
                fileChooser.setFileName(file.getName());
                fileChooser.setChooserListener(new DialogFileChooserBuilder.DialogFileChooserListener() {
                    @Override
                    public void onFileChooserSelected(final File selected) {
                        if(!selected.exists()){
                            salvaFile(selected);
                        } else {
                            final CustomDialogBuilder builder = new CustomDialogBuilder(ActivityEditorTesti.this);
                            builder.setType(CustomDialogBuilder.TYPE_WARNING);
                            builder.setMessage(getString(R.string.sovrascrivi_file, selected.getName()));
                            builder.setPositiveButton(R.string.sovrascrivi, (dialogInterface, i) -> salvaFile(selected));
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.create().show();
                        }
                    }

                    @Override
                    public void onFileChooserCanceled() {}
                });
                fileChooser.create().show();

                //su lollipop se non è possibile scrivere sulla sd esterna chiedo il tree uri
                new ChiediTreeUriTask(ActivityEditorTesti.this, storagePath, true).execute();
            }

            @Override
            public void onCancelStorageSelection(){}
        });
        builder.showSelectDialogIfNecessary();
    }


    /**
     * Mostra la dialog di errore, e nascondo il menu
     */
    private void notifyError(boolean interrotto){
        try {
            if(interrotto) {
                //lettura parziale a causa di un'interruzione della lettura da parte dell'utente
                ColoredToast.makeText(this, R.string.operazione_annulata, Toast.LENGTH_LONG).show();
            } else {
                //non è stato possibile leggere il file a causa di un errore
                if(isFinishing()){
                    ColoredToast.makeText(this, R.string.impossibile_leggere_file, Toast.LENGTH_LONG).show();
                } else {
                    CustomDialogBuilder.make(this, R.string.impossibile_leggere_file, CustomDialogBuilder.TYPE_ERROR).show();
                }
            }
            file = null;
            invalidateOptionsMenu();
        } catch (Exception ignored){}
    }


    /**
     * Mostro la dialog per la modifica della dimensione del carattere
     */
    private void showDialogTextSize(){
        final CustomDialogBuilder dialogBuilder = new CustomDialogBuilder(this);
        dialogBuilder.setTitle(R.string.dimensione_testo);
        dialogBuilder.hideIcon(true);
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_text_size, null);
        final TextView textView = view.findViewById(R.id.textview);
        final SeekBar seekBar = view.findViewById(R.id.seekBar);
        int textSize = getPrefs().getInt(KEY_PREF_EDITOR_TEXT_SIZE, DEFAULT_TEXT_SIZE);
        textView.setText(String.format("%s %s", String.valueOf(textSize), getString(R.string.text_size_sp)));
        final int minSeekBar = 5, maxSeekBar = 20; //la seekbar arriverà a 25 (20+5)
        seekBar.setMax(maxSeekBar);
        int differenza = textSize - minSeekBar;
        if(differenza >= 0 && differenza <= maxSeekBar) {
            seekBar.setProgress(differenza);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int textSize = progress + minSeekBar;
                textView.setText(String.format("%s %s", String.valueOf(textSize), getString(R.string.text_size_sp)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        dialogBuilder.setView(view);
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int textSize = seekBar.getProgress() + minSeekBar;
                getPrefs().edit().putInt(KEY_PREF_EDITOR_TEXT_SIZE, textSize).apply();
                editTextFileContent.setTextSize(COMPLEX_UNIT_SP, textSize);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);
        dialogBuilder.create().show();
    }


    /**
     * Cerca l'occorrenza di una parola e restituisce una lista con tutti gli indici in cui si trova quella parola
     * @param fullText Testo in cui effettuare la ricerca
     * @param word Parola da cercare
     * @return Lista con tutti gli indici in cui si trova quella parola
     */
    private List<Integer> cercaIndiciStringa(String fullText, String word){
        final List<Integer> listaIndici = new ArrayList<>();
        if(word.isEmpty() || fullText.isEmpty()){
            return listaIndici;
        }
        for (int i = -1; (i = fullText.indexOf(word, i + 1)) != -1; i++) {
            listaIndici.add(i);
        }
        return listaIndici;
    }


    /**
     * Prima di chiudere l'activity verifica se il file è stato modificato e non è stato salvato.
     * In caso positivo chiede il salvataggio con una dialo
     */
    private void verificaSalvataggioPrimaDiChiudere(){
        if(modificheNonSalvate){
            final CustomDialogBuilder customDialogBuilder = new CustomDialogBuilder(this);
            customDialogBuilder.setType(CustomDialogBuilder.TYPE_WARNING);
            customDialogBuilder.setMessage(R.string.chiudere_senza_salvare);
            customDialogBuilder.setPositiveButton(R.string.salva, (dialogInterface, i) -> {
                if(nuovoFile){
                    mostraDialogStorage();
                } else {
                    salvaFile(file);
                }
                terminaDopoIlSalvataggio = true;
            });
            customDialogBuilder.setNegativeButton(R.string.non_salvare, (dialogInterface, i) -> finish() );
            customDialogBuilder.create().show();
        } else {
            finish();
        }
    }


    /**
     * Crea una dialog da visualizzare quando si apre un nuovo file se quello corrente non è stato salvato, visto che verrà chiuso.
     */
    private void chiediSalvataggioPrimaDiNuovoFile(){
        final CustomDialogBuilder customDialogBuilder = new CustomDialogBuilder(this);
        customDialogBuilder.setType(CustomDialogBuilder.TYPE_WARNING);
        customDialogBuilder.setMessage(R.string.chiudere_senza_salvare);
        customDialogBuilder.setPositiveButton(R.string.salva, (dialogInterface, i) -> {
            if(nuovoFile){
                mostraDialogStorage();
            } else {
                salvaFile(file);
            }
        });
        customDialogBuilder.setNegativeButton(R.string.non_salvare, (dialogInterface, i) -> nuovoFile() );
        customDialogBuilder.create().show();
    }


    /**
     * Apre una nuova activity per la creazione di un file nuovo e chiude l'activity corrente
     * Se non si chiude l'activity e contiene molti dati, all'apertura della nuova sarà generata una TransactionTooLargeException
     */
    private void nuovoFile(){
        final Intent nuovoFileIntent = new Intent(this, ActivityEditorTesti.class);
        nuovoFileIntent.putExtra(KEY_BUNDLE_NUOVO_FILE, true);
        startActivity(nuovoFileIntent);
        finish();
    }


    /**
     * Azione da eseguire alla pressione del tasto indietro
     */
    @Override
    public void onBackPressed() {
        verificaSalvataggioPrimaDiChiudere();
    }


    /**
     * Gestione del tasti ricerca avanti e ricerca indietro
     * @param v View
     */
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.layout_ricerca_avanti:
                nascondiTastiera();
                if(listaIndiciRicerca == null) {
                    listaIndiciRicerca = cercaIndiciStringa(editTextFileContent.getText().toString(), editTextRicerca.getText().toString());
                }
                if(!listaIndiciRicerca.isEmpty()) {
                    ultimoIndiceListaRicerca++;
                    if(ultimoIndiceListaRicerca >= listaIndiciRicerca.size()){
                        ultimoIndiceListaRicerca = 0;
                    }
                    final int startSelection = listaIndiciRicerca.get(ultimoIndiceListaRicerca);
                    final int endSelection = startSelection + editTextRicerca.getText().toString().length();
                    editTextFileContent.requestFocus();
                    editTextFileContent.setSelection(startSelection, endSelection);
                } else {
                    ColoredToast.makeText(ActivityEditorTesti.this, R.string.occorrenza_non_trovata, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.layout_ricerca_indietro:
                if(listaIndiciRicerca == null) {
                    listaIndiciRicerca = cercaIndiciStringa(editTextFileContent.getText().toString(), editTextRicerca.getText().toString());
                }
                if(!listaIndiciRicerca.isEmpty()) {
                    ultimoIndiceListaRicerca--;
                    if(ultimoIndiceListaRicerca < 0){
                        ultimoIndiceListaRicerca = listaIndiciRicerca.size()-1;
                    }
                    final int startSelection = listaIndiciRicerca.get(ultimoIndiceListaRicerca);
                    final int endSelection = startSelection + editTextRicerca.getText().toString().length();
                    editTextFileContent.requestFocus();
                    try {
                        editTextFileContent.setSelection(startSelection, endSelection);
                    } catch (ArrayIndexOutOfBoundsException e){
                        //in rari casi potrebbe causare un'eccezione
                        e.printStackTrace();
                    }
                } else {
                    ColoredToast.makeText(ActivityEditorTesti.this, R.string.occorrenza_non_trovata, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    /**
     * Abilita la modalità di modifica
     */
    private void abilitaModalitaModifica(){
        modalitaModifica = true;
        invalidateOptionsMenu();
        editTextFileContent.setClickable(true);
        editTextFileContent.setCursorVisible(true);
        editTextFileContent.requestFocus();
        editTextFileContent.setInputType(TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_MULTI_LINE);
        final InputMethodManager imm = (InputMethodManager) ActivityEditorTesti.this.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editTextFileContent, 0);
        editTextFileContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imm.showSoftInput(editTextFileContent, 0);
            }
        });
        editTextFileContent.setSelection(0);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(file != null) {
            getMenuInflater().inflate(R.menu.menu_editor_testi, menu);
            menu.findItem(R.id.modifica).setVisible(!modalitaModifica);
            menu.findItem(R.id.salva).setVisible(modalitaModifica);
            menu.findItem(R.id.salva_con_nome).setVisible(modalitaModifica);
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                verificaSalvataggioPrimaDiChiudere();
                return true;
            case R.id.modifica:
                abilitaModalitaModifica();
                return true;
            case R.id.salva:
                if(nuovoFile || FileUtils.fileIsInCache(file, this)){
                    mostraDialogStorage();
                } else {
                    salvaFile(file);
                }
                return true;
            case R.id.salva_con_nome:
                mostraDialogStorage();
                return true;
            case R.id.textsize:
                showDialogTextSize();
                return true;
            case R.id.ricerca:
                item.setChecked(!item.isChecked());
                if(item.isChecked()){
                    layoutRicerca.setVisibility(View.VISIBLE);
                    editTextRicerca.requestFocus();
                } else {
                    layoutRicerca.setVisibility(View.GONE);
                    listaIndiciRicerca = null;
                    ultimoIndiceListaRicerca = -1;
                    editTextRicerca.setText("");
                }
                return true;
            case R.id.proprieta:
                if(file != null) {
                    final List<File> listaFiles = new ArrayList<>(1);
                    listaFiles.add(file);
                    new FileManager(this).mostraProprietaCategoria(listaFiles);
                }
                return true;
            case R.id.nuovo_file:
                if(modificheNonSalvate){
                    chiediSalvataggioPrimaDiNuovoFile();
                } else {
                    nuovoFile();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
