package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.visualizzazione.VisualizzazioneBase;


/**
 * Builder per la dialog di scelta visualizzazione
 */
public class DialogVisualizzazioneBuilder implements View.OnClickListener {
    private final Context context;
    private int tipoVisualizzazione;
    private final DialogInterface.OnClickListener listener;
    private AlertDialog dialog;
    private boolean nascondiVisualizzazioneAnteprima;


    /**
     *
     * @param context Context chiamante
     * @param tipoVisualizzazione Una delle costanti VISUALIZZAZIONE della classe VisualizzazioneBase
     * @param listener Listener eseguito quando si cambia la visualizzazione
     */
    public DialogVisualizzazioneBuilder(@NonNull Context context, int tipoVisualizzazione, @NonNull DialogInterface.OnClickListener listener){
        this.context = context;
        this.tipoVisualizzazione = tipoVisualizzazione;
        this.listener = listener;
    }


    /**
     * Nasconde la possibilità di scelta visualizzazione anteprima
     */
    public void nascondiVisualizzazioneAnteprima(){
        this.nascondiVisualizzazioneAnteprima = true;
    }


    /**
     * Crea la dialog
     * @return Dialog creata
     */
    public AlertDialog create(){
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.setTitle(R.string.visualizzazione);
        builder.hideIcon(true);
        builder.removeTitleSpace(true);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_visualizzazione, null);
        builder.setView(view);

        final LinearLayout layoutLista = view.findViewById(R.id.layout_lista);
        layoutLista.setOnClickListener(this);
        final LinearLayout layoutListaPiccola = view.findViewById(R.id.layout_lista_piccola);
        layoutListaPiccola.setOnClickListener(this);
        final LinearLayout layoutGriglia = view.findViewById(R.id.layout_griglia);
        layoutGriglia.setOnClickListener(this);
        final LinearLayout layoutAnteprima = view.findViewById(R.id.layout_anteprima);
        layoutAnteprima.setOnClickListener(this);

        if(nascondiVisualizzazioneAnteprima){
            layoutAnteprima.setVisibility(View.GONE);
            if(tipoVisualizzazione == VisualizzazioneBase.VISUALIZZAZIONE_ANTEPRIMA){
                tipoVisualizzazione = VisualizzazioneBase.VISUALIZZAZIONE_LISTA;
            }
        }

        final View iconaLista = view.findViewById(R.id.view_lista);
        final View iconaListaPiccola = view.findViewById(R.id.view_lista_piccola);
        final View iconaGriglia = view.findViewById(R.id.view_griglia);
        final View iconaAnteprima = view.findViewById(R.id.view_anteprima);

        final TextView textViewLista = view.findViewById(R.id.text_view_lista);
        final TextView textViewListaPiccola = view.findViewById(R.id.text_view_lista_piccola);
        final TextView textViewGriglia = view.findViewById(R.id.text_view_griglia);
        final TextView textViewAnteprima = view.findViewById(R.id.text_view_anteprima);

        switch (tipoVisualizzazione){
            case VisualizzazioneBase.VISUALIZZAZIONE_LISTA:
                seleziona(iconaLista, textViewLista);
                break;
            case VisualizzazioneBase.VISUALIZZAZIONE_LISTA_PICCOLA:
                seleziona(iconaListaPiccola, textViewListaPiccola);
                break;
            case VisualizzazioneBase.VISUALIZZAZIONE_GRIGLIA:
                seleziona(iconaGriglia, textViewGriglia);
                break;
            case VisualizzazioneBase.VISUALIZZAZIONE_ANTEPRIMA:
                seleziona(iconaAnteprima, textViewAnteprima);
                break;
        }

        builder.setNegativeButton(android.R.string.cancel, null);
        dialog = builder.create();
        return dialog;
    }


    /**
     * Imposta le view come selezionate
     * @param viewIcona Icona
     * @param textView Descrizione
     */
    private void seleziona(View viewIcona, TextView textView){
        viewIcona.setBackgroundResource(R.drawable.button_visualizzazione_selected);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = viewIcona.getContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        @ColorRes int colorAccent = typedValue.resourceId;
        textView.setTextColor(ContextCompat.getColor(context, colorAccent));
    }


    /**
     * Gestisce i tocchi di selezione
     * @param view View toccata
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.layout_lista:
                listener.onClick(dialog, VisualizzazioneBase.VISUALIZZAZIONE_LISTA);
                break;
            case R.id.layout_lista_piccola:
                listener.onClick(dialog, VisualizzazioneBase.VISUALIZZAZIONE_LISTA_PICCOLA);
                break;
            case R.id.layout_griglia:
                listener.onClick(dialog, VisualizzazioneBase.VISUALIZZAZIONE_GRIGLIA);
                break;
            case R.id.layout_anteprima:
                listener.onClick(dialog, VisualizzazioneBase.VISUALIZZAZIONE_ANTEPRIMA);
                break;
        }
        dialog.dismiss();
    }
}
