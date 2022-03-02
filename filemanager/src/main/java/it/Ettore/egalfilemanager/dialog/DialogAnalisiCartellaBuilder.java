package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.tools.analisispazio.AnalisiCartella;


/**
 * Dialog per la visualizzazione dei risultati dell'analisi sulle cartelle
 */
public class DialogAnalisiCartellaBuilder {
    private final AnalisiCartella analisiCartella;
    private final Context context;
    private String[] arrayBytes;
    private NumberFormat nf2;


    /**
     *
     * @param context Context
     * @param analisiCartella AnalisiSpazioThread totale della cartella genitore, ottenuta unendo i risultati delle sottocartelle.
     */
    public DialogAnalisiCartellaBuilder(@NonNull Context context, @NonNull AnalisiCartella analisiCartella){
        this.context = context;
        this.analisiCartella = analisiCartella;
    }


    /**
     * Crea la dialog
     * @return Dialog con i risultati dell'analisi
     */
    @SuppressLint("RtlHardcoded")
    public AlertDialog create(){
        arrayBytes = MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS);
        final NumberFormat nf1 = NumberFormat.getInstance(Locale.getDefault());
        nf1.setMaximumFractionDigits(1);
        nf2 = NumberFormat.getInstance(Locale.getDefault());
        nf2.setMaximumFractionDigits(2);

        final CustomDialogBuilder mediaInfoBuilder = new CustomDialogBuilder(context);
        mediaInfoBuilder.hideIcon(true);
        mediaInfoBuilder.setTitle(R.string.analisi_cartella);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_analisi_cartella, null);

        final TextView cartellaTextView = view.findViewById(R.id.textview_cartella);
        cartellaTextView.setText(analisiCartella.file.getAbsolutePath());
        final TextView datiCartella = view.findViewById(R.id.textview_dati_cartella);
        datiCartella.setText(String.format("%s %s (%s)", String.valueOf(analisiCartella.totFiles), context.getString(R.string.files),
                MyMath.humanReadableByte(analisiCartella.totBytes, arrayBytes)));

        final TextView numImmaginiTextView = view.findViewById(R.id.textview_num_immagini);
        setNumFiles(numImmaginiTextView, R.string.categorie_immagini, analisiCartella.totImmagini);
        final TextView dimensioniImmaginiTextView = view.findViewById(R.id.textview_dimensioni_immagini);
        setBytes(dimensioniImmaginiTextView, analisiCartella.totBytesImmagini);

        final TextView numVideoTextView = view.findViewById(R.id.textview_num_video);
        setNumFiles(numVideoTextView, R.string.categorie_video, analisiCartella.totVideo);
        final TextView dimensioniVideoTextView = view.findViewById(R.id.textview_dimensioni_video);
        setBytes(dimensioniVideoTextView, analisiCartella.totBytesVideo);

        final TextView numAudioTextView = view.findViewById(R.id.textview_num_audio);
        setNumFiles(numAudioTextView, R.string.categorie_audio, analisiCartella.totAudio);
        final TextView dimensioniAudioTextView = view.findViewById(R.id.textview_dimensioni_audio);
        setBytes(dimensioniAudioTextView, analisiCartella.totBytesAudio);

        final TextView numAltriTextView = view.findViewById(R.id.textview_num_altrifiles);
        setNumFiles(numAltriTextView, R.string.categorie_altro, analisiCartella.totAltriFiles());
        final TextView dimensioniAltriTextView = view.findViewById(R.id.textview_dimensioni_altrifiles);
        setBytes(dimensioniAltriTextView, analisiCartella.totBytesAltriFiles());

        final TextView viewImmagini = view.findViewById(R.id.view_immagini);
        float rapportoImmagini = (float)analisiCartella.totBytesImmagini/analisiCartella.totBytes;
        viewImmagini.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, rapportoImmagini));
        viewImmagini.setText(String.format("%s%s", nf1.format(rapportoImmagini*100), "%"));

        final TextView viewVideo = view.findViewById(R.id.view_video);
        float rapportoVideo = (float)analisiCartella.totBytesVideo/analisiCartella.totBytes;
        viewVideo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, rapportoVideo));
        viewVideo.setText(String.format("%s%s", nf1.format(rapportoVideo*100), "%"));

        final TextView viewAudio = view.findViewById(R.id.view_audio);
        float rapportoAudio = (float)analisiCartella.totBytesAudio/analisiCartella.totBytes;
        viewAudio.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, rapportoAudio));
        viewAudio.setText(String.format("%s%s", nf1.format(rapportoAudio*100), "%"));

        final TextView viewAltri = view.findViewById(R.id.view_altrifiles);
        float rapportoAltri = (float)analisiCartella.totBytesAltriFiles()/analisiCartella.totBytes;
        viewAltri.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, rapportoAltri));
        viewAltri.setText(String.format("%s%s", nf1.format(rapportoAltri*100), "%"));

        if(LayoutDirectionHelper.isRightToLeft(context)){
            cartellaTextView.setGravity(Gravity.RIGHT);
            dimensioniImmaginiTextView.setGravity(Gravity.RIGHT);
            dimensioniVideoTextView.setGravity(Gravity.RIGHT);
            dimensioniAudioTextView.setGravity(Gravity.RIGHT);
            dimensioniAltriTextView.setGravity(Gravity.RIGHT);
        }

        mediaInfoBuilder.setView(view);
        mediaInfoBuilder.setNeutralButton(android.R.string.ok, null);
        return mediaInfoBuilder.create();
    }


    /**
     * Setta i dati della textview principale
     * @param tv TextView su cui mostrare i dati
     * @param resIdTitolo Titolo da mostrare (categoria)
     * @param numFiles Numero di files per quella categoria
     */
    private void setNumFiles(TextView tv, @StringRes int resIdTitolo, int numFiles){
        tv.setText(String.format("%s: %s %s", context.getString(resIdTitolo), String.valueOf(numFiles), context.getString(R.string.files)));
    }


    /**
     * Setta i dati della textview secondaria
     * @param tv TextView su cui mostrare i dati
     * @param bytes Bytes totale per quella categoria
     */
    private void setBytes(TextView tv, long bytes){
        double percentuale = (double)bytes * 100 / analisiCartella.totBytes;
        tv.setText(String.format("%s  -  %s %s", MyMath.humanReadableByte(bytes, arrayBytes), nf2.format(percentuale), "%"));
    }
}
