package no.nav.syfo.infrastructure.clients.dokarkiv.dto

enum class Filtype(
    val value: String,
) {
    PDFA("PDFA"),
}

enum class Variantformat(
    val value: String,
) {
    ARKIV("ARKIV"),
}

const val DOKUMENTVARIANT_FILNAVN_MAX_LENGTH = 200

data class Dokumentvariant(
    val filnavn: String,
    val filtype: String,
    val fysiskDokument: ByteArray,
    val variantformat: String,
) {

    init {
        if ((filnavn.length + filtype.length) >= DOKUMENTVARIANT_FILNAVN_MAX_LENGTH) {
            throw IllegalArgumentException("Filnavn of Dokumentvariant is too long, max size is $DOKUMENTVARIANT_FILNAVN_MAX_LENGTH")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dokumentvariant

        if (filnavn != other.filnavn) return false
        if (filtype != other.filtype) return false
        if (!fysiskDokument.contentEquals(other.fysiskDokument)) return false
        if (variantformat != other.variantformat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filnavn.hashCode()
        result = 31 * result + filtype.hashCode()
        result = 31 * result + fysiskDokument.contentHashCode()
        result = 31 * result + variantformat.hashCode()
        return result
    }
}
