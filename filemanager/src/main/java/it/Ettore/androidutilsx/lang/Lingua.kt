package it.Ettore.androidutilsx.lang

import java.text.Normalizer

/*
Copyright (c)2019 - Egal Net di Ettore Gallina
*/


class Lingua(val nome: String, val codiceLocale: String, vararg traduttori: String) : Comparable<Lingua> {

    val traduttoriACapo: String = traduttori.joinToString("\n")

    override fun compareTo(other: Lingua): Int {
        return nome.compareTo(other.nome)
    }
}