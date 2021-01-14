package q19.kenes.widget.util

internal inline fun <reified T : Enum<*>> findEnumBy(predicate: (T) -> Boolean): T? =
    T::class.java.enumConstants?.find(predicate)
