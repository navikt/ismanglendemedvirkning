package no.nav.syfo

import no.nav.syfo.domain.Personident

object UserConstants {
    val ARBEIDSTAKER_PERSONIDENT = Personident("12345678910")
    val ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS = Personident("11111111111")
    val ARBEIDSTAKER_PERSONIDENT_NAME_WITH_DASH = Personident("11111111234")
    val ARBEIDSTAKER_PERSONIDENT_NO_NAME = Personident("11111111222")
    val ARBEIDSTAKER_PERSONIDENT_PDL_FAILS = Personident("11111111666")
    const val VEILEDER_IDENT = "Z999999"

    const val PERSON_FORNAVN = "Fornavn"
    const val PERSON_MELLOMNAVN = "Mellomnavn"
    const val PERSON_ETTERNAVN = "Etternavnesen"
    const val PERSON_FULLNAME = "Fornavn Mellomnavn Etternavnesen"
    const val PERSON_FORNAVN_DASH = "For-Navn"
    const val PERSON_FULLNAME_DASH = "For-Navn Mellomnavn Etternavnesen"
}
