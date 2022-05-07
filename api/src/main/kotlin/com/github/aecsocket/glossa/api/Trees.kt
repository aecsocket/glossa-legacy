package com.github.aecsocket.glossa.api

/**
 * Helper class for tree structures.
 */
object Trees {
    /**
     * Renders a tree structure using box drawing characters.
     * @param items the items to render.
     * @param getValue function to get value lines from an item.
     * @param getTree function to get tree lines from an item (call [render] on the child)
     * @return the lines of the rendered form.
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
