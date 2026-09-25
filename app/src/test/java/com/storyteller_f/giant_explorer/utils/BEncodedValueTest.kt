package com.storyteller_f.giant_explorer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BEncodedValueTest {
    @Test
    fun dictionaryToStringEncodesValuesInInsertionOrder() {
        val dictionary = BEncodedDictionary()
        dictionary.put("name", "temp")
        dictionary.put("size", 1024L)
        dictionary.put("pieces", BEncodedList().apply {
            add("first")
            add(2L)
        })

        assertEquals("d4:name4:temp4:sizei1024e6:piecesl5:firsti2eee", dictionary.toString())
    }

    @Test
    fun listToStringEncodesSupportedValueTypes() {
        val nested = BEncodedDictionary().apply {
            put("key", "value")
        }
        val list = BEncodedList().apply {
            add("item")
            add(7L)
            add(nested)
        }

        assertEquals("l4:itemi7ed3:key5:valueee", list.toString())
    }
}
