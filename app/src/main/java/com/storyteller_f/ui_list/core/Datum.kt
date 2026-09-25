@file:Suppress("Filename", "MatchingDeclarationName")

package com.storyteller_f.ui_list.core

interface Model {
    fun commonId(): String

    /**
     * 用于object pool的标识
     */
    fun uniqueIdInOP() = commonId()
}
