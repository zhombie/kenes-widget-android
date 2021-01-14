package q19.kenes.widget.util

internal object ThreadUtil {

    /**
     * Helper method for building a string of thread information.
     */
    @JvmStatic
    val threadInfo: String
        get() = "@[name=" + Thread.currentThread().name + ", id=" + Thread.currentThread().id + "]"

}