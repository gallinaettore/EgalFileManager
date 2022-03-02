package it.Ettore.egalfilemanager.imageviewer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.activity.ActivityImageViewer;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


/**
 * Adapter per la visualizzazione delle immagini nel pager
 */
public class ImagePagerAdapter extends PagerAdapter {
    private final List<File> immagini;
    private final List<Integer> rotazioni;
    private final ActivityImageViewer activityImageViewer;
    private boolean inAttesaDiNascondimento;


    /**
     *
     * @param activityImageViewer Activity del visualizzatore di immagini
     * @param immagini Lista delle immagini da visualizzare
     */
    public ImagePagerAdapter(ActivityImageViewer activityImageViewer, @NonNull List<File> immagini) {
        this.activityImageViewer = activityImageViewer;
        this.immagini = immagini;
        this.rotazioni = new ArrayList<>(immagini.size());
        for(int i=0; i < immagini.size(); i++){
            rotazioni.add(0);
        }
    }


    /**
     * Rimuovo un file dall'adapter
     * @param file File da rimuovere
     */
    public void remove(File file){
        int index = immagini.indexOf(file);
        if(index != -1) {
            immagini.remove(index);
            rotazioni.remove(index);
            notifyDataSetChanged();
        }
    }


    /**
     * This way, when you call notifyDataSetChanged(), the view pager will remove all views and reload them all.
     * @param object Object
     * @return POSITION_NONE
     */
    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }


    @Override
    public int getCount() {
        return immagini.size();
    }


    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        final View itemView = LayoutInflater.from(activityImageViewer).inflate(R.layout.fragment_slide_page_image_viewer, container, false);

        final TextView textViewNome = itemView.findViewById(R.id.textview_nome);

        final LinearLayout layoutErrore = itemView.findViewById(R.id.layout_errore);
        layoutErrore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostraBarre(textViewNome);
            }
        });

        final PhotoView photoView = itemView.findViewById(R.id.photo_view);
        photoView.setVisibility(View.GONE);
        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostraBarre(textViewNome);
            }
        });

        final ProgressBar progressBar = itemView.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostraBarre(textViewNome);
            }
        });

        final WebView webView = itemView.findViewById(R.id.webview);
        webView.setVisibility(View.GONE);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mostraBarre(textViewNome);
                return false;
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostraBarre(textViewNome);
            }
        });

        if(position >= 0 && position < immagini.size()) {
            //se la posizione è corretta eseguo il task per la visualizzazione dell'immagine
            final File file = immagini.get(position);
            textViewNome.setText(file.getName());
            final LoadImageFileTask loadImageFileTask = new LoadImageFileTask(file, new LoadImageFileTask.LoadImageFileListener() {
                @Override
                public void onLoadImage(Bitmap bitmap, boolean isGif) {
                    int rotazione = rotazioni.get(position);
                    progressBar.setVisibility(View.GONE);
                    if(isGif){
                        //visualizzo l'immagine gif nella webview
                        photoView.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        webView.loadUrl(file.toURI().toString());
                        if(rotazione != 0){
                            webView.setRotation(rotazione);
                        }
                    } else {
                        if(bitmap != null){
                            //visualizzo l'immagine nella photoview
                            webView.setVisibility(View.GONE);
                            photoView.setVisibility(View.VISIBLE);
                            //applico la rotazione
                            if(rotazione == 0){
                                photoView.setImageBitmap(bitmap);
                            } else {
                                final Matrix matrix = new Matrix();
                                matrix.postRotate(rotazione);
                                final Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                photoView.setImageBitmap(rotated);
                            }
                        } else {
                            //errore nella decodifica dell'immagine
                            webView.setVisibility(View.GONE);
                            photoView.setVisibility(View.GONE);
                            layoutErrore.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
            loadImageFileTask.execute();
        } else {
            //visualizzo il messaggio di errore
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            photoView.setVisibility(View.GONE);
            layoutErrore.setVisibility(View.VISIBLE);
        }

        container.addView(itemView);

        //nasconde le barre
        activityImageViewer.nascondiToolbarConRitardo();
        nascondiNomeFileConRitardo(textViewNome);

        return itemView;
    }


    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View)object);
    }


    /**
     * Ruota l'immagine
     * @param position Posizione nell'adapter dell'immagine da ruotare
     * @param rotazione Rotazione da aggiungere alla rotazione corrente
     */
    public void rotate(int position, int rotazione){
        int rotazioneCorrente = rotazioni.get(position);
        rotazioni.set(position, rotazioneCorrente + rotazione);
        notifyDataSetChanged();
    }


    /**
     * Mostra la toolbar e la barra del nome file temporaneamente
     * @param textViewNome Barra del nome del file
     */
    private void mostraBarre(final TextView textViewNome){
        activityImageViewer.mostraToolbar();
        mostraNomeFile(textViewNome);
    }


    /**
     * Mostra la barra del nome file temporaneamente
     * @param textViewNome Barra del nome del file
     */
    private void mostraNomeFile(final TextView textViewNome){
        if(textViewNome.getVisibility() != View.VISIBLE) {
            textViewNome.setVisibility(View.VISIBLE);
            nascondiNomeFileConRitardo(textViewNome);
        }
    }


    /**
     * Nasconde la barra del nome file dopo un tot di tempo
     * @param textViewNome Barra del nome del file
     */
    private void nascondiNomeFileConRitardo(final TextView textViewNome){
        if(textViewNome.getVisibility() == View.VISIBLE && !inAttesaDiNascondimento){
            inAttesaDiNascondimento = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(inAttesaDiNascondimento) {
                        textViewNome.setVisibility(View.GONE);
                        inAttesaDiNascondimento = false;
                    }
                }
            }, ActivityImageViewer.RITARDO_NASCONDIMENTO);
        }
    }


    /**
     * Restituisce la lista immagini settata
     * @return Lista immagini
     */
    public List<File> getImmagini(){
        return this.immagini;
    }


    /**
     * Restituisce l'immagine a un determinata posizione
     * @param index Posizione desiderata
     * @return File immagine. Nill se la lista immagini non è stata impostata o se l'indice rischiesto non è incluso nella lista
     */
    public File immagineAt(int index){
        if(immagini == null){
            return null;
        }
        if(index >= 0 || index < immagini.size()){
            return immagini.get(index);
        } else {
            return null;
        }
    }
}
