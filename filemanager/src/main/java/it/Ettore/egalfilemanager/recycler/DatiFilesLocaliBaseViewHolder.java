package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.androidutilsx.utils.CompatUtils;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.ThemeUtils;


/**
 *  ViewHolder base per la visualizzazione dei files
 */
abstract class DatiFilesLocaliBaseViewHolder extends RecyclerView.ViewHolder {
    private final CheckBox checkBox;


    /**
     *
     * @param itemView View generale
     * @param resIdCheckBox id della risorsa che identifica il checkbox
     */
    DatiFilesLocaliBaseViewHolder(View itemView, @IdRes int resIdCheckBox) {
        super(itemView);
        this.checkBox = itemView.findViewById(resIdCheckBox);
    }


    /**
     * Metodo da overridare per effettuare il bind sulla ViewHolder
     *
     * @param position posizione nell'adapter
     */
    abstract void bind(int position);


    /**
     *  Configura i tocchi del view holder
     *
     * @param file File da visualizzare
     * @param adapter Adapter con la lista dei files
     */
    void manageTouch(final File file, final DatiFilesLocaliBaseAdapter adapter){
        //imposto il tocco lungo
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                adapter.toggleSelezioneMultipla();
                if (adapter.modalitaSelezioneMultipla()) {
                    if (!adapter.getElementiSelezionati().contains(file)) {
                        adapter.getElementiSelezionati().add(file);
                        checkBox.setChecked(true);
                    }
                } else {
                    adapter.getElementiSelezionati().clear();
                }
                adapter.notificaModalitaSelezioneMultipla();

                if (adapter.getListener() != null) {
                    adapter.getListener().onItemLongClick(file);
                }
                return true; //restituisco true per indicare che non si desiderano ulteriori elaborazioni (non viene chiamato onClickListener)
            }
        });

        //imposto il tocco breve
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(adapter.modalitaSelezioneMultipla()){
                    if(adapter.getElementiSelezionati().contains(file)){
                        adapter.getElementiSelezionati().remove(file);
                    } else {
                        adapter.getElementiSelezionati().add(file);
                    }
                    checkBox.setChecked(adapter.getElementiSelezionati().contains(file));
                } else {
                    checkBox.setChecked(false);
                }
                if (adapter.getListener() != null) {
                    adapter.getListener().onItemClick(file);
                }
            }
        });
    }


    /**
     * Configura le views in relazione alla visibilità del file
     * @param adapter Adapter
     * @param file File
     * @param iconImageView Image view dell'icona
     * @param primaryTextView TextView su cui applicare il colore testo primario
     * @param secondaryTextViews TextViews su cui applicare il colore del testo secondario
     */
    void manageFileVisibility(@NonNull DatiFilesLocaliBaseAdapter adapter, @NonNull File file, @NonNull ImageView iconImageView, @NonNull TextView primaryTextView, @NonNull TextView... secondaryTextViews){
        if(file.isHidden()){
            CompatUtils.setAlpha(iconImageView, Costanti.ALPHA_IMMAGINI_FILES_NASCOSTI);
            primaryTextView.setTextColor(adapter.getThemeUtils().getHiddenFileTextColor());
            for (TextView textView : secondaryTextViews){
                textView.setTextColor(adapter.getThemeUtils().getHiddenFileTextColor());
            }
        } else {
            CompatUtils.setAlpha(iconImageView, 255);
            primaryTextView.setTextColor(adapter.getThemeUtils().getPrimaryTextColor());
            for (TextView textView : secondaryTextViews){
                textView.setTextColor(adapter.getThemeUtils().getSecondaryTextColor());
            }
        }
    }


    /**
     * Configura la view collegamento
     * @param file File
     * @param collegamentoImageView Image view con l'icona di collegamento
     */
    void manageFileLink(@NonNull File file, @NonNull ImageView collegamentoImageView){
        if (FileUtils.isSymlink(file)) {
            collegamentoImageView.setVisibility(View.VISIBLE);
        } else {
            collegamentoImageView.setVisibility(View.GONE);
        }
    }



    /**
     *
     * @param selezioneMultipla Visualizza o no la checkbox
     */
    void abilitaSelezioneMultipla(boolean selezioneMultipla){
        if(selezioneMultipla) {
            checkBox.setVisibility(View.VISIBLE);
        } else {
            checkBox.setVisibility(View.GONE);
        }
    }


    /**
     *
     * @param selezionaTutto Chiamato su ogni view, per selezionare/delezionare tutto
     */
    void selezionaTutto(boolean selezionaTutto){
        checkBox.setChecked(selezionaTutto);
    }


    /**
     * Dopo il bind viene controllato il corretto stato della checkbox (visible/gone o checheck/unchecked)
     *
     * @param adapter Adapter con la lista dei files
     * @param position Posizione del ViewHolder all'interno dell'adapter
     */
    void modificaStatoCheckbox(DatiFilesLocaliBaseAdapter adapter, int position){
        final File file = adapter.getListaFiles().get(position);
        checkBox.setChecked(adapter.getElementiSelezionati().contains(file));
        if(adapter.modalitaSelezioneMultipla()){
            checkBox.setVisibility(View.VISIBLE);
        } else {
            checkBox.setVisibility(View.GONE);
        }
    }

}
