package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;


/**
 * Classe di decorazione per inserire il separatore nelle views
 */
public final class LineItemDecoration extends RecyclerView.ItemDecoration {
    private static final float LINE_SIZE = 1.0f;
    private final Paint paint;

    public LineItemDecoration(){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(LINE_SIZE);
        paint.setColor(Color.GRAY);
    }


    /**
     * Modifica lo spazio occupato dalla View
     * @param outRect
     * @param view
     * @param parent
     * @param state
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state){
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(0, 0, 0, (int) Math.floor(LINE_SIZE));
    }


    /**
     * Disegna la "decorazione" nello spazio messo a disposizione dal metodo getItemOffsets()
     * @param canvas
     * @param parent
     * @param state
     */
    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state){
        super.onDrawOver(canvas, parent, state);
        final RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        for (int i = 0; i < parent.getChildCount(); i++){
            final View child = parent.getChildAt(i);
            canvas.drawLine(layoutManager.getDecoratedLeft(child), layoutManager.getDecoratedBottom(child), layoutManager.getDecoratedRight(child), layoutManager.getDecoratedBottom(child), paint);
        }
    }
}
