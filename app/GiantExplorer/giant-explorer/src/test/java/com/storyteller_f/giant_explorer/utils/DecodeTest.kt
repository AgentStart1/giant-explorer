package com.storyteller_f.giant_explorer.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class DecodeTest {
    @Test
    fun decodeDictionaryWithNestedListAndDictionary() = runBlocking {
        val input = "d4:name4:temp5:counti42e5:filesl5:alpha4:betae4:infod6:lengthi123eee"

        val dictionary = Decode.bDecode(input.byteInputStream())

        assertEquals("temp", dictionary.get("name"))
        assertEquals(42L, dictionary.get("count"))

        val files = dictionary.get("files") as BEncodedList
        assertEquals(listOf("alpha", "beta"), files)

        val info = dictionary.get("info") as BEncodedDictionary
        assertEquals(123L, info.get("length"))
    }

    @Test
    fun decodeReadsBytesAsIso88591SoTorrentNamesCanBeDecodedAsUtf8Later() = runBlocking {
        val torrentName = "测试文件"
        val nameBytes = torrentName.toByteArray(StandardCharsets.UTF_8)
        val encodedName = String(nameBytes, StandardCharsets.ISO_8859_1)
        val stream = ByteArrayInputStream(
            "d4:infod4:name${nameBytes.size}:${encodedName}ee".toByteArray(StandardCharsets.ISO_8859_1)
        )

        assertEquals(torrentName, getTorrentName(stream))
    }

    @Test
    fun decodeRejectsNonDictionaryRoot() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                Decode.bDecode("li1ee".byteInputStream())
            }
        }
    }
}
