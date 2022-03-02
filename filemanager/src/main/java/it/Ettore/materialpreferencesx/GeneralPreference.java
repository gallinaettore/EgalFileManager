package it.Ettore.materialpreferencesx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import it.Ettore.androidutilsx.utils.LayoutDirectionHelper;

/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/
public abstract class GeneralPreference extends LinearLayout {
    private TextView titleTextView, summaryTextView;
    private ImageView iconaImageView;
    private int defaultTextColor, focusTextColor, defaultIconColor, disabledIconColor;


    public GeneralPreference(Context context) {
        super(context);
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(focusTextColor != 0){
                    if(hasFocus){
                        titleTextView.setTextColor(focusTextColor);
                    } else {
                        titleTextView.setTextColor(defaultTextColor);
                    }
                }
            }
        });
    }


    @SuppressLint("RtlHardcoded")
    protected void setTitleTextView(TextView titleTextView) {
        this.titleTextView = titleTextView;
        testoSempreADestraPerLingueRtl(this.titleTextView);
        this.defaultTextColor = titleTextView.getCurrentTextColor();
    }


    protected void setSummaryTextView(TextView summaryTextView) {
        this.summaryTextView = summaryTextView;
        testoSempreADestraPerLingueRtl(this.summaryTextView);
    }


    public TextView getTitleTextView() {
        return titleTextView;
    }


    public TextView getSummaryTextView() {
        return summaryTextView;
    }

    protected void nascondiSummary(boolean nascondi){
        final LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)titleTextView.getLayoutParams();
        if(nascondi) {
            this.summaryTextView.setVisibility(View.GONE);
            layoutParams.setMargins(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15));
        } else {
            this.summaryTextView.setVisibility(View.VISIBLE);
            layoutParams.setMargins(dpToPx(15), dpToPx(9), dpToPx(15), dpToPx(2));
        }
        this.titleTextView.setLayoutParams(layoutParams);
    }


    protected int dpToPx(final float dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }


    public void setTitle(int resIdTitle){
        this.titleTextView.setText(resIdTitle);
    }


    public void setTitle(String title){
        this.titleTextView.setText(title);
    }


    public String getTitle(){
        return this.titleTextView.getText().toString();
    }


    public void setSummary(int resIdSummary){
        if(resIdSummary != 0){
            nascondiSummary(false);
            this.summaryTextView.setText(resIdSummary);
        } else {
            nascondiSummary(true);
            this.summaryTextView.setText(null);
        }
    }


    /**
     * Imposta il summary da visualizzare
     * @param summary Summary da visualizzare. Null per nascondere il summary
     */
    public void setSummary(String summary){
        nascondiSummary(summary == null);
        this.summaryTextView.setText(summary);
    }


    @Override
    public void setEnabled(boolean enabled){
        super.setEnabled(enabled);
        this.titleTextView.setEnabled(enabled);
        this.summaryTextView.setEnabled(enabled);
        if(enabled && defaultIconColor != 0){
            changeIconColor(defaultIconColor);
        } else if (!enabled && disabledIconColor != 0){
            changeIconColor(disabledIconColor);
        }
    }


    protected void setIconImageView(ImageView imageView){
        this.iconaImageView = imageView;
    }


    public void setIcon(@DrawableRes int resIdIcon){
        this.iconaImageView.setVisibility(View.VISIBLE);
        this.iconaImageView.setImageResource(resIdIcon);
    }


    public void setIcon(@DrawableRes int resIdIcon, @ColorRes int resIdColor){
        setIcon(resIdIcon);
        this.defaultIconColor = resIdColor;
        changeIconColor(resIdColor);
    }


    /**
     * Cambia il colore dell'icona
     * @param resIdColor Nuovo colore dell'icona
     */
    public void changeIconColor(@ColorRes int resIdColor){
        this.iconaImageView.setColorFilter(ContextCompat.getColor(iconaImageView.getContext().getApplicationContext(), resIdColor));
    }


    /**
     * Imposta il colore da applicare all'icona quando viene disabilitata la preference
     * @param resIdColor Risorsa del colore
     */
    public void setDisabledIconColor(@ColorRes int resIdColor){
        this.disabledIconColor = resIdColor;
    }


    private void testoSempreADestraPerLingueRtl(@NonNull TextView textView){
        //Sposto anche i testi concatenati a destra nelle lingue RTL
        if (LayoutDirectionHelper.isRightToLeft(getContext())) {
            textView.setGravity(Gravity.RIGHT);
        }
    }


    /**
     * Imposta il colore del testo del titolo se la preference ha il focus
     * @param color
     */
    public void setFocusTextColor(int color){
        this.focusTextColor = color;
    }

}
