package q19.kenes_widget.util

object AssertionUtil {

    /**
     * Helper method which throws an exception when an assertion has failed
     */
    @JvmStatic
    fun assertIsTrue(condition: Boolean) {
        if (!condition) {
            throw AssertionError("Expected condition to be true")
        }
    }

}