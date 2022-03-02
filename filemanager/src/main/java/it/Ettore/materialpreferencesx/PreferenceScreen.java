package it.Ettore.materialpreferencesx;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import it.Ettore.androidutilsx.utils.MyMath;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public class PreferenceScreen extends ScrollView {
    private final LinearLayout rootLayout;


    public PreferenceScreen(Context context) {
        super(context);
        rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(0, 0, 0, (int) MyMath.dpToPx(context, 20f));
        addView(rootLayout);
    }


    public void addCategory(PreferenceCategory preferenceCategory){
        rootLayout.addView(preferenceCategory);
    }
}
