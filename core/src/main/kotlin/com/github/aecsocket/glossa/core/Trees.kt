package com.github.aecsocket.glossa.core

/**
 * Helper class for tree structures.
 */
object Trees {
    /**
     * Renders a tree structure using box drawing characters.
     * @param items Items to render.
     * @param getValue Function to get value lines from an item.
     * @param getTree Function to get tree lines from an item (call [render] on the child)
     * @return Lines of the rendered form.
     */
    fun <T> render(items: List<T>, getValue: (T) -> List<String>, getTree: (T) -> List<String>): List<String> {
        val res = ArrayList<String>()
        items.forEachIndexed { idx, child ->
            val last = idx == items.size - 1
            getValue(child).forEachIndexed { lineIdx, line ->
                res.add("${if (lineIdx == 0) if (last) "└─" else "├─" else if (last) "  " else "│ "} $line")
            }
            getTree(child).forEach {
                res.add("${if (last) " " else "│"}   $it")
            }
        }
        return res
    }
}
