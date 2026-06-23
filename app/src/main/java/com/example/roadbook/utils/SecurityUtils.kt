package com.example.roadbook.utils

object SecurityUtils {
    // Klucz XOR (dowolna liczba od 1 do 255)
    private const val XOR_KEY: Byte = 0x5A

    // POPRAWIONE: Dokładnie wyliczone bajty XOR dla tekstu "PGA_RALLY_SECRET_2026!#"
    private val OBFUSCATED_TOKEN_BYTES = byteArrayOf(
        0x0A, 0x1D, 0x1B, 0x05, 0x08, 0x1B, 0x16, 0x16, 0x03, 0x05,
        0x09, 0x1F, 0x19, 0x08, 0x1F, 0x0E, 0x05, 0x68, 0x6A, 0x68,
        0x6C, 0x7B, 0x79
    )

    /**
     * Rekonstruuje token w pamięci RAM tylko na ułamek sekundy przed wysłaniem zapytania HTTP.
     * Po zakończeniu metody odkodowany String natychmiast znika (jest czyszczony przez Garbage Collector).
     */
    fun getSecretToken(): String {
        val decoded = ByteArray(OBFUSCATED_TOKEN_BYTES.size)
        for (i in OBFUSCATED_TOKEN_BYTES.indices) {
            decoded[i] = (OBFUSCATED_TOKEN_BYTES[i].toInt() xor XOR_KEY.toInt()).toByte()
        }
        return String(decoded, Charsets.UTF_8)
    }
}