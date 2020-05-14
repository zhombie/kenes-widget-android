package q19.kenes_widget.model

import java.util.*

class Language(val key: String, val value: String) {

    companion object {
        private const val KAZAKH = "kk"
        private const val RUSSIAN = "ru"
        private const val ENGLISH = "en"

        val DEFAULT: Language
            get() = when (Locale.getDefault().language) {
                KAZAKH -> Kazakh
                RUSSIAN -> Russian
                ENGLISH -> English
                else -> Russian
            }

        val Kazakh: Language
            get() = Language(KAZAKH, "Қаз")

        val Russian: Language
            get() = Language(RUSSIAN, "Рус")

        val English: Language
            get() = Language(ENGLISH, "Eng")

        val AllLanguages: Array<Language>
            get() = arrayOf(Kazakh, Russian)
    }

    val locale: Locale
        get() = Locale(key)

}