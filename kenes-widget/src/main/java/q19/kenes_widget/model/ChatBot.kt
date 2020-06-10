package q19.kenes_widget.model

internal class ChatBot {

    /**
     * State variables
     */
    var activeCategory: Category? = null

    /**
     * Basic category variables
     */
    var isBasicCategoriesFilled = false

    var basicCategories = listOf<Category>()
    val allCategories = ObservableList()

    /**
     * Callback variable
     */
    var callback: Callback? = null

    private val listCallback by lazy {
        object : ObservableList.Callback {
            override fun onFull(basic: List<Category>) {
                allCategories.callback = null
                basicCategories = basic
                callback?.onBasicCategoriesLoaded(basic)
            }
        }
    }

    init {
        initialize()
    }

    private fun initialize() {
        allCategories.callback = listCallback
    }

    fun clear() {
        activeCategory = null
        isBasicCategoriesFilled = false
        initialize()
        basicCategories = listOf()
        allCategories.clear()
    }

    interface Callback {
        fun onBasicCategoriesLoaded(categories: List<Category>)
    }

}


internal class ObservableList : ArrayList<Category>() {

    var callback: Callback? = null

    override fun add(element: Category): Boolean {
        if (any { it == element }) {
            return false
        }

        val added = super.add(element)

        if (callback != null) {
            val basic = filter { it.parentId == null }

            basic.forEach {
                if (it.id == element.parentId && !it.children.contains(element)) {
                    it.children.add(element)
                }
            }

            if (!basic.isNullOrEmpty() && basic.all { !it.children.isNullOrEmpty() }) {
                callback?.onFull(basic)
            }
        }

        return added
    }

    override fun addAll(elements: Collection<Category>): Boolean {
        if (any { elements.contains(it) }) {
            return false
        }

        val added = super.addAll(elements)

        if (callback != null) {
            val basic = filter { it.parentId == null }

            for (element in elements) {
                basic.forEach {
                    if (it.id == element.parentId && !it.children.contains(element)) {
                        it.children.add(element)
                    }
                }
            }

            if (!basic.isNullOrEmpty() && basic.all { !it.children.isNullOrEmpty() }) {
                callback?.onFull(basic)
            }
        }

        return added
    }

    interface Callback {
        fun onFull(basic: List<Category>)
    }

}