package it.Ettore.egalfilemanager.view;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;


/**
 * View della dialog proprietà file
 */
public class ViewProprieta extends LinearLayout {
    private boolean singoloFile;
    private ImageView iconaImageView;
    private TextView nomeTextView, percorsoTextView, contenutoTextView, dimensioneTextView, tipoTextView, dataTextView, permessiUnixTextView;
    private TableRow tipoTableRow, dataTableRow, permessiNormaliTableRow, permessiRootTableRow, nascostoTableRow, percorsoTableRow, contenutoTableRow;
    private CheckBox letturaCheckBox, scritturaCheckBox, nascostoCheckBox;
    private Button modificaPermessiButton;


    private ViewProprieta(@NonNull Context context){
        super(context);
    }


    /**
     *
     * @param context Context chiamante
     * @param singoloFile True se la dialog mostrerà i dati di un file solo. False se deve mostrare i dati di files multipli
     */
    public ViewProprieta(Context context, boolean singoloFile) {
        this(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_proprieta, null);
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(param);
        addView(view);

        iconaImageView = view.findViewById(R.id.image_view_tipo);
        nomeTextView = view.findViewById(R.id.text_view_nome);
        percorsoTextView = view.findViewById(R.id.text_view_percorso);
        contenutoTextView = view.findViewById(R.id.text_view_contenuto);
        dimensioneTextView = view.findViewById(R.id.text_view_dimensione);
        tipoTextView = view.findViewById(R.id.text_view_tipo);
        dataTextView = view.findViewById(R.id.text_view_data);
        permessiUnixTextView = view.findViewById(R.id.text_view_permessi_unix);
        modificaPermessiButton = view.findViewById(R.id.button_modifica_permessi);
        modificaPermessiButton.setVisibility(View.GONE);
        letturaCheckBox = view.findViewById(R.id.checkbox_lettura);
        scritturaCheckBox = view.findViewById(R.id.checkbox_scrittura);
        nascostoCheckBox = view.findViewById(R.id.checkbox_nascosto);

        tipoTableRow = view.findViewById(R.id.tablerow_tipo);
        dataTableRow = view.findViewById(R.id.tablerow_data);
        percorsoTableRow = view.findViewById(R.id.tablerow_percorso);
        permessiNormaliTableRow = view.findViewById(R.id.tablerow_permessi_normali);
        permessiNormaliTableRow.setVisibility(View.GONE);
        permessiRootTableRow = view.findViewById(R.id.tablerow_permessi_root);
        permessiRootTableRow.setVisibility(View.GONE);
        nascostoTableRow = view.findViewById(R.id.tablerow_nascosto);
        contenutoTableRow = view.findViewById(R.id.tablerow_contenuto);

        setSingoloFile(singoloFile);
    }


    /**
     * Imposta i parametri per visualizzare un singolo file o per la visualizzazione files multipli
     * @param singoloFile True se la dialog mostrerà i dati di un file solo. False se deve mostrare i dati di files multipli
     */
    private void setSingoloFile(boolean singoloFile){
        this.singoloFile = singoloFile;
        if(!singoloFile){
            tipoTableRow.setVisibility(View.GONE);
            dataTableRow.setVisibility(View.GONE);
            contenutoTableRow.setVisibility(View.GONE);
            permessiNormaliTableRow.setVisibility(View.GONE);
            permessiRootTableRow.setVisibility(View.GONE);
            nascostoTableRow.setVisibility(View.GONE);
        }
    }


    /**
     * Mostra l'icona passata se la view sta gestendo un unico file
     * @param resIdIcona Risorsa dell'icona
     */
    private void setIcona(@DrawableRes int resIdIcona){
        if(singoloFile) {
            iconaImageView.setImageResource(resIdIcona);
        } else {
            iconaImageView.setImageResource(R.drawable.ico_files_multipli);
        }
    }


    /**
     * Mostra il nome del file se la view sta gestendo un unico file
     * @param nomeFile Nome del file
     */
    private void setNomeFile(String nomeFile){
        if(singoloFile) {
            nomeTextView.setText(nomeFile);
        } else {
            nomeTextView.setText(R.string.selezione_multipla);
        }
    }


    /**
     * Mostra il percorso passato
     * @param percorso Percorso da mostrare. Null se si vuole nascondere l'intera riga
     */
    public void setPercorso(String percorso){
        if(percorso != null){
            percorsoTextView.setText(percorso);
        } else {
            percorsoTableRow.setVisibility(View.GONE);
        }
    }


    /**
     * Mostra il nome del file, il tipo e la relativa icona
     * @param nomeFile Nome del file
     * @param isDirectory True se è una directory
     * @param isSymlink True se è un link
     */
    public void setFile(@NonNull String nomeFile, boolean isDirectory, boolean isSymlink){
        String tipo;
        if(!isDirectory){
            setIcona(IconManager.iconForFile(nomeFile));
            final String mime = FileUtils.getMimeType(nomeFile);
            if(mime.equals("*/*")) {
                tipo = getContext().getString(R.string.file);
            } else {
                tipo = mime;
            }
            contenutoTableRow.setVisibility(View.GONE);
        } else {
            setIcona(R.drawable.ico_cartella);
            tipo = getContext().getString(R.string.directory);
        }
        if(isSymlink){
            tipo = String.format("%s (%s)", tipo, getContext().getString(R.string.collegamento));
        }
        tipoTextView.setText(tipo);
        setNomeFile(nomeFile);
    }


    /**
     * Mostra la dimensione del file in formato leggibile
     * @param dimensione Dimensione in bytes
     */
    public void setDimensione(long dimensione){
        dimensioneTextView.setText(dimensioneFile(dimensione));
    }


    /**
     * Mostra la data in formato leggibile
     * @param data Millis dal 1970
     */
    public void setData(long data){
        try {
            final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT, Locale.getDefault());
            dataTextView.setText(dateFormat.format(data));
        } catch (Exception e){
            final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.DEFAULT, Locale.ENGLISH);
            dataTextView.setText(dateFormat.format(data));
        }
    }


    /**
     * Ottiene la dimensione del file in formato leggibile
     * @param bytes Dimensione file in bytes
     * @return Dimensione file human readable
     */
    private String dimensioneFile(long bytes){
        final int[] arrayBytesIds = {R.string.unit_byte, R.string.unit_kilobyte, R.string.unit_megabyte, R.string.unit_gigabyte, R.string.unit_terabyte};
        final String[] arrayBytes = MyUtils.stringResToStringArray(getContext(), arrayBytesIds);
        return String.format("%s  (%s %s)", MyMath.humanReadableByte(bytes, arrayBytes), NumberFormat.getInstance().format(bytes), getContext().getString(R.string.bytes));
    }


    /**
     * Mostra la visualizzazione dei permessi nelle checkbox
     * @param canRead True se il file è leggibile
     * @param canWrite True se il file è scrivibile
     */
    public void setPermessiNormali(boolean canRead, boolean canWrite){
        if(singoloFile) {
            permessiNormaliTableRow.setVisibility(View.VISIBLE);
            letturaCheckBox.setChecked(canRead);
            scritturaCheckBox.setChecked(canWrite);
        }
    }


    /**
     * Mostra la visualizzazione dei permessi nel formato stringa Unix
     * @param permessi Stringa da mostrare
     */
    public void setPermessiUnix(String permessi){
        if(singoloFile) {
            permessiRootTableRow.setVisibility(View.VISIBLE);
            permessiUnixTextView.setText(permessi);
        }
    }


    /**
     * Abilita la modifica dei permessi mostrando l'apposito pulsante
     * @param onClickListener Listener da eseguire al click del pulsante
     */
    public void setPermessiUnixModificabili(View.OnClickListener onClickListener){
        if(singoloFile) {
            modificaPermessiButton.setVisibility(View.VISIBLE);
            modificaPermessiButton.setOnClickListener(onClickListener);
        }
    }


    /**
     * Mostra su una checkbox se il file è nascosto
     * @param nascosto True se il file è nascosto
     * @param modificabile True le la checkbox deve essere modificabile
     */
    public void setNascosto(boolean nascosto, boolean modificabile){
        nascostoCheckBox.setChecked(nascosto);
        nascostoCheckBox.setEnabled(modificabile);
    }


    /**
     * Restituisce lo stato della checkbox nascosto
     * @return True se la checkbox nascosto è clickata
     */
    public boolean nascostoCheckBoxIsChecked(){
        return nascostoCheckBox.isChecked();
    }


    /**
     * Mostra il contenuto della cartella
     * @param totFiles Numero di files
     * @param totCartelle Numero di cartelle
     */
    public void setContenuto(int totFiles, int totCartelle){
        contenutoTextView.setText(String.format("%s %s, %s %s",
                String.valueOf(totFiles), getContext().getString(R.string.files), String.valueOf(totCartelle), getContext().getString(R.string.directories)));
    }


    /**
     * Nasconde la riga relativa alle informazioni del file nascosto
     */
    public void nascondiRigaFileNascosto(){
        nascostoTableRow.setVisibility(View.GONE);
    }
}
