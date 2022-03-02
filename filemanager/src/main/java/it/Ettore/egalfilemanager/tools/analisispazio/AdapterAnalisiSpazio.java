package it.Ettore.egalfilemanager.tools.analisispazio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.iconmanager.IconManager;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Adapter per la visualizzazione dello spazio occupato dalle cartelle
 */
public class AdapterAnalisiSpazio extends ArrayAdapter<AnalisiCartella> {
    private final static int mIdRisorsaVista = R.layout.riga_analisi_spazio;


    /**
     *
     * @param context Context
     * @param objects Lista con i risultati delle analisi sulle cartelle
     */
    public AdapterAnalisiSpazio(@NonNull Context context, @NonNull List<AnalisiCartella> objects) {
        super(context, mIdRisorsaVista, objects);
    }


    @SuppressLint("RtlHardcoded")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(mIdRisorsaVista, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.iconaImageView = convertView.findViewById(R.id.iconaImageView);
            viewHolder.nomeTextView = convertView.findViewById(R.id.nomeFileTextView);
            viewHolder.descrizioneTextView = convertView.findViewById(R.id.infoFileTextView);
            viewHolder.progressBar = convertView.findViewById(R.id.progressBar);
            viewHolder.percentualeTextView = convertView.findViewById(R.id.textview_percentuale);
            viewHolder.totBytesTextView = convertView.findViewById(R.id.textview_spazio_storage);
            convertView.setTag(viewHolder);

            if(LayoutDirectionHelper.isRightToLeft(getContext())){
                viewHolder.nomeTextView.setGravity(Gravity.RIGHT);
                viewHolder.descrizioneTextView.setGravity(Gravity.RIGHT);
                viewHolder.totBytesTextView.setGravity(Gravity.LEFT);
                viewHolder.percentualeTextView.setGravity(Gravity.RIGHT);
            }
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final AnalisiCartella ris = getItem(position);
        viewHolder.iconaImageView.setImageResource(ris.file.isDirectory() ? R.drawable.ico_cartella : IconManager.iconForFile(ris.file));
        viewHolder.nomeTextView.setText(ris.file.getName());
        final String descrizione = ris.file.isDirectory() ? String.format("%s %s, %s %s",
                String.valueOf(ris.totFiles), getContext().getString(R.string.files), String.valueOf(ris.totCartelle), getContext().getString(R.string.directories)) :
                FileUtils.getMimeType(ris.file);
        viewHolder.descrizioneTextView.setText(descrizione);
        final NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        viewHolder.percentualeTextView.setText(String.format("%s%s", nf.format(ris.percentualeOccupamento), "%"));
        final String[] arrayBytes = MyUtils.stringResToStringArray(getContext(), Costanti.ARRAY_BYTES_IDS);
        viewHolder.totBytesTextView.setText(MyMath.humanReadableByte(ris.totBytes, arrayBytes));
        viewHolder.progressBar.setMax(100);
        viewHolder.progressBar.setProgress((int)Math.round(ris.percentualeOccupamento));

        return convertView;
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeTextView, descrizioneTextView, percentualeTextView, totBytesTextView;
        ProgressBar progressBar;
    }
}
