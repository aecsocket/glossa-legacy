package com.gitlab.aecsocket.glossa.core

/** The valid characters that can be used in an argument or template key. */
const val KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"

/**
 * An exception for if a key contained any characters that are not in [KEY_CHARS].
 */
class KeyValidationException(string: String, val index: Int)
    : RuntimeException("Invalid character '${string[index]}' in '$string' at position ${index+1}, valid: [$KEY_CHARS]")

object Keys {
    /**
     * Checks if a key contains any characters not in [KEY_CHARS].
     */
    fun validate(key: String): String {
        val idx = key.indexOfFirst { !KEY_CHARS.contains(it) }
        if (idx != -1)
            throw KeyValidationException(key, idx)
        return key
    }
}
