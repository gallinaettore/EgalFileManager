package it.Ettore.egalfilemanager.pathbar;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import org.jetbrains.annotations.NotNull;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.egalfilemanager.R;

/**
 * Classe base per le path bar
 */
public abstract class BasePathBar {
    private final HorizontalScrollView scrollView;
    private final ViewGroup layout;
    private final ImageView icona;


    /**
     *
     * @param scrollView ScrollView orizzontale dentro cui posizionare la pathbar
     */
    public BasePathBar(@NotNull HorizontalScrollView scrollView){
        this.scrollView = scrollView;
        this.layout = scrollView.findViewById(R.id.path_layout);
        this.icona = scrollView.findViewById(R.id.image_view_icona);
    }


    /**
     * Setta l'icona all'inizio della path bar
     * @param resIdIcona Risorsa dell'icona da mostrare
     */
    public void setIcon(@DrawableRes int resIdIcona){
        this.icona.setImageResource(resIdIcona);
    }


    /**
     * Effettua lo scroll del layout verso l'ultimo elementi se non è possibile mostrarli tutti
     */
    protected void scrollLayout(){
        final View previousFocusedView = focusedView(scrollView.getContext());
        scrollView.post(() -> {
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                if (LayoutDirectionHelper.isRightToLeft(layout.getContext())) {
                    scrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
                } else {
                    scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                }
            } else {
                scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
            //lo scroll imposta il focus sulla TextView della scrollview, reimposto il focus nella view che l'aveva in precedenza
            if(previousFocusedView != null){
                previousFocusedView.requestFocus();
            }
        });
    }


    /**
     * Restituisce il layout principale che si trova all'interno della scrollview
     * @return Root layout
     */
    protected ViewGroup getRootLayout(){
        return this.layout;
    }


    /**
     * Restituisce la view che ha il focus
     * @param context Activity
     * @return View che ha il focus, null se non è possibile stabilire quale view ha il focus
     */
    private View focusedView(Context context){
        if(context instanceof Activity){
            return ((Activity)context).getCurrentFocus();
        } else {
            return null;
        }
    }
}
