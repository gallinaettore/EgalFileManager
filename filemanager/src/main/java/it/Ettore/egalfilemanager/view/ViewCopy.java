package it.Ettore.egalfilemanager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * View utilizzata all'interno della dialog di copia
 */
public class ViewCopy extends LinearLayout {
    private TextView textViewDa, textViewA, textViewNomeFile, textViewPercentualeFile, textViewDimensioneFile,
            textViewPercentualeTotale, textViewDimensioneTotale, textViewTotale;
    private ProgressBar progressBarFile, progressBarTotale;

    private int totFiles;
    private long fileSize, totSize;
    private String humanFileSize, humanTotSize;



    public ViewCopy(Context context) {
        super(context);
        init();
    }


    public ViewCopy(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public ViewCopy(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    /**
     * Inizializzazione della view
     */
    private void init(){
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.view_copy, null);
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(param);
        addView(view);
        textViewDa = view.findViewById(R.id.textview_da);
        textViewA = view.findViewById(R.id.textview_a);
        textViewNomeFile = view.findViewById(R.id.textview_nome_file);
        progressBarFile = view.findViewById(R.id.progressbar_file);
        progressBarFile.setMax(100);
        progressBarTotale = view.findViewById(R.id.progressbar_totale);
        progressBarTotale.setMax(100);
        textViewPercentualeFile = view.findViewById(R.id.textview_percentuale_file);
        textViewDimensioneFile = view.findViewById(R.id.textview_dimensione_file);
        textViewPercentualeTotale = view.findViewById(R.id.textview_percentuale_totale);
        textViewDimensioneTotale = view.findViewById(R.id.textview_dimensione_totale);
        textViewTotale = view.findViewById(R.id.textview_totale);

        //imposto dei drawable personalizzati per android 4 (perchè non viene utilizzato il material
        //è comunque possibile anche utulizzarli su android successivi sovrascrivendo quelli di default
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            progressBarFile.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_drawable));
            progressBarFile.setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indeterminate_drawable_horizontal));
            progressBarTotale.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.progress_drawable));
            progressBarTotale.setIndeterminateDrawable(ContextCompat.getDrawable(getContext(), R.drawable.indeterminate_drawable_horizontal));
        }
    }


    /**
     * Mostra l'origine
     * @param text Path di origine
     */
    private void mostraOrigine(String text){
        final String da = getContext().getString(R.string.origine);
        textViewDa.setText(text != null ? String.format("%s %s", da, text) : da);
    }


    /**
     * Mostra la destinazione
     * @param text Path di destinazione
     */
    public void mostraDestinazione(String text){
        final String a = getContext().getString(R.string.destinazione);
        textViewA.setText(text != null ? String.format("%s %s", a, text) : a);
    }



    /**
     * Setta il file che si sta processando
     * @param nomeFile Nome del file da copiare
     * @param parentPath Path della cartella che contiene il path
     */
    public void mostraFileCorrente(String nomeFile, String parentPath){
        textViewNomeFile.setText(nomeFile);
        mostraOrigine(parentPath);
    }


    /**
     * Setta le progress bar indeterminate
     * @param indeterminate Indeterminate
     */
    public void setIndeterminate(boolean indeterminate){
        progressBarFile.setIndeterminate(indeterminate);
        progressBarTotale.setIndeterminate(indeterminate);
    }


    /**
     * Mostra un messaggio
     * @param message Messaggio da mostrare
     */
    public void mostraMessaggio(String message){
        textViewDa.setText(message);
        textViewA.setVisibility(View.GONE);
        textViewNomeFile.setVisibility(View.GONE);
        progressBarFile.setVisibility(View.GONE);
        progressBarTotale.setVisibility(View.GONE);
        textViewPercentualeFile.setVisibility(View.GONE);
        textViewPercentualeTotale.setVisibility(View.GONE);
        textViewDimensioneFile.setVisibility(View.GONE);
        textViewDimensioneTotale.setVisibility(View.GONE);
        textViewTotale.setVisibility(View.GONE);
    }


    /**
     * Imposta il numero totale di files
     * @param totFiles Numero totale di files
     */
    public void setTotFiles(int totFiles){
        this.totFiles = totFiles;
    }


    /**
     * Imposta la dimensione totale dei files da copiare
     * @param totSize Dimensione totale in bytes
     */
    public void setTotSize(long totSize){
        this.totSize = totSize;
        this.humanTotSize = MyMath.humanReadableByte(totSize);
    }


    /**
     * Imposta la dimensione del file corrente
     * @param fileSize Dimensione file in bytes
     */
    public void setFileSize(long fileSize){
        this.fileSize = fileSize;
        this.humanFileSize = MyMath.humanReadableByte(fileSize);
    }


    /**
     * Mostra l'indice del file corrente
     * @param indiceFile Indice file
     */
    public void mostraIndiceFile(int indiceFile){
        if(totFiles > 0 && indiceFile <= totFiles){
            textViewTotale.setText(String.format("%s  %s/%s", getContext().getString(R.string.totale), String.valueOf(indiceFile), String.valueOf(totFiles)));
        } else {
            textViewTotale.setText(R.string.totale);
        }
    }


    /**
     * Mostra il progresso del file corrente
     * @param bytesWritedFile bytes scritti
     */
    public void mostraProgressoFile(long bytesWritedFile){
        if(fileSize > 0 && bytesWritedFile <= fileSize) {
            int percent = (int) (bytesWritedFile * 100 / fileSize);
            progressBarFile.setProgress(percent);
            textViewPercentualeFile.setText(String.format("%s%s", String.valueOf(percent), "%"));
            final String humanProgress = MyMath.humanReadableByte(bytesWritedFile);
            textViewDimensioneFile.setText(String.format("%s / %s", humanProgress, humanFileSize));
        } else {
            progressBarFile.setProgress(0);
            textViewPercentualeFile.setText(null);
            textViewDimensioneFile.setText(null);
        }
    }


    /**
     * Mostra il progresso totale
     * @param bytesWritedTot bytes totali scritti
     */
    public void mostraProgressoTotale(long bytesWritedTot){
        if(totSize > 0 && bytesWritedTot <= totSize){
            int percent = (int) (bytesWritedTot * 100 / totSize);
            progressBarTotale.setProgress(percent);
            textViewPercentualeTotale.setText(String.format("%s%s", String.valueOf(percent), "%"));
            final String humanProgress = MyMath.humanReadableByte(bytesWritedTot);
            textViewDimensioneTotale.setText(String.format("%s / %s", humanProgress, humanTotSize));
        } else {
            progressBarTotale.setProgress(0);
            textViewPercentualeTotale.setText(null);
            textViewDimensioneTotale.setText(null);
        }
    }
}
