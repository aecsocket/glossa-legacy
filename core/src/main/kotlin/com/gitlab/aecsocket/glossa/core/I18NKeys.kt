package com.gitlab.aecsocket.glossa.core

private const val KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"

class KeyValidationException(string: String, val index: Int)
    : RuntimeException("Invalid character '${string[index]}' in '$string' at position ${index+1}, valid: [$KEY_CHARS]")

object I18NKeys {
    fun validate(key: String): String {
        val idx = key.indexOfFirst { !KEY_CHARS.contains(it) }
        if (idx != -1)
            throw KeyValidationException(key, idx)
        return key
    }
}
