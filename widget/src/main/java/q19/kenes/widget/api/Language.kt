package q19.kenes.widget.api

enum class Language {
    KAZAKH,
    RUSSIAN;

    internal companion object {
        fun map(language: Language?): kz.q19.domain.model.language.Language {
            return when (language) {
                KAZAKH -> kz.q19.domain.model.language.Language.KAZAKH
                RUSSIAN -> kz.q19.domain.model.language.Language.RUSSIAN
                else -> kz.q19.domain.model.language.Language.DEFAULT
            }
        }
    }
}