package com.gitlab.aecsocket.glossa.core

interface Localizable<T> {
    fun localize(i18n: I18N<T>): T
}
