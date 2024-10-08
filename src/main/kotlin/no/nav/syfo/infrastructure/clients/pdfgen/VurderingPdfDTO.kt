package no.nav.syfo.infrastructure.clients.pdfgen

import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.sanitizeForPdfGen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class VurderingPdfDTO private constructor(
    val mottakerNavn: String,
    val mottakerFodselsnummer: String,
    val datoSendt: String,
    val documentComponents: List<DocumentComponent>,
) {
    constructor(
        mottakerNavn: String,
        mottakerPersonident: Personident,
        documentComponents: List<DocumentComponent>,
    ) : this(
        mottakerNavn = mottakerNavn,
        mottakerFodselsnummer = mottakerPersonident.value,
        datoSendt = LocalDate.now().format(formatter),
        documentComponents = documentComponents.sanitizeForPdfGen()
    )

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale("no", "NO"))
    }
}
