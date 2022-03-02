package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import it.Ettore.androidutilsx.ui.ColoredToast;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.Clipboard;


/**
 * Dialog per la visualizzazione dei files presenti nella clipboard
 */
public class DialogContenutoClipboardBuilder {
    private final Context context;
    private final Clipboard clipboard;
    private final DialogClipboardListener listener;


    /**
     *
     * @param context Context chiamante
     * @param clipboard Clipboard
     * @param listener Listener eseguito allo svuotamento della clipboard
     */
    public DialogContenutoClipboardBuilder(@NonNull Context context, @NonNull Clipboard clipboard, DialogClipboardListener listener){
        this.context = context;
        this.clipboard = clipboard;
        this.listener = listener;
    }


    /**
     * Crea la dialog
     * @return Dialog creata
     */
    public AlertDialog create() {
        final CustomDialogBuilder builder = new CustomDialogBuilder(context);
        builder.hideIcon(true);
        builder.setTitle(R.string.clipboard);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_clipboard, null);
        final ListView listView = view.findViewById(R.id.listview);
        listView.setAdapter(new ContenutoClipboardAdapter(context, clipboard.getListaPath()));
        builder.setView(view);
        builder.setPositiveButton(R.string.cancella_clipboard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ColoredToast.makeText(context, R.string.clipboard_vuota, Toast.LENGTH_LONG).show();
                clipboard.clear();
                if(listener != null){
                    listener.onClipboardEmpty();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }


    /**
     * Adapter per la visualizzazione della losta files
     */
    private static class ContenutoClipboardAdapter extends ArrayAdapter<String> {
        private static final int RESID_VIEW = R.layout.riga_clipboard;

        private ContenutoClipboardAdapter(@NonNull Context context, @NonNull List<String> objects) {
            super(context, RESID_VIEW, objects);
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(RESID_VIEW, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.pathTextView = convertView.findViewById(R.id.textview_path);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            viewHolder.pathTextView.setText(getItem(position));
            return convertView;
        }
    }


    /**
     * ViewHolder dell'adapter
     */
    private static class ViewHolder {
        TextView pathTextView;
    }




    /**
     * Listener della dialog
     */
    public interface DialogClipboardListener {

        /**
         * Chiamato quando si svuota la clipboard
         */
        void onClipboardEmpty();
    }
}
