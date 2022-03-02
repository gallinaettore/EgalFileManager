package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.home.HomeItem;


/**
 * Builder per la Dialog di selezione dello storage
 */
public class SelectStorageDialogBuilder extends CustomDialogBuilder {
    private List<HomeItem> storageItems;
    private final ListView listView;
    private SelectStorageListener listener;


    public SelectStorageDialogBuilder(@NonNull Context context) {
        super(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_chooser_storage, null);
        listView = view.findViewById(R.id.listview);
        setView(view);
    }


    /**
     * Setta la lista di Items che contiene gli storages disponibili
     * @param storageItems
     * @return Builder
     */
    public SelectStorageDialogBuilder setStorageItems(List<HomeItem> storageItems){
        this.storageItems = storageItems;
        return this;
    }


    /**
     * Setta il listener da chiamare al termine della selezione
     * @param listener Listener da chiamare quando si seleziona lo storage
     * @return Builder
     */
    public SelectStorageDialogBuilder setSelectStorageListener(SelectStorageListener listener){
        this.listener = listener;
        return this;
    }


    /**
     * Crea la dialog
     * @return AlertDialog
     */
    @Override
    public AlertDialog create(){
        setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            if(listener != null){
                listener.onCancelStorageSelection();
            }
        });
        final AlertDialog dialog = super.create();
        final AdapterStorage adapter = new AdapterStorage(getContext());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, position, id) -> {
            if(listener != null){
                listener.onSelectStorage(adapter.getItem(position).startDirectory);
            }
            //chiudo la dialog
            if(dialog != null && dialog.isShowing()){
                dialog.dismiss();
            }
        });
        return dialog;
    }


    /**
     * Da utilizzare in alternativa al metodo show().
     * Mostra la dialog di selezione solo se ci sono più storages presenti. SE invece ce n'è soltanto uno viene selezionato direttamente.
     */
    public void showSelectDialogIfNecessary(){
        if(storageItems.size() == 1){
            //se c'è un unico elemento lo invio direttamente al listener senza mostra la dialog
            if(listener != null){
                listener.onSelectStorage(storageItems.get(0).startDirectory);
            }
        } else {
            //se ci sono tanti elementi monstro la dialog di selezione
            create().show();
        }
    }





    /**
     * Listener di selezione storages
     */
    public interface SelectStorageListener {

        /**
         * Chiamato quando lo storage viene selezionato
         * @param storagePath Percorso dello storage
         */
        void onSelectStorage(File storagePath);


        /**
         * Chiamanto quando non viene selezionato niente
         */
        void onCancelStorageSelection();
    }





    /**
     * Adapter per la visualizzazione dei tipi mime da utilizzare per l'apertura del file
     */
    private class AdapterStorage extends ArrayAdapter<HomeItem> {
        private final static int mIdRisorsaVista = R.layout.riga_chooser_storage;

        private AdapterStorage(Context ctx) {
            super(ctx, mIdRisorsaVista, storageItems);
        }

        @SuppressLint("RtlHardcoded")
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(mIdRisorsaVista, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.iconaImageView = convertView.findViewById(R.id.image_view_icona);
                viewHolder.textViewNome = convertView.findViewById(R.id.text_view_nome);
                viewHolder.textViewPath = convertView.findViewById(R.id.text_view_path);
                convertView.setTag(viewHolder);

                if(LayoutDirectionHelper.isRightToLeft(getContext())){
                    viewHolder.textViewNome.setGravity(Gravity.RIGHT);
                    viewHolder.textViewPath.setGravity(Gravity.RIGHT);
                }
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final HomeItem item = getItem(position);
            viewHolder.iconaImageView.setImageResource(item.resIdIcona);
            viewHolder.textViewNome.setText(item.titolo);
            viewHolder.textViewPath.setText(item.startDirectory.getAbsolutePath());
            return convertView;
        }
    }



    /**
     * View Holder dell'adapter storage
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView textViewNome, textViewPath;
    }
}
