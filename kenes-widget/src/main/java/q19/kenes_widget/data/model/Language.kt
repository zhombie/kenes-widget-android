package q19.kenes_widget.data.model

import java.util.*

class Language(
    val key: String,
    val value: String,
    val identificator: Int
) {

    companion object {
        private const val KEY_KAZAKH = "kk"
        private const val KEY_RUSSIAN = "ru"

        @Deprecated(message = "Application does not support yet.")
        private const val KEY_ENGLISH = "en"

        @JvmStatic
        val DEFAULT: Language
            get() = by(Locale.getDefault().language)

        @JvmStatic
        val KAZAKH: Language
            get() = Language(KEY_KAZAKH, "Қаз", 2)

        @JvmStatic
        val RUSSIAN: Language
            get() = Language(KEY_RUSSIAN, "Рус", 1)

        @Deprecated(message = "Application does not support yet.")
        @JvmStatic
        val ENGLISH: Language
            get() = Language(KEY_ENGLISH, "Eng", 3)

        fun getSupportedLanguages(): Array<Language> {
            return arrayOf(KAZAKH, RUSSIAN)
        }

        fun from(locale: Locale): Language {
            return by(locale.language)
        }

        fun by(language: String): Language {
            return when (language) {
                KEY_KAZAKH -> KAZAKH
                KEY_RUSSIAN -> RUSSIAN
//                KEY_ENGLISH -> ENGLISH
                else -> RUSSIAN
            }
        }
    }

    val locale: Locale
        get() = Locale(key)

    override fun toString(): String {
        return key
    }

}