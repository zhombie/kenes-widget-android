package q19.kenes_widget.model

import java.util.*

class Language(val key: String, val value: String) {

    companion object {
        private const val KEY_KAZAKH = "kk"
        private const val KEY_RUSSIAN = "ru"
//        private const val KEY_ENGLISH = "en"

        val DEFAULT: Language
            get() = by(Locale.getDefault().language)

        val Kazakh: Language
            get() = Language(KEY_KAZAKH, "Қаз")

        val Russian: Language
            get() = Language(KEY_RUSSIAN, "Рус")

//        val English: Language
//            get() = Language(KEY_ENGLISH, "Eng")

        fun getSupportedLanguages(): Array<Language> {
            return arrayOf(Kazakh, Russian)
        }

        fun from(locale: Locale): Language {
            return by(locale.language)
        }

        fun by(language: String): Language {
            return when (language) {
                KEY_KAZAKH -> Kazakh
                KEY_RUSSIAN -> Russian
//                KEY_ENGLISH -> English
                else -> Russian
            }
        }
    }

    val locale: Locale
        get() = Locale(key)

}