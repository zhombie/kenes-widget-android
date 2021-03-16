package q19.kenes_widget.data.model

internal fun q19.kenes_widget.api.model.Language?.toDomain(): Language {
    if (this == null) Language.RUSSIAN
    return when (this) {
        q19.kenes_widget.api.model.Language.KAZAKH -> Language.KAZAKH
        q19.kenes_widget.api.model.Language.RUSSIAN -> Language.RUSSIAN
        else -> Language.RUSSIAN
    }
}