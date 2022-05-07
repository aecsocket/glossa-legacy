package com.github.aecsocket.glossa.core

object Trees {
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