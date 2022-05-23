package com.github.aecsocket.glossa.core.extension

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition

fun MessageFormat.build(args: Map<String, Any?>): String {
    val res = StringBuffer()
    format(args, res, FieldPosition(0))
    return res.toString()
}
