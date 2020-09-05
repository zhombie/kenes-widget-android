package q19.kenes_widget.util

import android.content.res.ColorStateList

class ColorStateListBuilder {

    var colors: MutableList<Int> = ArrayList()
    var states: MutableList<IntArray> = ArrayList()

    fun addState(state: IntArray, color: Int): ColorStateListBuilder {
        states.add(state)
        colors.add(color)
        return this
    }

    fun build(): ColorStateList {
        return ColorStateList(
            convertToTwoDimensionalIntArray(states),
            convertToIntArray(colors)
        )
    }

    private fun convertToTwoDimensionalIntArray(integers: List<IntArray>): Array<IntArray> {
        val result = Array(integers.size) { IntArray(1) }
        val iterator = integers.iterator()
        var i = 0
        while (iterator.hasNext()) {
            result[i] = iterator.next()
            i++
        }
        return result
    }

    private fun convertToIntArray(integers: List<Int>): IntArray {
        val result = IntArray(integers.size)
        val iterator = integers.iterator()
        var i = 0
        while (iterator.hasNext()) {
            result[i] = iterator.next()
            i++
        }
        return result
    }

}