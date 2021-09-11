package q19.kenes.widget.domain.model

import kz.q19.domain.model.call.AnyCall
import kz.q19.domain.model.call.Call
import kz.q19.domain.model.call.CallGroup
import kz.q19.domain.model.configs.Configs
import kz.q19.domain.model.language.Language
import q19.kenes.widget.ui.presentation.call.*

internal fun Configs.Call.getTitle(language: Language): String? {
    val title = title.get(language) ?: title.get(Language.DEFAULT)
    if (title.isNullOrBlank()) return null
    return title
}


internal fun Configs.buildCallsAsTree(language: Language): List<AnyCall>? {
    fun buildChildren(call: Configs.Call): List<AnyCall> {
        val children = calls?.filter { call.id == it.parentId }
        val mappedChildren = mutableListOf<AnyCall>()
        children?.forEach { child ->
            if (child.isFolder()) {
                mappedChildren.add(
                    CallGroup.Secondary(
                        id = child.id,
                        title = child.getTitle(language) ?: return@forEach,
                        children = buildChildren(child)
                    )
                )
            } else {
                when {
                    child.isAudioCall() ->
                        mappedChildren.add(
                            Call.Audio(
                                id = child.id,
                                title = child.getTitle(language) ?: return@forEach
                            )
                        )
                    child.isVideoCall() ->
                        mappedChildren.add(
                            Call.Video(
                                id = child.id,
                                title = child.getTitle(language) ?: return@forEach
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
            CallGroup.Primary(
                id = call.id,
                title = call.getTitle(language) ?: return@mapNotNull null,
                children = buildChildren(call)
            )
        }
}