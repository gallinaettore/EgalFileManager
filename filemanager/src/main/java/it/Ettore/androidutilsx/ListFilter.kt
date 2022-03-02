package it.Ettore.androidutilsx

import android.view.MenuItem
import androidx.appcompat.widget.SearchView

/*
Copyright (c)2018 - Egal Net di Ettore Gallina
*/

/**
 * Classe per l'utilizzo della search view per gestire il filtraggio di un adapter
 */
class ListFilter {
    private val searchView: SearchView
    private var menuItem: MenuItem? = null
    private var searchViewListener: SearchViewListener? = null

    /**
     *
     * @param searchView Search View
     */
    constructor(searchView: SearchView) {
        this.searchView = searchView
    }

    /**
     *
     * @param searchView Search View
     */
    constructor(searchView: SearchView, searchMenuItem: MenuItem?) {
        this.searchView = searchView
        menuItem = searchMenuItem
    }

    fun setSearchViewListener(searchViewListener: SearchViewListener?) {
        this.searchViewListener = searchViewListener
    }

    /**
     * Configura il funzionamento della search view
     */
    fun configuraSearchView(filterable: Filterable?) {

        //all'apertura della search view
        searchView.setOnSearchClickListener {
            filterable?.setFilterMode(true)
            searchViewListener?.onOpenSearchView()
        }

        //alla chiusura della search view
        searchView.setOnCloseListener {
            filterable?.setFilterMode(false)
            searchViewListener?.onCloseSearchView()
            false
        }

        //alla chiusura della search view
        menuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                filterable?.setFilterMode(false)
                searchViewListener?.onCloseSearchView()
                return true
            }
        })


        //modalità ricerca
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                //Filtro i risultati da mostrare nella reciclerview
                if (!searchView.isIconified && filterable != null) {
                    filterable.filter(newText)
                }
                return true
            }
        })
    }

    /**
     * Chiude la search view se aperta
     */
    fun chiudiSearchView() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            searchView.isIconified = true //a volte è necessario eseguire il comando 2 volte
        }
    }


    /**
     * Interfaccia che deve implementare l'adapter
     */
    interface Filterable {
        /**
         * Imposta la modalità filtro
         * @param filterMode Modalità filtro
         */
        fun setFilterMode(filterMode: Boolean)

        /**
         * Filtra l'adapter
         * @param query Stringa di filtraggio
         */
        fun filter(query: String?)
    }


    interface SearchViewListener {
        fun onOpenSearchView()
        fun onCloseSearchView()
    }
}