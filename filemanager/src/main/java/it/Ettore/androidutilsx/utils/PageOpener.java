package it.Ettore.androidutilsx.utils;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import it.Ettore.androidutilsx.ui.ColoredToast;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/

public class PageOpener {

    private final Context context;
    public static final String WEB_SITE = "https://www.gallinaettore.com";


    public PageOpener(Context context){
        this.context = context;
    }

    public void openPage(String page){
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(page));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e){
            ColoredToast.makeText(context, "Browser not found", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            ColoredToast.makeText(context, "Browser error", Toast.LENGTH_SHORT).show();
        }
    }

    public void openWebSite(){
        openPage(WEB_SITE);
    }

}
