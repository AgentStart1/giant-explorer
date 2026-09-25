package com.storyteller_f.giant_explorer.control.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManagerTest {
    private val meta = PluginMeta("", "sample.gep", "sample.gep", "other")

    @Test
    fun legacyMetadataKeepsFragmentEntryAndVersion() {
        val configuration = gepConfiguration(meta, javaClass.classLoader!!, listOf("Entry", "Entry,Child", "1.2"))
        assertTrue(configuration is FragmentPluginConfiguration)
        configuration as FragmentPluginConfiguration
        assertEquals("Entry", configuration.startFragment)
        assertEquals(listOf("Entry", "Child"), configuration.pluginFragments)
        assertEquals("1.2", configuration.meta.version)
    }

    @Test
    fun shellMetadataSelectsShellLoaderWithoutReplacingVersion() {
        val configuration = gepConfiguration(
            meta,
            javaClass.classLoader!!,
            listOf("ShellEntry", "", "1.2", "type=shell")
        )
        assertTrue(configuration is ShellPluginConfiguration)
        assertEquals("ShellEntry", (configuration as ShellPluginConfiguration).entryClass)
        assertEquals("1.2", configuration.meta.version)
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedPluginTypeIsRejected() {
        gepConfiguration(meta, javaClass.classLoader!!, listOf("Entry", "", "1", "type=unknown"))
    }

    @Test
    fun gepUsesFragmentPluginLoader() {
        assertEquals(PluginType.fragment, pluginTypeForExtension("gep"))
        assertEquals(PluginType.fragment, pluginTypeForExtension("GEP"))
    }

    @Test
    fun otherArchivesUseHtmlPluginLoader() {
        assertEquals(PluginType.html, pluginTypeForExtension("zip"))
    }
}
