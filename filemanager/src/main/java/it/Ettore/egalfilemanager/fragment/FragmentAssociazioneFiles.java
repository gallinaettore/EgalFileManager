package it.Ettore.egalfilemanager.fragment;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityMain;
import it.Ettore.egalfilemanager.dialog.CustomDialogBuilder;
import it.Ettore.egalfilemanager.fileutils.AssociazioneFiles;


/**
 * Fragment per la visualizzazione dell'associazione estenzione/app da utilizzare per l'apertura inpostate in precedenza
 */
public class FragmentAssociazioneFiles extends GeneralFragment {
    private AssociazioneFiles associazioneFiles;
    private ListView listView;
    private TextView emptyView;


    public FragmentAssociazioneFiles(){}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final ActivityMain activityMain = (ActivityMain)getActivity();
        activityMain.settaTitolo(R.string.associazione_files);
        setHasOptionsMenu(true);	//importante per fare visualizzare il menu
        activityMain.getOverflowMenu();

        final View view = inflater.inflate(R.layout.fragment_associazioni_files, container, false);
        listView = view.findViewById(R.id.listview);
        listView.setSelector(android.R.color.transparent);
        emptyView = view.findViewById(R.id.empty_view);

        associazioneFiles = new AssociazioneFiles(getContext());
        mostraAssociazioni();

        return view;
    }


    private void mostraAssociazioni(){
        final Map<String,String> associazioni = associazioneFiles.getMapAssociazioni();
        final TreeSet<String> setEstenzioni = new TreeSet<>(associazioni.keySet());
        final List<String> listaEstenzioni = new ArrayList<>(setEstenzioni);
        Collections.sort(listaEstenzioni, String.CASE_INSENSITIVE_ORDER);
        final List<String> listaNomiPackage = new ArrayList<>(listaEstenzioni.size());
        for(String est : listaEstenzioni){
            listaNomiPackage.add(associazioni.get(est));
        }
        listView.setAdapter(new AdapterAssociazioni(getContext(), listaEstenzioni, listaNomiPackage));
        listView.setItemsCanFocus(true); //gli items della lista possono ricevere il focus

        if(listaEstenzioni.isEmpty()){
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_associazione_file, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ripristina_predefiniti:
                final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                builder.setType(CustomDialogBuilder.TYPE_WARNING);
                builder.setMessage(R.string.ripristinare_associazioni_predefinite);
                builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    associazioneFiles.scriviAssociazioniPredefinite();
                    mostraAssociazioni();
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




    /**
     * Adapter per la visualizzazione delle associazioni
     */
    private class AdapterAssociazioni extends ArrayAdapter<String> {
        private final static int mIdRisorsaVista = R.layout.riga_associazione_estenzione_app;
        private final LayoutInflater mInflater;
        private final List<String> nomiPackage;


        /**
         *
         * @param ctx Context
         * @param estenzioni Lista estenzioni
         * @param nomiPackage Lista packages associati alle estenzioni
         */
        private AdapterAssociazioni(Context ctx, List<String> estenzioni, List<String> nomiPackage) {
            super(ctx, mIdRisorsaVista, estenzioni);
            this.mInflater = LayoutInflater.from(ctx);
            this.nomiPackage = nomiPackage;
        }


        /**
         * Rimuove un'estenzione da lista
         * @param position Posizione da rimuovere
         */
        private void rimuoviEstenzione(int position){
            final String item = getItem(position);
            remove(item);
            nomiPackage.remove(position);
            notifyDataSetChanged();
            if(nomiPackage.isEmpty()){
                listView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }


        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null){
                convertView = mInflater.inflate(mIdRisorsaVista, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.convertView = convertView;
                viewHolder.iconaImageView = convertView.findViewById(R.id.imageview_icona_app);
                viewHolder.estenzioneTextView = convertView.findViewById(R.id.textview_estenzione);
                viewHolder.nomeAppTextView = convertView.findViewById(R.id.textview_nome_app);
                viewHolder.cancellaImageView = convertView.findViewById(R.id.imageview_cancella);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final String nomePackage = nomiPackage.get(position);
            try {
                final PackageManager pm = getContext().getPackageManager();
                final ApplicationInfo appInfo = pm.getApplicationInfo(nomePackage, 0);
                final String appName = pm.getApplicationLabel(appInfo).toString();
                viewHolder.nomeAppTextView.setText(appName);
                final Drawable iconDrawable = pm.getApplicationIcon(appInfo);
                if(iconDrawable instanceof BitmapDrawable) {
                    final Bitmap iconBitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                    final int iconSize = (int) MyMath.dpToPx(getContext(), 38);
                    final Drawable iconDrawableScaled = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(iconBitmap, iconSize, iconSize, true));
                    viewHolder.iconaImageView.setImageDrawable(iconDrawableScaled);
                } else {
                    viewHolder.iconaImageView.setImageResource(R.drawable.icon_app);
                }
            } catch (final PackageManager.NameNotFoundException e) {
                viewHolder.nomeAppTextView.setText(nomePackage);
                viewHolder.iconaImageView.setImageResource(R.drawable.icon_app);
            }

            viewHolder.estenzioneTextView.setText(getItem(position).toLowerCase());

            viewHolder.cancellaImageView.setOnClickListener(view -> {
                final CustomDialogBuilder builder = new CustomDialogBuilder(getContext());
                builder.setType(CustomDialogBuilder.TYPE_WARNING);
                builder.setMessage(R.string.messaggio_cancellazione_associazione);
                builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    associazioneFiles.eliminaAssociazione(getItem(position).toLowerCase());
                    rimuoviEstenzione(position);
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.create().show();
            });

            //quando un item riceve il focus lo do alla immagine che fa da button per poter essere utilizzata anche in modalità tv
            viewHolder.convertView.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus){
                    viewHolder.cancellaImageView.requestFocus();
                }
            });

            return convertView;
        }
    }


    /**
     * View Holder dell'adapter
     */
    private static class ViewHolder{
        View convertView;
        ImageView iconaImageView, cancellaImageView;
        TextView estenzioneTextView, nomeAppTextView;
    }
}
