package it.Ettore.egalfilemanager.tools.duplicati;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


/**
 * Adapter per la visualizzazione di gruppi di files duplicati
 */
public class GruppiAdapter extends ArrayAdapter <List<String>> {
    private static final int RES_ID_LAYOUT = R.layout.riga_files_duplicati;
    private final boolean mostraAnteprime;
    //private final ExecutorService executorService;
    private final String[] arrayBytes;
    private final List<List<String>> gruppi;
    private boolean adapterHasChanged;


    /**
     *
     * @param context Context chiamante
     * @param gruppi Lista di gruppi di files. Ogni gruppo contiene una lista con i paths dei files che sono uguali
     */
    public GruppiAdapter(@NonNull Context context, @NonNull List<List<String>> gruppi) {
        super(context, RES_ID_LAYOUT, gruppi);
        this.gruppi = gruppi;
        //this.executorService = Executors.newFixedThreadPool(8);
        this.arrayBytes = MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS);
        this.mostraAnteprime = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Costanti.KEY_PREF_MOSTRA_ANTEPRIME, true);
    }



    /**
     * Rimuove i paths che appartengono a un determinato gruppo
     * @param position Indice del gruppo
     * @param filesDaRimuovere Files da rimuovere
     */
    public void removeFilesAt(int position, List<File> filesDaRimuovere){
        if(gruppi.size() > 0) {
            adapterHasChanged = true;
            final List<String> gruppo = getItem(position);
            //rimuovo i files cancellati dalla lista
            final List<String> pathsDaRimuovere = new ArrayList<>(filesDaRimuovere.size());
            for (File f : filesDaRimuovere) {
                pathsDaRimuovere.add(f.getAbsolutePath());
            }
            gruppo.removeAll(pathsDaRimuovere);
            //rimuovo l'intera lista se è vuota o contiene un solo file
            if (gruppo.size() < 2) {
                gruppi.remove(position);
            }
            notifyDataSetChanged();
        }
    }


    /**
     * Restituisce i gruppi gestiti dall'adapter
     * @return Lista di gruppi
     */
    public List<List<String>> getGruppi(){
        return this.gruppi;
    }


    /**
     * Restituisce un booleano che indica se l'adapter è stato modificato dopo la sua creazione
     * @return True se l'adapter è stato modificato dopo la sua creazione
     */
    public boolean adapterHasChanged(){
        return this.adapterHasChanged;
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(RES_ID_LAYOUT, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.iconaImageView = convertView.findViewById(R.id.iconaImageView);
            viewHolder.nomeFileTextView = convertView.findViewById(R.id.nomeFileTextView);
            viewHolder.dimensioneTextView = convertView.findViewById(R.id.dimensioneTextView);
            viewHolder.numFilesTextView = convertView.findViewById(R.id.numFilesTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final List<String> gruppo = getItem(position);
        if(gruppo != null) {
            final File primoFile = new File(gruppo.get(0));

            viewHolder.iconaImageView.setImageResource(IconManager.iconForFile(primoFile));
            if (mostraAnteprime) {
                //executorService.execute(new LoadThumbnailThread(getContext(), viewHolder.iconaImageView, primoFile, 36, 27));
                int imageSizePx = (int) getContext().getResources().getDimension(R.dimen.size_icona_lista_files); //ritorna pixel anche se espresso in dp
                IconManager.showImageWithGlide(primoFile, viewHolder.iconaImageView, imageSizePx, imageSizePx);
            }
            viewHolder.nomeFileTextView.setText(primoFile.getName());
            viewHolder.dimensioneTextView.setText(MyMath.humanReadableByte(primoFile.length(), arrayBytes));
            viewHolder.numFilesTextView.setText(String.format("%s %s", String.valueOf(gruppo.size()), getContext().getString(R.string.files)));
        }

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeFileTextView, dimensioneTextView, numFilesTextView;
    }
}
