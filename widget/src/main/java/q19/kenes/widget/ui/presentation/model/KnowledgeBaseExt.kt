package q19.kenes.widget.ui.presentation.model

import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.domain.model.language.Language
import kz.q19.socket.model.Category

fun Category.isResponseGroup(): Boolean {
    return responses.isEmpty()
}

fun Category.toResponseGroup(): ResponseGroup {
    return ResponseGroup(
        id = id,
        title = title ?: "",
        language = when (language) {
            Language.ID.KK -> Language.KAZAKH
            Language.ID.RU -> Language.RUSSIAN
            Language.ID.EN -> Language.ENGLISH
        },
        children = mutableListOf(),
        extra = ResponseGroup.Extra(config?.order ?: -1)
    )
}

fun Category.toResponse(): Response {
    return Response(
        id = id,
        title = title ?: "",
        language = when (language) {
            Language.ID.KK -> Language.KAZAKH
            Language.ID.RU -> Language.RUSSIAN
            Language.ID.EN -> Language.ENGLISH
        },
        responses = responses
    )
}