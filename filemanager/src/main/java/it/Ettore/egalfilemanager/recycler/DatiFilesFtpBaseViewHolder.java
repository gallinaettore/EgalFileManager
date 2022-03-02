package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import it.Ettore.egalfilemanager.ftp.FtpElement;


/**
 *  ViewHolder base per la visualizzazione dei files
 */
abstract class DatiFilesFtpBaseViewHolder extends RecyclerView.ViewHolder {
    private final CheckBox checkBox;


    /**
     *
     * @param itemView View generale
     * @param resIdCheckBox id della risorsa che identifica il checkbox
     */
    DatiFilesFtpBaseViewHolder(View itemView, @IdRes int resIdCheckBox) {
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
     *  Metodo che configura il ViewHolder (in particolare i tocchi)
     *
     * @param file File da visualizzare
     * @param adapter Adapter con la lista dei files
     */
    void manageView(final FtpElement file, final DatiFilesFtpBaseAdapter adapter){
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
    void modificaStatoCheckbox(DatiFilesFtpBaseAdapter adapter, int position){
        final FtpElement file = adapter.getListaFiles().get(position);
        checkBox.setChecked(adapter.getElementiSelezionati().contains(file));
        if(adapter.modalitaSelezioneMultipla()){
            checkBox.setVisibility(View.VISIBLE);
        } else {
            checkBox.setVisibility(View.GONE);
        }
    }
}
