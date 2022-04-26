package com.github.aecsocket.glossa.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object DecimalFormats {
    private val formats = HashMap<Locale, DecimalFormat>()

    fun format(locale: Locale) = formats.computeIfAbsent(locale) {
        DecimalFormat("0.###", DecimalFormatSymbols.getInstance(it))
    }
}
