package it.Ettore.materialpreferencesx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;


/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public class PreferenceCategory extends LinearLayout {
    private TextView categoryNameTextView;


    public PreferenceCategory(Context context, int resdTitle) {
        super(context);
        setup();
        categoryNameTextView.setText(resdTitle);
    }


    public PreferenceCategory(Context context, String title) {
        super(context);
        setup();
        categoryNameTextView.setText(title);
        if(title == null){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);
            categoryNameTextView.setLayoutParams(params);
        }
    }


    @SuppressLint("RtlHardcoded")
    private void setup(){
        inflate(getContext(), R.layout.category, this);
        setOrientation(LinearLayout.VERTICAL);
        this.categoryNameTextView = (TextView)findViewById(R.id.categoryNameTextView);
        //Sposto anche i testi concatenati a destra nelle lingue RTL
        if (LayoutDirectionHelper.isRightToLeft(getContext())) {
            this.categoryNameTextView.setGravity(Gravity.RIGHT);
        }
    }


    public void addPreference(GeneralPreference preference){
        addView(preference);
    }
}
