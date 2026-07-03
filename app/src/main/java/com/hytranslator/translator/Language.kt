package com.hytranslator.translator

/** Supported languages for HY-MT 1.5 1.8B model */
enum class Language(val code: String, val displayName: String, val hyCode: String) {
    CHINESE("zh", "中文", "Chinese"),
    ENGLISH("en", "English", "English"),
    JAPANESE("ja", "日本語", "Japanese"),
    KOREAN("ko", "한국어", "Korean"),
    FRENCH("fr", "Français", "French"),
    GERMAN("de", "Deutsch", "German"),
    SPANISH("es", "Español", "Spanish"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    RUSSIAN("ru", "Русский", "Russian"),
    ARABIC("ar", "العربية", "Arabic"),
    ITALIAN("it", "Italiano", "Italian"),
    DUTCH("nl", "Nederlands", "Dutch"),
    POLISH("pl", "Polski", "Polish"),
    THAI("th", "ไทย", "Thai"),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian"),
    MALAY("ms", "Bahasa Melayu", "Malay"),
    HINDI("hi", "हिन्दी", "Hindi"),
    TURKISH("tr", "Türkçe", "Turkish");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code } ?: ENGLISH
    }
}
