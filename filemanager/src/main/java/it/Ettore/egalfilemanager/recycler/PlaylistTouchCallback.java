package it.Ettore.egalfilemanager.recycler;
/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Classe per la gestione dello spostamento degli elementi all'interno della recyclerview
 */
public class PlaylistTouchCallback extends ItemTouchHelper.Callback {
    private final OnItemMoveListener listener;


    /**
     *
     * @param listener Adapter che implementa OnItemMoveListener
     */
    public PlaylistTouchCallback(@NonNull OnItemMoveListener listener){
        this.listener = listener;
    }


    /**
     * Restituisce le azioni possibili nell'adapter
     * @param recyclerView RecyclerView
     * @param viewHolder ViewHolder
     * @return Azioni
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }


    /**
     * Notifica all'adapter che la recycler view ha effettuato un movimento
     * @param recyclerView RecyclerView
     * @param src ViewHolder iniziale
     * @param dst ViewHolder di destinazione
     * @return True
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder src, RecyclerView.ViewHolder dst) {
        listener.onItemMove(src.getAdapterPosition(), dst.getAdapterPosition());
        return true;
    }


    /**
     * Verifica se l'adapter è abilitato ad effettuare spostamenti con il long click
     * @return True se l'adapter è abilitato ad effettuare spostamenti con il long click
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }


    /**
     * Verifica se l'adapter è abilitato ad effettuare swipe verso destra o verso sinistra
     * @return True
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }


    /**
     * Notifica all'adapter che è stato effetuato lo swipe su un item della recycler View
     * @param viewHolder ViewHolder
     * @param direction Direzione dello swipe
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        listener.onItemSwiped(viewHolder.getAdapterPosition());
    }



    /**
     * Interfaccia che deve implementare l'adapter per la gestione dei movimenti
     */
    public interface OnItemMoveListener {

        /**
         * Notifica all'adapter che la recycler view ha effettuato un movimento
         * @param fromPosition posizione iniziale
         * @param toPosition posizione di destinazione
         */
        void onItemMove(int fromPosition, int toPosition);


        /**
         * Notifica all'adapter che è stato effettuato una swipe su un item
         * @param position Posizione dell'item su cui è stato effettuato lo swipe
         */
        void onItemSwiped(int position);
    }
}
