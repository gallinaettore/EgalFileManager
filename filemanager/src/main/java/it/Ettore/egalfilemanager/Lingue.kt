package it.Ettore.egalfilemanager

import it.Ettore.androidutilsx.lang.Lingua

/*
Copyright (c)2019 - Egal Net di Ettore Gallina
*/


class Lingue {

    companion object {
        @JvmStatic
        val values = listOf(
                Lingua("English", "en", "Trusted Translations"),
                Lingua("Italian" ,"it", "Ettore Gallina"),
                Lingua("French", "fr", "Translated srl"),
                Lingua("Spanish", "es", "Translated srl"),
                Lingua("Portuguese (Brazil)", "pt_BR", "Translated srl"),
                Lingua("Russian", "ru", "Translated srl", "Radmir Far"),
                Lingua("Vietnamese", "vi", "Nguyễn Duy Trung"),
                Lingua("Portuguese (Portugal)", "pt_PT", "Leandro Sousa"),
                Lingua("German", "de", "Andreas"),
                Lingua("Polish", "pl", "RaV", "Kacper Szuba", "Adamszopki"),
                Lingua("Japanese", "ja", "mark ashe"),
                Lingua("Turkish", "tr", "Seyyid Nebati"),
                Lingua("Persian", "fa", "Reza Taherizadeh"),
                Lingua("Indonesian", "in", "Sony Bejo"),
                Lingua("Dutch", "nl", "René Nolden"),
                Lingua("Slovak", "sk", "Dominick"),
                Lingua("Romanian", "ro", "Laurentiu")
        )
    }
}