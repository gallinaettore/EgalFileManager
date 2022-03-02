package it.Ettore.egalfilemanager.musicplayer


import java.util.*


/**
 * Classe per l'estrazione dei brani casuali in modo da evitare ripetizioni
 */
class RandomTrack {
    private val indexList = mutableListOf<Int>()
    private val random = Random()
    private var playlistTotalTracks = 0
    private var precedenteTracciaRiprodotta = -1


    /**
     * Notifica che è stata aggiunta una nuova traccia
     */
    fun addTrack(){
        indexList.add(playlistTotalTracks)
        playlistTotalTracks++
    }


    /**
     * Notifica che una traccia è stata rimossa
     * @param playlistTrackIndex Indice nella playlist della traccia che è stata rimossa
     */
    fun removeTrack(playlistTrackIndex: Int){
        indexList.remove(playlistTrackIndex) //rimuove l'indice della traccia se non è già stato riprodotto
        playlistTotalTracks--
        //dopo la rimozione, faccio scivolare tutti gli indici seguenti di un posto
        for(i in indexList.indices){
            val currentIndex = indexList[i]
            if(currentIndex > playlistTrackIndex){
                indexList[i] = currentIndex - 1
            }
        }
    }


    /**
     * Notifica la traccia corrente in fase di riproduzione
     * Da chiamare quando si attiva la funzione shuffle in modo da evitare che la traccia corrente venga estratta nuovamente
     * @param playlistTrackIndex Indice nella playlist della traccia che è in fase di riproduzione
     */
    fun setCurrentTrackPlaying(playlistTrackIndex: Int){
        indexList.remove(playlistTrackIndex)
    }


    /**
     * Restituisce l'indice della prossima traccia da riprodurre
     * @param repeatAll True se dopo aver estratto tutte le tracce, deve cominciare nuovamente con l'estrazione
     * @return Indice della traccia nella Playlist da riprodurre.
     *         -1 se repeatAll è impostato su false e tutte le tracce sono state riprodotte.
     */
    fun nextTrackIndex(repeatAll: Boolean): Int {

        /**
         * Estrae una traccia a caso
         * @return Indice della traccia nella Playlist da riprodurre.
         */
        fun sorteggia(): Int {
            val listIndex: Int = random.nextInt(indexList.size)
            return indexList[listIndex]
        }

        //tutte le tracce sono state estratte ricomincia d'accapo
        if(repeatAll && indexList.isEmpty()){
            for(i in 0 until playlistTotalTracks){
                indexList.add(i)
            }
        }

        //se non ci sono più tracce da riprodurre
        if(indexList.isEmpty()) return -1

        var playlistTrackIndex: Int = sorteggia()
        //estraggo un'altra traccia se questa è stata estratta la volta precedente
        if(playlistTrackIndex == precedenteTracciaRiprodotta && playlistTotalTracks > 1){
            playlistTrackIndex = sorteggia()
        }

        indexList.remove(playlistTrackIndex)
        precedenteTracciaRiprodotta = playlistTrackIndex
        return playlistTrackIndex
    }
}