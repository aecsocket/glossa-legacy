package com.github.aecsocket.glossa.core

const val KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"

class KeyValidationException(string: String, val index: Int)
    : RuntimeException("Invalid character '${string[index]}' in '$string' at position ${index+1}, valid: [$KEY_CHARS]")

fun String.validate(): String {
    val idx = indexOfFirst { !KEY_CHARS.contains(it) }
    if (idx != -1)
        throw KeyValidationException(this, idx)
    return this
}
