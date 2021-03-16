package q19.kenes_widget.api.model

enum class Language {
    KAZAKH,
    RUSSIAN;

    fun getAllSupportedLanguages(): List<Language> {
        return listOf(KAZAKH, RUSSIAN)
    }
}