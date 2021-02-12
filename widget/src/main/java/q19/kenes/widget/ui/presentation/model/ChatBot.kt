package q19.kenes.widget.ui.presentation.model

import kz.q19.domain.model.knowledge_base.Response
import kz.q19.domain.model.knowledge_base.ResponseGroup
import kz.q19.socket.model.Category
import q19.kenes.widget.util.Logger.debug

internal class ChatBot {

    /**
     * State variables
     */
    @Deprecated("")
    var activeCategory: Category? = null

    var activeResponseGroup: ResponseGroup? = null
    var activeResponse: Response? = null

    /**
     * Response groups variables
     */
    var isParentResponseGroupChildrenRequested = false

    @Deprecated("")
    val allCategories = ObservableList()

    var responseGroups = listOf<ResponseGroup>()

    /**
     * Callback variable
     */
    var callback: Callback? = null

    @Deprecated("")
    private val observableListCallback by lazy {
        ObservableList.Callback { categories ->
            allCategories.callback = null
            callback?.onDashboardCategoriesLoaded(categories)
        }
    }

    init {
        initialize()
    }

    private fun initialize() {
        allCategories.callback = observableListCallback
    }

    fun clear() {
        activeCategory = null
        isParentResponseGroupChildrenRequested = false
        initialize()
        allCategories.clear()
    }

    fun interface Callback {
        fun onDashboardCategoriesLoaded(categories: List<Category>)
    }

}


internal class ObservableList : ArrayList<Category>() {

    companion object {
        private val TAG = ObservableList::class.java.simpleName
    }

    var callback: Callback? = null

    override fun add(element: Category): Boolean {
        if (any { it == element }) {
            return false
        }

        val added = super.add(element)

        if (callback != null) {
            val parents = filter { it.parentId == Category.NO_PARENT_ID }

            parents.forEach {
                if (it.id == element.parentId && !it.children.contains(element)) {
//                    it.children.add(element)
                }
            }

            if (!parents.isNullOrEmpty() && parents.all { !it.children.isNullOrEmpty() }) {
                callback?.onFull(parents)
            }
        }

        return added
    }

    override fun addAll(elements: Collection<Category>): Boolean {
//        debug(TAG, "addAll() -> elements: $elements")

        // Fix on potential exception (java.util.ConcurrentModificationException)
        try {
            if (any { elements.contains(it) }) {
                return false
            }
        } catch (e: Exception) {
            debug(TAG, "addAll() -> exception: $e")
            return false
        }

        val isAdded = super.addAll(elements)

        if (callback != null) {
            val parents = filter { it.parentId == Category.NO_PARENT_ID }.toMutableList()

            for (element in elements) {
                parents.forEach {
                    if (it.id == element.parentId && !it.children.contains(element)) {
//                        it.children.add(element)
                    }
                }
            }

            if (!parents.isNullOrEmpty() && parents.all { !it.children.isNullOrEmpty() }) {
                callback?.onFull(parents)
            }
        }

        return isAdded
    }

    fun interface Callback {
        fun onFull(categories: List<Category>)
    }

}