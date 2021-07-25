package q19.kenes.widget.domain.model

import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import q19.kenes.widget.ui.presentation.calls.*

internal fun Configs.Call.getTitle(language: Language): String? {
    val title = title.get(language) ?: title.get(Language.DEFAULT)
    if (title.isNullOrBlank()) return null
    return title
}

internal fun Configs.buildCalls(language: Language): List<OldCall>? {
    fun buildChildren(call: Configs.Call): List<OldCall> {
        return calls
            ?.filter { call.id == it.parentId }
            ?.mapNotNull {
                OldCall(
                    id = it.id,
                    title = it.getTitle(language) ?: return@mapNotNull null,
                    isPrimary = false,
                    children = buildChildren(it)
                )
            }
            ?: emptyList()
    }

    return calls
        ?.filter { it.isParent() }
        ?.mapNotNull { call ->
            OldCall(
                id = call.id,
                title = call.getTitle(language) ?: return@mapNotNull null,
                isPrimary = true,
                children = buildChildren(call)
            )
        }
}


internal fun Configs.buildCalls2(language: Language): List<Call>? {
    fun buildChildren(call: Configs.Call): List<Call> {
        val children = calls?.filter { call.id == it.parentId }
        val mappedChildren = mutableListOf<Call>()
        children?.forEach { child ->
            if (child.isFolder()) {
                mappedChildren.add(
                    CallGroup(
                        child.id,
                        child.getTitle(language) ?: return@forEach,
                        buildChildren(child)
                    )
                )
            } else {
                when {
                    child.isAudioCall() ->
                        mappedChildren.add(
                            Call.Audio(
                                child.id,
                                child.getTitle(language) ?: return@forEach
                            )
                        )
                    child.isVideoCall() ->
                        mappedChildren.add(
                            Call.Video(
                                child.id,
                                child.getTitle(language) ?: return@forEach
                            )
                        )
                }
            }
        }
        return mappedChildren
    }

    return calls
        ?.filter { it.isParent() }
        ?.mapNotNull { call ->
            PrimaryCallGroup(
                id = call.id,
                title = call.getTitle(language) ?: return@mapNotNull null,
                children = buildChildren(call)
            )
        }
}