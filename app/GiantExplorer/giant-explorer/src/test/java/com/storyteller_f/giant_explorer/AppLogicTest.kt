package com.storyteller_f.giant_explorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogicTest {
    @Test
    fun mapNullNullReturnsMappedValuesWhenAllInputsProduceValues() {
        val result = listOf(1, 2, 3).mapNullNull { it * 2 }

        assertEquals(listOf(2, 4, 6), result)
    }

    @Test
    fun mapNullNullReturnsNullIfAnyInputProducesNull() {
        val result = listOf(1, 2, 3).mapNullNull {
            if (it == 2) null else it * 2
        }

        assertNull(result)
    }

    @Test
    fun collectionValidOnlyAcceptsNonEmptyCollections() {
        assertFalse(emptyList<Int>().valid())
        assertFalse((null as List<Int>?).valid())
        assertTrue(listOf(1).valid())
    }
}
