package it.Ettore.egalfilemanager.iconmanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import it.Ettore.androidutilsx.utils.MyMath;


/**
 * Thread per il caricamento dell'icona dell'app
 */
public class LoadAppChooserIconThread implements Runnable {
    private final WeakReference<Context> context;
    private final WeakReference<ImageView> imageView;
    private final ResolveInfo resolveInfo;
    private final PackageManager pm;
    private BitmapDrawable bitmap;


    /**
     *
     * @param context Context
     * @param imageView ImageView in cui visualizzare l'icona
     * @param resolveInfo ResolveInfo dell'app da cui estrarre l'icona
     * @param pm PackageManager da utilizzare per l'estrazione
     */
    public LoadAppChooserIconThread(Context context, ImageView imageView, ResolveInfo resolveInfo, PackageManager pm){
        this.context = new WeakReference<>(context);
        this.imageView = new WeakReference<>(imageView);
        this.resolveInfo = resolveInfo;
        this.pm = pm;
    }


    @Override
    public void run() {
        //se l'image view è stata deallocata (il fragment è stato chiuso) non esegui il thread
        if(imageView.get() == null){
            return;
        }

        //eseguo in background
        try {
            //ottengo l'icona dell'app e la ridimensiono
            final Drawable iconDrawable = resolveInfo.loadIcon(pm);
            final Bitmap iconBitmap = IconManager.getBitmapFromDrawable(iconDrawable);
            final int iconSize = (int) MyMath.dpToPx(context.get(), 38);
            bitmap = new BitmapDrawable(context.get().getResources(), Bitmap.createScaledBitmap(iconBitmap, iconSize, iconSize, true));
        } catch (Exception ignored){
            Log.e("Bitmap drawable", "drawable");
            ignored.printStackTrace();
        }

        //eseguo sul thread della ui
        try {
            ((Activity) context.get()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(imageView != null){
                            if(bitmap != null){
                                imageView.get().setImageDrawable(bitmap);
                            } else {
                                imageView.get().setBackgroundColor(Color.TRANSPARENT);
                            }
                        }
                    } catch (Exception ignored){} //imageview può essere null perchè deallocato all'ultimo momento
                }
            });
        } catch (Exception ignored){}
    }
}
