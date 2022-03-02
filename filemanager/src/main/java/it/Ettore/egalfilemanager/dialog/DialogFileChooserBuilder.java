package it.Ettore.egalfilemanager.dialog;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.filemanager.FileManager;
import it.Ettore.egalfilemanager.filemanager.OrdinatoreFiles;
import it.Ettore.egalfilemanager.filemanager.thread.CreaCartellaTask;
import it.Ettore.egalfilemanager.filemanager.thread.LsTask;


/**
 * Builder per la creazione di una dialog per la scelta di files
 */
public class DialogFileChooserBuilder extends CustomDialogBuilder implements AdapterView.OnItemClickListener, View.OnClickListener,
        DialogInterface.OnClickListener, LsTask.LsListener, CreaCartellaTask.CreaCartellaListener {
    public static final int TYPE_SELECT_FOLDER = 0;
    public static final int TYPE_SAVE_FILE = 1;
    public static final int TYPE_SELECT_FILE_FOLDER = 2;
    private final FileManager fileManager;
    private final int type;
    private DialogFileChooserListener listener;
    private File currentDir;
    private File selectedFile; //utilizzato solo in selectFileFolder;
    private String nomeFile;
    private final ListView listView;
    private final TextView pathTextView;
    private final TextView emptyView;
    private FileChooserAdapter adapter;
    private final EditText nomeFileEditText;
    private final ProgressBar progressBar;


    /**
     *
     * @param type Tipo di dialog. Utilizzare i tipi di questa classe
     */
    public DialogFileChooserBuilder(@NonNull Context context, int type){
        super(context);
        this.fileManager = new FileManager(context);
        this.fileManager.ottieniStatoRootExplorer();
        //this.fileManager.ottieniStatoMostraNascosti();
        this.type = type;
        currentDir = new File("/");

        hideIcon(true);
        removeTitleSpace(true);

        final View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_file_chooser, null);
        setView(view);
        pathTextView = view.findViewById(R.id.text_view_path);
        final LinearLayout upFolderLayout = view.findViewById(R.id.layout_cartella_superiore);
        upFolderLayout.setOnClickListener(this);
        final LinearLayout newFolderLayout = view.findViewById(R.id.layout_nuova_cartella);
        newFolderLayout.setOnClickListener(this);
        progressBar = view.findViewById(R.id.progressBar);
        listView = view.findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        emptyView = view.findViewById(R.id.empty_view);
        final LinearLayout nomeFileLayout = view.findViewById(R.id.layout_nome_file);
        nomeFileEditText = view.findViewById(R.id.edit_text_nome_file);

        switch (type){
            case TYPE_SELECT_FOLDER:
                nomeFileLayout.setVisibility(View.GONE);
                setPositiveButton(android.R.string.ok, this);
                break;
            case TYPE_SAVE_FILE:
                nomeFileLayout.setVisibility(View.VISIBLE);
                setPositiveButton(R.string.salva, this);
                break;
            case TYPE_SELECT_FILE_FOLDER:
                nomeFileLayout.setVisibility(View.GONE);
                setPositiveButton(android.R.string.ok, this);
                break;
            default:
                throw new IllegalArgumentException("Tipo dialog file chooser non gestito: " + type);
        }

        setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(listener != null){
                    listener.onFileChooserCanceled();
                }
            }
        });
    }



    /**
     * Setta la directory da visualizzare all'avvio della dialog
     * @param startFolder Directory da visualizzare
     */
    public void setStartFolder(File startFolder){
        if(startFolder != null){
            this.currentDir = startFolder;
        }
    }



    /**
     * Per il tipo "salva file" imposta il nome del file da suggerire
     * @param fileName Nome del file da visualizzare
     */
    public void setFileName(String fileName){
        this.nomeFile = fileName;
    }



    /**
     * Imposta il listener da chiamare quando un file o una cartella viene selezionata
     * @param listener Listener per selezione file o cartella
     */
    public void setChooserListener(DialogFileChooserListener listener){
        this.listener = listener;
    }



    /**
     * Crea la dialog
     * @return AlertDialog
     */
    @Override
    public AlertDialog create(){
        final AlertDialog dialog = super.create();
        nomeFileEditText.setText(nomeFile);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); //nascondo la tastiera
        ls(currentDir);
        return dialog;
    }


    /**
     * Avvia la scansione della directory
     * @param directory Directory da analizzare
     */
    private void ls(File directory){
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        fileManager.ls(directory, this);
    }


    /**
     * Eseguito al click dell'item della listview
     * @param adapterView
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        final File file = adapter.getItem(position);
        if(file.isDirectory()){
            ls(file);
        } else {
            if(type == TYPE_SAVE_FILE){
                //in modalità salvataggio, se seleziono un file viene usato come nome del file da salvare
                nomeFileEditText.setText(file.getName());
            } else if(type == TYPE_SELECT_FILE_FOLDER){
                //modalità seleziona sia file che directory
                selectedFile = file;
                pathTextView.setText(selectedFile.getAbsolutePath());
            }
        }
    }




    /**
     * Eseguito al click dei layout (usati come button bar)
     * @param view View che riceve il click
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.layout_cartella_superiore:
                final File parentFile = currentDir.getParentFile();
                if(parentFile != null){
                    ls(parentFile);
                }
                break;
            case R.id.layout_nuova_cartella:
                final DialogNewFolderBuilder dialogNewFolderBuilder = new DialogNewFolderBuilder(getContext(), new DialogNewFolderBuilder.DialogNewFolderListener() {
                    @Override
                    public void onNewFolderInput(String name) {
                        fileManager.creaCartella(currentDir, name, DialogFileChooserBuilder.this);
                    }
                });
                dialogNewFolderBuilder.create().show();
                break;
        }
    }



    /**
     * Eseguito al click dei button della dialog
     * @param dialogInterface
     * @param i
     */
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (type){
            case TYPE_SELECT_FOLDER:
                if(listener != null){
                    listener.onFileChooserSelected(currentDir);
                }
                break;
            case TYPE_SAVE_FILE:
                final String nomeFileDaSalvare = nomeFileEditText.getText().toString().replace("/", ""); //tolgo questo carattere altrimenti il nome viene splittato;
                if (listener != null) {
                    listener.onFileChooserSelected(new File(currentDir, nomeFileDaSalvare));
                }
                break;
            case TYPE_SELECT_FILE_FOLDER:
                if(listener != null){
                    listener.onFileChooserSelected(selectedFile);
                }
                break;
        }
    }









    /**
     * Adapter per la visualizzazione dei files
     */
    private class FileChooserAdapter extends ArrayAdapter<File> {
        private static final int RES_ID_VIEW = R.layout.riga_file_chooser;
        private final int defaultTextColor;
        private final int hiddenFileTextColor;

        FileChooserAdapter(Context ctx, List<File> listaFiles) {
            super(ctx, RES_ID_VIEW, listaFiles);
            this.defaultTextColor = new TextView(ctx).getTextColors().getDefaultColor();
            this.hiddenFileTextColor = ContextCompat.getColor(ctx, R.color.file_nascosto);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(RES_ID_VIEW, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.iconaImageView = convertView.findViewById(R.id.image_view_icona);
                viewHolder.nomeFileTextView = convertView.findViewById(R.id.image_view_file);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final File file = getItem(position);
            viewHolder.iconaImageView.setImageResource(file.isDirectory() ? R.drawable.file_chooser_cartella : R.drawable.file_chooser_file);
            viewHolder.nomeFileTextView.setText(file.getName());
            if(file.isHidden()){
                viewHolder.nomeFileTextView.setTextColor(hiddenFileTextColor);
            } else {
                viewHolder.nomeFileTextView.setTextColor(defaultTextColor);
            }
            return convertView;
        }
    }


    /**
     * View Holder dell'adapter per la visualizzazione dei files
     */
    private static class ViewHolder {
        ImageView iconaImageView;
        TextView nomeFileTextView;
    }












    /* FILE MANAGER LISTENER */

    /**
     * Dopo la scansione della cartella vengono mostrati i files contenuti
     * @param directory Cartella scansionata
     * @param listaFiles Lista di files o directory al suo interno
     */
    @Override
    public void onFileManagerLsFinished(File directory, List<File> listaFiles) {
        progressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        final OrdinatoreFiles ordinatoreFiles = new OrdinatoreFiles(getContext());
        ordinatoreFiles.ottieniStatoMostraNascosti();
        final List<File> listaFilesOrdinata = ordinatoreFiles.ordinaListaFiles(listaFiles);
        adapter = new FileChooserAdapter(getContext(), listaFilesOrdinata);
        listView.setAdapter(adapter);
        currentDir = directory;
        selectedFile = directory;
        pathTextView.setText(currentDir.getAbsolutePath());
        if(listaFilesOrdinata == null || listaFilesOrdinata.isEmpty()){
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Dopo la creazione della cartella, riscansiono la cartella parent per aggiornarla
     * @param created True se la cartella è stata creata o è già esistente.
     */
    @Override
    public void onFileManagerNewFolderFinished(boolean created) {
        if(created) {
            //fileManager.ls(currentDir, this);
            ls(currentDir);
        }
    }







    /**
     * Interfaccia che gestisce la selezione di files e cartelle
     */
    public interface DialogFileChooserListener {

        /**
         * Chiamato quando viene selezionato un elemento
         * @param file Elemento selezionato
         */
        void onFileChooserSelected(File file);

        /**
         * Chiamato quando non viene selezionato niente
         */
        void onFileChooserCanceled();
    }
}
